package com.netcoffee.repository;

import com.netcoffee.entity.TMenuItemInventoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemInventoryRepository
        extends JpaRepository<TMenuItemInventoryEntity, TMenuItemInventoryEntity.PK> {

    List<TMenuItemInventoryEntity> findByMenuItemId(Long menuItemId);

    List<TMenuItemInventoryEntity> findByInventoryItemId(Long inventoryItemId);

    void deleteByInventoryItemId(Long inventoryItemId);

    void deleteByMenuItemId(Long menuItemId);
}
