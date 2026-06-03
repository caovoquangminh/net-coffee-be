package com.netcoffee.repository;

import com.netcoffee.entity.TFoodOrderItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodOrderItemRepository extends JpaRepository<TFoodOrderItemEntity, Long> {

    List<TFoodOrderItemEntity> findByOrderId(Long orderId);
}
