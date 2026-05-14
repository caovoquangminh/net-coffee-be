package com.netcoffee.service.impl;

import com.netcoffee.entity.TMenuItemEntity;
import com.netcoffee.exception.ResourceNotFoundException;
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

    @Override
    @Transactional(readOnly = true)
    public List<TMenuItemEntity> findAvailable() {
        return menuItemRepository.findByIsAvailableTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TMenuItemEntity> findByCategory(String category) {
        return menuItemRepository.findByCategoryAndIsAvailableTrue(category);
    }

    @Override
    @Transactional(readOnly = true)
    public TMenuItemEntity findById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Món không tồn tại: " + id));
    }
}
