package com.netcoffee.repository;

import com.netcoffee.entity.TInventoryItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InventoryItemRepository extends JpaRepository<TInventoryItemEntity, Long> {

    @Query("SELECT i FROM TInventoryItemEntity i WHERE i.currentStock <= i.minStock")
    List<TInventoryItemEntity> findLowStock();
}
