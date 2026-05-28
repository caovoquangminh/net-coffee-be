package com.netcoffee.repository;

import com.netcoffee.entity.TFoodOrderItemAddonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodOrderItemAddonRepository extends JpaRepository<TFoodOrderItemAddonEntity, Long> {
    List<TFoodOrderItemAddonEntity> findByOrderItemId(Long orderItemId);
}
