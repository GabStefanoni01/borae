package com.borae.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class Product3DModelPostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID RESTAURANT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID MENU_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID CATEGORY_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID SECOND_PRODUCT_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final UUID MODEL_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void insertCatalogFixture() {
        jdbc.update("INSERT INTO user_account (id, full_name, email, password_hash) VALUES (?, ?, ?, ?)",
                USER_ID, "Owner Test", "owner-postgres-test@example.com", "test-hash");
        jdbc.update("INSERT INTO restaurant (id, name, slug, description) VALUES (?, ?, ?, ?)",
                RESTAURANT_ID, "Restaurant Test", "restaurant-postgres-test", "Integration fixture");
        jdbc.update("INSERT INTO restaurant_menu (id, restaurant_id, name, status, display_order) VALUES (?, ?, ?, ?, ?)",
                MENU_ID, RESTAURANT_ID, "Menu Test", "PUBLISHED", 0);
        jdbc.update("INSERT INTO menu_category (id, menu_id, name, active, display_order) VALUES (?, ?, ?, ?, ?)",
                CATEGORY_ID, MENU_ID, "Category Test", true, 0);
        insertProduct(PRODUCT_ID, "Product Test");
        insertProduct(SECOND_PRODUCT_ID, "Second Product Test");
    }

    @Test
    void persistsModelAndAuditWithRealPostgresConstraints() {
        insertModel(MODEL_ID, PRODUCT_ID, "imagekit-file-1", 120, 80, 60);
        jdbc.update("INSERT INTO menu_product_3d_model_audit (id, product_id, actor_user_id, action, provider_file_id) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), PRODUCT_ID, USER_ID, "UPLOADED", "imagekit-file-1");

        assertEquals(1, count("SELECT COUNT(*) FROM menu_product_3d_model WHERE product_id = ?", PRODUCT_ID));
        assertEquals(1, count("SELECT COUNT(*) FROM menu_product_3d_model_audit WHERE product_id = ?", PRODUCT_ID));
        assertEquals("READY", jdbc.queryForObject(
                "SELECT status FROM menu_product_3d_model WHERE id = ?", String.class, MODEL_ID));
    }

    @Test
    void enforcesOneModelPerProduct() {
        insertModel(MODEL_ID, PRODUCT_ID, "imagekit-file-first", 120, 80, 60);

        assertThrows(DataIntegrityViolationException.class,
                () -> insertModel(UUID.randomUUID(), PRODUCT_ID, "imagekit-file-second", 120, 80, 60));
    }

    @Test
    void enforcesUniqueImageKitFileIdAcrossProducts() {
        insertModel(MODEL_ID, PRODUCT_ID, "imagekit-file-shared", 120, 80, 60);

        assertThrows(DataIntegrityViolationException.class,
                () -> insertModel(UUID.randomUUID(), SECOND_PRODUCT_ID, "imagekit-file-shared", 120, 80, 60));
    }

    @Test
    void rejectsInvalidDimensions() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertModel(MODEL_ID, PRODUCT_ID, "imagekit-invalid-dimensions", 0, 80, 60));
    }

    @Test
    void rejectsUnknownProductReferences() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertModel(MODEL_ID, UUID.randomUUID(), "imagekit-orphan", 120, 80, 60));
    }

    @Test
    void deletingProductCascadesModelButKeepsAuditHistory() {
        insertModel(MODEL_ID, PRODUCT_ID, "imagekit-file-delete", 120, 80, 60);
        jdbc.update("INSERT INTO menu_product_3d_model_audit (id, product_id, actor_user_id, action, provider_file_id) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), PRODUCT_ID, USER_ID, "DELETED", "imagekit-file-delete");

        jdbc.update("DELETE FROM menu_product WHERE id = ?", PRODUCT_ID);

        assertEquals(0, count("SELECT COUNT(*) FROM menu_product_3d_model WHERE product_id = ?", PRODUCT_ID));
        assertEquals(1, count("SELECT COUNT(*) FROM menu_product_3d_model_audit WHERE product_id = ?", PRODUCT_ID));
    }

    @Test
    void incrementsSharedRateLimitAtomicallyInRealPostgres() {
        com.borae.security.PostgresRateLimitStorage storage = new com.borae.security.PostgresRateLimitStorage(jdbc);
        String key = "usuario:test-pg:auth-login";

        com.borae.security.RateLimitWindow first = storage.increment(key, 60_000L);
        com.borae.security.RateLimitWindow second = storage.increment(key, 60_000L);

        assertEquals(1, first.count());
        assertEquals(2, second.count());
        assertEquals(first.resetAtEpochMs(), second.resetAtEpochMs());
    }

    private void insertProduct(UUID productId, String name) {
        jdbc.update("INSERT INTO menu_product (id, category_id, name, price_in_cents, lifecycle_status, availability, display_order) VALUES (?, ?, ?, ?, ?, ?, ?)",
                productId, CATEGORY_ID, name, 2590L, "ACTIVE", "AVAILABLE", 0);
    }

    private void insertModel(UUID modelId, UUID productId, String fileId,
                             int widthMm, int heightMm, int depthMm) {
        jdbc.update("""
                INSERT INTO menu_product_3d_model
                    (id, product_id, status, provider_file_id, public_url, content_type,
                     file_size, width_mm, height_mm, depth_mm, created_by_user_id, updated_by_user_id)
                VALUES (?, ?, 'READY', ?, ?, 'model/gltf-binary', 2048, ?, ?, ?, ?, ?)
                """,
                modelId, productId, fileId, "https://ik.imagekit.io/test/" + fileId + ".glb",
                widthMm, heightMm, depthMm, USER_ID, USER_ID);
    }

    private int count(String sql, UUID id) {
        return jdbc.queryForObject(sql, Integer.class, id);
    }
}
