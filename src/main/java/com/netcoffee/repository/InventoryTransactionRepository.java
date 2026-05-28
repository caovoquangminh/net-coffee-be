package com.netcoffee.repository;

import com.netcoffee.entity.TInventoryTransactionEntity;
import com.netcoffee.enumtype.InventoryTransactionTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<TInventoryTransactionEntity, Long> {
    Page<TInventoryTransactionEntity> findByInventoryItemIdOrderByCreatedAtDesc(Long inventoryItemId, Pageable pageable);
    Page<TInventoryTransactionEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<TInventoryTransactionEntity> findByType(InventoryTransactionTypeEnum type);
}
