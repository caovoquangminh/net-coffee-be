package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.request.CreateAddonRequest;
import com.netcoffee.dto.request.CreateMenuItemRequest;
import com.netcoffee.dto.request.SetInventoryLinksRequest;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.MenuItemResponse;
import com.netcoffee.service.MenuAdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.ADMIN_MENU)
@RequiredArgsConstructor
public class MenuAdminController {

    private final MenuAdminService menuAdminService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(menuAdminService.findAll()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> create(
            @RequestBody CreateMenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo món thành công", menuAdminService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> update(
            @PathVariable Long id, @RequestBody CreateMenuItemRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Cập nhật món thành công", menuAdminService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        menuAdminService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã ẩn món thành công", null));
    }

    @PostMapping("/{id}/addons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> addAddon(
            @PathVariable Long id, @RequestBody CreateAddonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                "Thêm addon thành công", menuAdminService.addAddon(id, request)));
    }

    @PutMapping("/{id}/addons/{addonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateAddon(
            @PathVariable Long id,
            @PathVariable Long addonId,
            @RequestBody CreateAddonRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Cập nhật addon thành công",
                        menuAdminService.updateAddon(id, addonId, request)));
    }

    @DeleteMapping("/{id}/addons/{addonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAddon(
            @PathVariable Long id, @PathVariable Long addonId) {
        menuAdminService.deleteAddon(id, addonId);
        return ResponseEntity.ok(ApiResponse.ok("Xóa addon thành công", null));
    }

    @PutMapping("/{id}/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> setInventoryLinks(
            @PathVariable Long id, @RequestBody List<SetInventoryLinksRequest> links) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Cập nhật nguyên liệu thành công",
                        menuAdminService.setInventoryLinks(id, links)));
    }
}
