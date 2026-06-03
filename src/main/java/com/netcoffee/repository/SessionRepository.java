package com.netcoffee.repository;

import com.netcoffee.entity.TSessionEntity;
import com.netcoffee.enumtype.SessionStatusEnum;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<TSessionEntity, Long> {

    Optional<TSessionEntity> findByMachineIdAndStatus(Long machineId, SessionStatusEnum status);

    Optional<TSessionEntity> findByUserIdAndStatus(Long userId, SessionStatusEnum status);

    List<TSessionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByMachineIdAndStatus(Long machineId, SessionStatusEnum status);

    @Query("SELECT s FROM TSessionEntity s WHERE s.status = 'ACTIVE' ORDER BY s.startedAt ASC")
    List<TSessionEntity> findAllActiveSessions();

    @Modifying
    @Query("UPDATE TSessionEntity s SET s.lastBilledAt = :lastBilledAt WHERE s.id = :id")
    void updateLastBilledAt(
            @Param("id") Long id, @Param("lastBilledAt") LocalDateTime lastBilledAt);

    @Modifying
    @Query("UPDATE TSessionEntity s SET s.lastHeartbeatAt = :ts WHERE s.id = :id")
    void updateLastHeartbeatAt(@Param("id") Long id, @Param("ts") LocalDateTime ts);

    /**
     * Tìm các session ACTIVE bị coi là orphaned: - Đã nhận heartbeat nhưng heartbeat cuối cùng >
     * staleThreshold (client đã chết) - Chưa nhận heartbeat nào và startedAt > maxDurationThreshold
     * (safety net cho phiên cũ)
     */
    @Query(
            "SELECT s FROM TSessionEntity s WHERE s.status = 'ACTIVE' AND "
                    + "((s.lastHeartbeatAt IS NOT NULL AND s.lastHeartbeatAt < :staleThreshold) OR "
                    + "(s.lastHeartbeatAt IS NULL AND s.startedAt < :maxDurationThreshold))")
    List<TSessionEntity> findStaleActiveSessions(
            @Param("staleThreshold") LocalDateTime staleThreshold,
            @Param("maxDurationThreshold") LocalDateTime maxDurationThreshold);

    @Query(
            "SELECT s FROM TSessionEntity s WHERE s.status != 'ACTIVE' AND s.createdAt >= :from ORDER BY s.createdAt DESC")
    Page<TSessionEntity> findEndedSince(@Param("from") LocalDateTime from, Pageable pageable);

    @Query(
            nativeQuery = true,
            value =
                    "SELECT COALESCE(SUM("
                            + "  CASE WHEN duration_seconds IS NOT NULL THEN duration_seconds"
                            + "  ELSE GREATEST(0, EXTRACT(EPOCH FROM (:now - started_at)))"
                            + "  END"
                            + "), 0) FROM sessions WHERE started_at >= :from AND started_at < :to")
    double sumSessionSecondsBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("now") LocalDateTime now);
}
