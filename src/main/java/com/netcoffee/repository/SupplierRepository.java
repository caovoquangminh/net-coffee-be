package com.netcoffee.repository;

import com.netcoffee.entity.TSupplierEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<TSupplierEntity, Long> {
    List<TSupplierEntity> findByIsActiveTrue();
}
