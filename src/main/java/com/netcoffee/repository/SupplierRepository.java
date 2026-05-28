package com.netcoffee.repository;

import com.netcoffee.entity.TSupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<TSupplierEntity, Long> {
    List<TSupplierEntity> findByIsActiveTrue();
}
