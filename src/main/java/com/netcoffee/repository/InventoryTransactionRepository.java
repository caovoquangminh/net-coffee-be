package com.netcoffee.repository;

import com.netcoffee.entity.TInventoryTransactionEntity;
import com.netcoffee.enumtype.InventoryTransactionTypeEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryTransactionRepository
        extends JpaRepository<TInventoryTransactionEntity, Long> {
    Page<TInventoryTransactionEntity> findByInventoryItemIdOrderByCreatedAtDesc(
            Long inventoryItemId, Pageable pageable);

    Page<TInventoryTransactionEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<TInventoryTransactionEntity> findByType(InventoryTransactionTypeEnum type);

    @Query(
            "SELECT COALESCE(SUM(it.quantity * COALESCE(it.purchasePrice, 0)), 0)"
                    + " FROM TInventoryTransactionEntity it"
                    + " WHERE it.type = :type AND it.createdAt >= :from AND it.createdAt < :to")
    BigDecimal sumCostByTypeBetween(
            @Param("type") InventoryTransactionTypeEnum type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
