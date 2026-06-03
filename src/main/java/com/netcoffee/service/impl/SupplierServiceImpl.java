package com.netcoffee.service.impl;

import com.netcoffee.dto.request.CreateSupplierRequest;
import com.netcoffee.dto.response.SupplierResponse;
import com.netcoffee.entity.TSupplierEntity;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.repository.SupplierRepository;
import com.netcoffee.service.SupplierService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    @Override
    @Transactional
    public SupplierResponse create(CreateSupplierRequest request) {
        TSupplierEntity entity =
                TSupplierEntity.builder()
                        .name(request.getName())
                        .phone(request.getPhone())
                        .address(request.getAddress())
                        .note(request.getNote())
                        .build();
        return toResponse(supplierRepository.save(entity));
    }

    @Override
    @Transactional
    public SupplierResponse update(Long id, CreateSupplierRequest request) {
        TSupplierEntity entity = findEntity(id);
        entity.setName(request.getName());
        entity.setPhone(request.getPhone());
        entity.setAddress(request.getAddress());
        entity.setNote(request.getNote());
        return toResponse(supplierRepository.save(entity));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        TSupplierEntity entity = findEntity(id);
        entity.setIsActive(false);
        supplierRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponse> findAll() {
        return supplierRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    private TSupplierEntity findEntity(Long id) {
        return supplierRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Nhà cung cấp không tồn tại: " + id));
    }

    private SupplierResponse toResponse(TSupplierEntity e) {
        return SupplierResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .phone(e.getPhone())
                .address(e.getAddress())
                .note(e.getNote())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
