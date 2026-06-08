package com.netcoffee.repository;

import com.netcoffee.entity.TShiftRegistrationEntity;
import com.netcoffee.enumtype.ShiftRegistrationStatusEnum;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftRegistrationRepository extends JpaRepository<TShiftRegistrationEntity, Long> {

    List<TShiftRegistrationEntity> findByShiftId(Long shiftId);

    List<TShiftRegistrationEntity> findByShiftIdIn(Collection<Long> shiftIds);

    List<TShiftRegistrationEntity> findByUserId(Long userId);

    Optional<TShiftRegistrationEntity> findByShiftIdAndUserId(Long shiftId, Long userId);

    long countByShiftIdAndStatus(Long shiftId, ShiftRegistrationStatusEnum status);
}
