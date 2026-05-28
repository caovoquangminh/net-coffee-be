package com.netcoffee.service.impl;

import com.netcoffee.dto.response.MenuItemResponse;
import com.netcoffee.entity.TMenuItemAddonEntity;
import com.netcoffee.entity.TMenuItemEntity;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.repository.MenuItemAddonRepository;
import com.netcoffee.repository.MenuItemRepository;
import com.netcoffee.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final MenuItemAddonRepository addonRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> findAvailable() {
        return menuItemRepository.findByIsAvailableTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> findByCategory(String category) {
        return menuItemRepository.findByCategoryAndIsAvailableTrue(category).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TMenuItemEntity findById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Món không tồn tại: " + id));
    }

    private MenuItemResponse toResponse(TMenuItemEntity item) {
        List<MenuItemResponse.AddonResponse> addons = addonRepository
                .findByMenuItemIdAndIsAvailable(item.getId(), true)
                .stream()
                .map(a -> MenuItemResponse.AddonResponse.builder()
                        .id(a.getId())
                        .name(a.getName())
                        .extraPrice(a.getExtraPrice())
                        .isAvailable(a.getIsAvailable())
                        .build())
                .toList();

        return MenuItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .category(item.getCategory())
                .imageUrl(item.getImageUrl())
                .isAvailable(item.getIsAvailable())
                .addons(addons)
                .build();
    }
}
