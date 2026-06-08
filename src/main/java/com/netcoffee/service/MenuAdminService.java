package com.netcoffee.service;

import com.netcoffee.dto.request.CreateAddonRequest;
import com.netcoffee.dto.request.CreateMenuItemRequest;
import com.netcoffee.dto.request.SetInventoryLinksRequest;
import com.netcoffee.dto.response.MenuItemResponse;
import java.util.List;

public interface MenuAdminService {

    List<MenuItemResponse> findAll();

    MenuItemResponse create(CreateMenuItemRequest request);

    MenuItemResponse update(Long id, CreateMenuItemRequest request);

    void softDelete(Long id);

    MenuItemResponse addAddon(Long menuItemId, CreateAddonRequest request);

    MenuItemResponse updateAddon(Long menuItemId, Long addonId, CreateAddonRequest request);

    void deleteAddon(Long menuItemId, Long addonId);

    MenuItemResponse setInventoryLinks(Long menuItemId, List<SetInventoryLinksRequest> links);
}
