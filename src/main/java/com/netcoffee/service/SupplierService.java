package com.netcoffee.service;

import com.netcoffee.dto.request.CreateSupplierRequest;
import com.netcoffee.dto.response.SupplierResponse;

import java.util.List;

public interface SupplierService {
    SupplierResponse create(CreateSupplierRequest request);
    SupplierResponse update(Long id, CreateSupplierRequest request);
    void deactivate(Long id);
    List<SupplierResponse> findAll();
    SupplierResponse findById(Long id);
}
