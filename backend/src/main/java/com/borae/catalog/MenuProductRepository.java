package com.borae.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuProductRepository extends JpaRepository<MenuProduct, UUID> {

    @Query("""
            select product
            from MenuProduct product
            join fetch product.category category
            join fetch category.menu menu
            where menu.id = :menuId
            order by category.displayOrder, product.displayOrder, product.name
            """)
    List<MenuProduct> findAllByMenuIdOrderByCategoryAndDisplayOrder(@Param("menuId") UUID menuId);

    List<MenuProduct> findAllByCategory_IdOrderByDisplayOrderAscNameAsc(UUID categoryId);

    List<MenuProduct> findAllByCategory_IdAndLifecycleStatusOrderByDisplayOrderAscNameAsc(
            UUID categoryId, ProductLifecycleStatus lifecycleStatus);

    Optional<MenuProduct> findByIdAndCategory_Menu_Id(UUID productId, UUID menuId);
}
