package com.netcoffee.service;

import com.netcoffee.dto.response.MenuItemResponse;
import com.netcoffee.entity.TMenuItemEntity;

import java.util.List;

public interface MenuItemService
{

    List<MenuItemResponse> findAvailable();

    List<MenuItemResponse> findByCategory(String category);

    TMenuItemEntity findById(Long id);
}
