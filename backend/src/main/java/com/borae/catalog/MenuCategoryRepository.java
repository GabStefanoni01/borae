package com.borae.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {
    List<MenuCategory> findAllByMenu_IdOrderByDisplayOrderAscNameAsc(UUID menuId);
    List<MenuCategory> findAllByMenu_IdAndActiveTrueOrderByDisplayOrderAscNameAsc(UUID menuId);
    Optional<MenuCategory> findByIdAndMenu_Id(UUID categoryId, UUID menuId);
}
