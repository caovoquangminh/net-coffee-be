package com.netcoffee.repository;

import com.netcoffee.entity.TFoodOrderEntity;
import com.netcoffee.enumtype.OrderStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodOrderRepository extends JpaRepository<TFoodOrderEntity, Long>
{

    List<TFoodOrderEntity> findBySessionIdOrderByCreatedAtDesc(Long sessionId);

    List<TFoodOrderEntity> findByStatus(OrderStatusEnum status);

    List<TFoodOrderEntity> findByMachineIdAndStatus(Long machineId, OrderStatusEnum status);
}
