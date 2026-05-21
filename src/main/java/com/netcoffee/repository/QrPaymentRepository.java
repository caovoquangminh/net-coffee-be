package com.netcoffee.repository;

import com.netcoffee.entity.TQrPaymentEntity;
import com.netcoffee.enumtype.QrPaymentStatusEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QrPaymentRepository extends JpaRepository<TQrPaymentEntity, Long>
{

    Optional<TQrPaymentEntity> findByReferenceCode(String referenceCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM TQrPaymentEntity q WHERE q.referenceCode = :referenceCode")
    Optional<TQrPaymentEntity> findByReferenceCodeForUpdate(@Param("referenceCode") String referenceCode);

    List<TQrPaymentEntity> findByUserIdAndStatus(Long userId, QrPaymentStatusEnum status);

    /**
     * Dùng để scheduled job expire các QR quá hạn
     */
    @Modifying @Query("UPDATE TQrPaymentEntity q SET q.status = 'EXPIRED' WHERE q.status = 'PENDING' AND q.expiredAt < :now")
    int expireOldQrPayments(@Param("now") LocalDateTime now);
}
