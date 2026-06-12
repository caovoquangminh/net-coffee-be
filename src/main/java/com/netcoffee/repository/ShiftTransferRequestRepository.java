package com.netcoffee.repository;

import com.netcoffee.entity.TShiftTransferRequestEntity;
import com.netcoffee.enumtype.ApprovalStatusEnum;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftTransferRequestRepository
        extends JpaRepository<TShiftTransferRequestEntity, Long> {

    List<TShiftTransferRequestEntity> findByOriginalUserId(Long userId);

    List<TShiftTransferRequestEntity> findByReplacementUserId(Long userId);

    List<TShiftTransferRequestEntity> findByStatus(ApprovalStatusEnum status);
}
