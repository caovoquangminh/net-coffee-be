package com.netcoffee.repository;

import com.netcoffee.entity.TSessionEntity;
import com.netcoffee.enumtype.SessionStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<TSessionEntity, Long>
{

    Optional<TSessionEntity> findByMachineIdAndStatus(Long machineId, SessionStatusEnum status);

    Optional<TSessionEntity> findByUserIdAndStatus(Long userId, SessionStatusEnum status);

    List<TSessionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByMachineIdAndStatus(Long machineId, SessionStatusEnum status);

    @Query("SELECT s FROM TSessionEntity s WHERE s.status = 'ACTIVE' ORDER BY s.startedAt ASC")
    List<TSessionEntity> findAllActiveSessions();
}
