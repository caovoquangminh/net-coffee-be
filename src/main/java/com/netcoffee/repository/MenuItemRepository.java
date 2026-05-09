package com.netcoffee.repository;

import com.netcoffee.entity.TMenuItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<TMenuItemEntity, Long>
{

    List<TMenuItemEntity> findByIsAvailableTrue();

    List<TMenuItemEntity> findByCategoryAndIsAvailableTrue(String category);
}
