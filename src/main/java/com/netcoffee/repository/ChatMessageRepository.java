package com.netcoffee.repository;

import com.netcoffee.entity.TChatMessageEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<TChatMessageEntity, Long> {

    /** Tin gần nhất của một máy (mọi khách). Dùng cho admin xem hội thoại của máy. */
    @Query(
            "SELECT m FROM TChatMessageEntity m WHERE m.machineId = :machineId ORDER BY m.createdAt"
                    + " DESC")
    List<TChatMessageEntity> findRecentByMachine(
            @Param("machineId") Long machineId, Pageable pageable);

    /** Tin gần nhất của đúng cặp (máy, khách) — tránh rò rỉ hội thoại của khách trước. */
    @Query(
            "SELECT m FROM TChatMessageEntity m WHERE m.machineId = :machineId AND m.userId ="
                    + " :userId ORDER BY m.createdAt DESC")
    List<TChatMessageEntity> findRecentByMachineAndUser(
            @Param("machineId") Long machineId, @Param("userId") Long userId, Pageable pageable);
}
