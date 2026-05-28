package com.netcoffee.repository;

import com.netcoffee.entity.TMenuItemAddonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemAddonRepository extends JpaRepository<TMenuItemAddonEntity, Long> {
    List<TMenuItemAddonEntity> findByMenuItemIdAndIsAvailable(Long menuItemId, Boolean isAvailable);
    List<TMenuItemAddonEntity> findByMenuItemId(Long menuItemId);
}
