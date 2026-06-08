package com.netcoffee.repository;

import com.netcoffee.entity.TPayrollRecordEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollRecordRepository extends JpaRepository<TPayrollRecordEntity, Long> {

    List<TPayrollRecordEntity> findByPeriodId(Long periodId);

    Optional<TPayrollRecordEntity> findByUserIdAndPeriodId(Long userId, Long periodId);

    List<TPayrollRecordEntity> findByUserId(Long userId);
}
