package com.netcoffee.repository;

import com.netcoffee.entity.TPayrollPeriodEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollPeriodRepository extends JpaRepository<TPayrollPeriodEntity, Long> {

    Optional<TPayrollPeriodEntity> findByYearAndMonth(int year, int month);
}
