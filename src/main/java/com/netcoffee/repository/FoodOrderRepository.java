package com.netcoffee.repository;

import com.netcoffee.entity.TFoodOrderEntity;
import com.netcoffee.enumtype.OrderStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FoodOrderRepository extends JpaRepository<TFoodOrderEntity, Long>
{

    List<TFoodOrderEntity> findBySessionIdOrderByCreatedAtDesc(Long sessionId);

    List<TFoodOrderEntity> findByStatus(OrderStatusEnum status);

    List<TFoodOrderEntity> findByMachineIdAndStatus(Long machineId, OrderStatusEnum status);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM TFoodOrderEntity o WHERE o.status = :status AND o.createdAt >= :from AND o.createdAt < :to")
    BigDecimal sumByStatusBetween(@Param("status") OrderStatusEnum status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
