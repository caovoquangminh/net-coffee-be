package com.netcoffee.service;

import com.netcoffee.entity.TMenuItemEntity;

import java.util.List;

public interface MenuItemService
{

    List<TMenuItemEntity> findAvailable();

    List<TMenuItemEntity> findByCategory(String category);

    TMenuItemEntity findById(Long id);
}
