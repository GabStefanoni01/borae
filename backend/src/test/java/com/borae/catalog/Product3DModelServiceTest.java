package com.borae.catalog;

import com.borae.identity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Product3DModelServiceTest {

    @Mock private RestaurantMembershipRepository membershipRepository;
    @Mock private MenuRepository menuRepository;
    @Mock private MenuProductRepository productRepository;
    @Mock private Product3DModelRepository modelRepository;
    @Mock private Product3DModelAuditRepository auditRepository;
    @Mock private RestaurantObjectStorage storage;

    private Product3DModelService service;
    private UserAccount actor;
    private Restaurant restaurant;
    private Menu menu;
    private MenuProduct product;

    @BeforeEach
    void setUp() {
        service = new Product3DModelService(membershipRepository, menuRepository, productRepository,
                modelRepository, auditRepository, storage, new GlbFileValidator(new ObjectMapper()));
        actor = new UserAccount("Owner", "owner@example.com", "hash");
        restaurant = new Restaurant("Restaurante", "restaurante-12345678", null);
        menu = new Menu(restaurant, "Almoço", null, 0);
        MenuCategory category = new MenuCategory(menu, "Pratos", null, 0);
        product = new MenuProduct(category, "Prato", null, 2590L, 0, List.of(), List.of());
    }

    @Test
    void staffCannotUploadOrChangeModelAssets() {
        grantRole(RestaurantRole.STAFF);
        assertThrows(ResponseStatusException.class, () -> service.upload(restaurant.getId(), menu.getId(),
                product.getId(), validFile(), 120, 80, 60, actor));
        verifyNoInteractions(storage, modelRepository, auditRepository);
    }

    @Test
    void ownerCanUploadValidGlbAndModelStartsAsReady() {
        grantRole(RestaurantRole.OWNER);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurant.getId()))
                .thenReturn(Optional.of(menu));
        when(productRepository.findByIdAndCategory_Menu_Id(product.getId(), menu.getId()))
                .thenReturn(Optional.of(product));
        when(modelRepository.findByProduct_Id(product.getId())).thenReturn(Optional.empty());
        when(storage.upload(any(), any(), any(), eq("model/gltf-binary")))
                .thenReturn(new RestaurantObjectStorage.StoredObject("file-123",
                        "https://ik.imagekit.io/demo/model.glb"));
        when(modelRepository.saveAndFlush(any(Product3DModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product3DModelResponse response = service.upload(restaurant.getId(), menu.getId(), product.getId(),
                validFile(), 120, 80, 60, actor);

        assertEquals(Product3DModelStatus.READY, response.status());
        assertEquals("https://ik.imagekit.io/demo/model.glb", response.modelUrl());
        assertEquals(120, response.widthMm());
        verify(auditRepository).save(any(Product3DModelAudit.class));
    }

    @Test
    void invalidGlbIsRejectedBeforeCallingImageKit() {
        grantRole(RestaurantRole.OWNER);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurant.getId()))
                .thenReturn(Optional.of(menu));
        when(productRepository.findByIdAndCategory_Menu_Id(product.getId(), menu.getId()))
                .thenReturn(Optional.of(product));

        MockMultipartFile invalid = new MockMultipartFile("file", "model.glb",
                "model/gltf-binary", "not a glb".getBytes(StandardCharsets.UTF_8));

        assertThrows(ResponseStatusException.class, () -> service.upload(restaurant.getId(), menu.getId(),
                product.getId(), invalid, 120, 80, 60, actor));
        verifyNoInteractions(storage, modelRepository, auditRepository);
    }


    @Test
    void rejectsNonPositivePhysicalDimensionsBeforeUploading() {
        grantRole(RestaurantRole.OWNER);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurant.getId()))
                .thenReturn(Optional.of(menu));
        when(productRepository.findByIdAndCategory_Menu_Id(product.getId(), menu.getId()))
                .thenReturn(Optional.of(product));

        assertThrows(ResponseStatusException.class, () -> service.upload(restaurant.getId(), menu.getId(),
                product.getId(), validFile(), 0, 80, 60, actor));
        verifyNoInteractions(storage, modelRepository, auditRepository);
    }

    @Test
    void removesNewlyUploadedAssetWhenDatabasePersistenceFails() {
        grantRole(RestaurantRole.OWNER);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurant.getId()))
                .thenReturn(Optional.of(menu));
        when(productRepository.findByIdAndCategory_Menu_Id(product.getId(), menu.getId()))
                .thenReturn(Optional.of(product));
        when(modelRepository.findByProduct_Id(product.getId())).thenReturn(Optional.empty());
        when(storage.upload(any(), any(), any(), eq("model/gltf-binary")))
                .thenReturn(new RestaurantObjectStorage.StoredObject("orphan-file",
                        "https://ik.imagekit.io/demo/orphan.glb"));
        when(modelRepository.saveAndFlush(any(Product3DModel.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, () -> service.upload(restaurant.getId(), menu.getId(),
                product.getId(), validFile(), 120, 80, 60, actor));

        verify(storage).delete("orphan-file");
        verify(auditRepository, never()).save(any(Product3DModelAudit.class));
    }


    @Test
    void replacementResetsPublicationAndDeletesPreviousAssetOnlyAfterCommit() {
        grantRole(RestaurantRole.OWNER);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurant.getId()))
                .thenReturn(Optional.of(menu));
        when(productRepository.findByIdAndCategory_Menu_Id(product.getId(), menu.getId()))
                .thenReturn(Optional.of(product));
        Product3DModel existing = new Product3DModel(product, "old-file",
                "https://ik.imagekit.io/demo/old.glb", "model/gltf-binary", 2048L,
                100, 70, 50, actor.getId());
        existing.publish(actor.getId());
        when(modelRepository.findByProduct_Id(product.getId())).thenReturn(Optional.of(existing));
        when(storage.upload(any(), any(), any(), eq("model/gltf-binary")))
                .thenReturn(new RestaurantObjectStorage.StoredObject("new-file",
                        "https://ik.imagekit.io/demo/new.glb"));

        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            Product3DModelResponse response = service.upload(restaurant.getId(), menu.getId(), product.getId(),
                    validFile(), 120, 80, 60, actor);

            assertEquals(Product3DModelStatus.READY, response.status());
            assertEquals("https://ik.imagekit.io/demo/new.glb", response.modelUrl());
            verify(modelRepository).flush();
            verify(storage, never()).delete("old-file");
            verify(auditRepository).save(argThat(audit -> true));

            org.springframework.transaction.support.TransactionSynchronizationManager
                    .getSynchronizations().forEach(
                            org.springframework.transaction.support.TransactionSynchronization::afterCommit);
            verify(storage).delete("old-file");
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletingModelRemovesDatabaseRecordAndSchedulesImageKitDeletionAfterCommit() {
        grantRole(RestaurantRole.OWNER);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurant.getId()))
                .thenReturn(Optional.of(menu));
        when(productRepository.findByIdAndCategory_Menu_Id(product.getId(), menu.getId()))
                .thenReturn(Optional.of(product));
        Product3DModel existing = new Product3DModel(product, "delete-file",
                "https://ik.imagekit.io/demo/delete.glb", "model/gltf-binary", 2048L,
                100, 70, 50, actor.getId());
        when(modelRepository.findByProduct_Id(product.getId())).thenReturn(Optional.of(existing));

        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            service.delete(restaurant.getId(), menu.getId(), product.getId(), actor);

            verify(modelRepository).delete(existing);
            verify(auditRepository).save(any(Product3DModelAudit.class));
            verify(storage, never()).delete("delete-file");

            org.springframework.transaction.support.TransactionSynchronizationManager
                    .getSynchronizations().forEach(
                            org.springframework.transaction.support.TransactionSynchronization::afterCommit);
            verify(storage).delete("delete-file");
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private MockMultipartFile validFile() {
        byte[] json = "{\"asset\":{\"version\":\"2.0\"}}".getBytes(StandardCharsets.UTF_8);
        int paddedLength = (json.length + 3) & ~3;
        ByteBuffer buffer = ByteBuffer.allocate(20 + paddedLength).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0x46546C67);
        buffer.putInt(2);
        buffer.putInt(buffer.capacity());
        buffer.putInt(paddedLength);
        buffer.putInt(0x4E4F534A);
        buffer.put(json);
        while (buffer.position() < buffer.capacity()) buffer.put((byte) 0x20);
        return new MockMultipartFile("file", "model.glb", "model/gltf-binary", buffer.array());
    }

    private void grantRole(RestaurantRole role) {
        RestaurantMembership membership = new RestaurantMembership(actor, restaurant, role);
        when(membershipRepository.findByUser_IdAndRestaurant_IdAndActiveTrue(actor.getId(), restaurant.getId()))
                .thenReturn(Optional.of(membership));
    }
}
