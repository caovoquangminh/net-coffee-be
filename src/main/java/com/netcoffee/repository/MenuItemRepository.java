package com.netcoffee.repository;

import com.netcoffee.entity.TMenuItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuItemRepository extends JpaRepository<TMenuItemEntity, Long> {

    List<TMenuItemEntity> findByIsAvailableTrue();

    List<TMenuItemEntity> findByCategoryAndIsAvailableTrue(String category);
}
