package com.netcoffee.repository;

import com.netcoffee.entity.TInventoryItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryItemRepository extends JpaRepository<TInventoryItemEntity, Long> {
    List<TInventoryItemEntity> findByMenuItemId(Long menuItemId);

    @Query("SELECT i FROM TInventoryItemEntity i WHERE i.currentStock <= i.minStock")
    List<TInventoryItemEntity> findLowStock();
}
