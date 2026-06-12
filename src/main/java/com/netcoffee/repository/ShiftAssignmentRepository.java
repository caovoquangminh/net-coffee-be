package com.netcoffee.repository;

import com.netcoffee.entity.TShiftAssignmentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftAssignmentRepository extends JpaRepository<TShiftAssignmentEntity, Long> {

    Optional<TShiftAssignmentEntity> findByShiftIdAndUserId(Long shiftId, Long userId);

    List<TShiftAssignmentEntity> findByShiftId(Long shiftId);
}
