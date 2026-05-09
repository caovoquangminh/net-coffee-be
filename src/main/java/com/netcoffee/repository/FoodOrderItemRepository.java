package com.netcoffee.repository;

import com.netcoffee.entity.TFoodOrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodOrderItemRepository extends JpaRepository<TFoodOrderItemEntity, Long>
{

    List<TFoodOrderItemEntity> findByOrderId(Long orderId);
}
