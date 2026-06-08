package com.netcoffee.repository;

import com.netcoffee.entity.TOvertimeRequestEntity;
import com.netcoffee.enumtype.OvertimeStatusEnum;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OvertimeRequestRepository extends JpaRepository<TOvertimeRequestEntity, Long> {

    List<TOvertimeRequestEntity> findByRequesterId(Long requesterId);

    List<TOvertimeRequestEntity> findByRequesterIdAndShiftId(Long requesterId, Long shiftId);

    List<TOvertimeRequestEntity> findByStatus(OvertimeStatusEnum status);

    List<TOvertimeRequestEntity> findByShiftId(Long shiftId);
}
