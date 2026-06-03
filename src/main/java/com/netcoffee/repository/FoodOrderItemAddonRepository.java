package com.netcoffee.repository;

import com.netcoffee.entity.TFoodOrderItemAddonEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodOrderItemAddonRepository
        extends JpaRepository<TFoodOrderItemAddonEntity, Long> {
    List<TFoodOrderItemAddonEntity> findByOrderItemId(Long orderItemId);
}
