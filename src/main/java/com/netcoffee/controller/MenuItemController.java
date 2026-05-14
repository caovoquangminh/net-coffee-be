package com.netcoffee.controller;

import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.entity.TMenuItemEntity;
import com.netcoffee.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/menu") @RequiredArgsConstructor
public class MenuItemController
{

    private final MenuItemService menuItemService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TMenuItemEntity>>> getAll()
    {
        return ResponseEntity.ok(ApiResponse.ok(menuItemService.findAvailable()));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<TMenuItemEntity>>> getByCategory(@PathVariable String category)
    {
        return ResponseEntity.ok(ApiResponse.ok(menuItemService.findByCategory(category)));
    }
}
