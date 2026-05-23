package com.netcoffee.repository;

import com.netcoffee.entity.TMachineEntity;
import com.netcoffee.enumtype.MachineStatusEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineRepository extends JpaRepository<TMachineEntity, Long>
{

    Optional<TMachineEntity> findByMachineCode(String machineCode);

    List<TMachineEntity> findByStatus(MachineStatusEnum status);

    List<TMachineEntity> findByRoomZone(String roomZone);

    boolean existsByMachineCode(String machineCode);

    long countByStatus(MachineStatusEnum status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM TMachineEntity m WHERE m.id = :id")
    Optional<TMachineEntity> findByIdForUpdate(@Param("id") Long id);

    @Modifying @Query("UPDATE TMachineEntity m SET m.status = :status, m.currentSessionId = :sessionId WHERE m.id = :id")
    int updateStatusAndSession(@Param("id") Long id, @Param("status") MachineStatusEnum status,
            @Param("sessionId") Long sessionId);
}
