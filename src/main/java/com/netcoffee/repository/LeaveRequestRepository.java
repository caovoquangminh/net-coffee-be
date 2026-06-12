package com.netcoffee.repository;

import com.netcoffee.entity.TLeaveRequestEntity;
import com.netcoffee.enumtype.ApprovalStatusEnum;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveRequestRepository extends JpaRepository<TLeaveRequestEntity, Long> {

    List<TLeaveRequestEntity> findByUserId(Long userId);

    List<TLeaveRequestEntity> findByStatus(ApprovalStatusEnum status);

    List<TLeaveRequestEntity> findByUserIdAndLeaveDateAndStatus(
            Long userId, LocalDate leaveDate, ApprovalStatusEnum status);

    List<TLeaveRequestEntity> findByLeaveDateBetweenAndStatus(
            LocalDate from, LocalDate to, ApprovalStatusEnum status);
}
