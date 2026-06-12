package com.netcoffee.repository;

import com.netcoffee.entity.TShiftSwapRequestEntity;
import com.netcoffee.enumtype.ApprovalStatusEnum;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftSwapRequestRepository extends JpaRepository<TShiftSwapRequestEntity, Long> {

    List<TShiftSwapRequestEntity> findByFromUserId(Long fromUserId);

    List<TShiftSwapRequestEntity> findByToUserId(Long toUserId);

    List<TShiftSwapRequestEntity> findByStatus(ApprovalStatusEnum status);

    List<TShiftSwapRequestEntity> findByStatusOrderByCreatedAtDesc(ApprovalStatusEnum status);
}
