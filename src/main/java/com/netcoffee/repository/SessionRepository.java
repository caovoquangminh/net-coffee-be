package com.netcoffee.repository;

import com.netcoffee.entity.TSessionEntity;
import com.netcoffee.enumtype.SessionStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Modifying
    @Query("UPDATE TSessionEntity s SET s.lastBilledAt = :lastBilledAt WHERE s.id = :id")
    void updateLastBilledAt(@Param("id") Long id, @Param("lastBilledAt") LocalDateTime lastBilledAt);

    @Query("SELECT s FROM TSessionEntity s WHERE s.status != 'ACTIVE' AND s.createdAt >= :from ORDER BY s.createdAt DESC")
    Page<TSessionEntity> findEndedSince(@Param("from") LocalDateTime from, Pageable pageable);
}
