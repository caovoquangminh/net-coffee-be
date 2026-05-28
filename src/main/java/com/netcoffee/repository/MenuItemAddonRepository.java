package com.netcoffee.repository;

import com.netcoffee.entity.TMenuItemAddonEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemAddonRepository extends JpaRepository<TMenuItemAddonEntity, Long> {
    List<TMenuItemAddonEntity> findByMenuItemIdAndIsAvailable(Long menuItemId, Boolean isAvailable);

    List<TMenuItemAddonEntity> findByMenuItemId(Long menuItemId);
}
