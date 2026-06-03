package com.netcoffee.repository;

import com.netcoffee.entity.TTransactionEntity;
import com.netcoffee.enumtype.PaymentMethodEnum;
import com.netcoffee.enumtype.TransactionTypeEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TTransactionEntity, Long> {

    Page<TTransactionEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<TTransactionEntity> findBySessionId(Long sessionId);

    Optional<TTransactionEntity> findByReferenceCode(String referenceCode);

    boolean existsByReferenceCode(String referenceCode);

    List<TTransactionEntity> findByUserIdAndType(Long userId, TransactionTypeEnum type);

    @Query(
            "SELECT COALESCE(SUM(t.amount), 0) FROM TTransactionEntity t WHERE t.type = :type AND t.createdAt >= :from AND t.createdAt < :to")
    BigDecimal sumByTypeBetween(
            @Param("type") TransactionTypeEnum type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(
            "SELECT t FROM TTransactionEntity t WHERE t.createdAt >= :from ORDER BY t.createdAt DESC")
    Page<TTransactionEntity> findAllSince(@Param("from") LocalDateTime from, Pageable pageable);

    @Query(
            "SELECT COALESCE(SUM(t.amount), 0) FROM TTransactionEntity t"
                    + " WHERE t.type = :type AND t.paymentMethod = :method"
                    + " AND t.createdAt >= :from AND t.createdAt < :to")
    BigDecimal sumByTypeAndMethodBetween(
            @Param("type") TransactionTypeEnum type,
            @Param("method") PaymentMethodEnum method,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TTransactionEntity t WHERE t.type = :type")
    BigDecimal sumAllByType(@Param("type") TransactionTypeEnum type);

    @Query(
            "SELECT t.performedByUserId, COUNT(t), SUM(t.amount)"
                    + " FROM TTransactionEntity t"
                    + " WHERE t.type = 'TOPUP' AND t.performedByUserId IS NOT NULL"
                    + " AND t.createdAt >= :from AND t.createdAt < :to"
                    + " GROUP BY t.performedByUserId")
    List<Object[]> staffTopUpStatsBetween(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(
            nativeQuery = true,
            value =
                    "SELECT CAST(created_at AS DATE) AS day, COALESCE(SUM(amount), 0) AS total"
                            + " FROM transactions"
                            + " WHERE type = :type AND created_at >= :from AND created_at < :to"
                            + " GROUP BY CAST(created_at AS DATE) ORDER BY day")
    List<Object[]> dailySumByTypeBetween(
            @Param("type") String type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(
            nativeQuery = true,
            value =
                    "SELECT CAST(created_at AS DATE) AS day, payment_method,"
                            + " COALESCE(SUM(amount), 0) AS total"
                            + " FROM transactions"
                            + " WHERE type = 'TOPUP' AND created_at >= :from AND created_at < :to"
                            + " GROUP BY CAST(created_at AS DATE), payment_method ORDER BY day")
    List<Object[]> dailyTopUpByMethodBetween(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(
            nativeQuery = true,
            value =
                    "SELECT u.phone_number, u.full_name, u.balance,"
                            + " COALESCE(SUM(t.amount), 0) AS spent"
                            + " FROM users u JOIN transactions t ON t.user_id = u.id"
                            + " WHERE u.role = 'CUSTOMER' AND t.type = 'DEDUCT'"
                            + " AND t.created_at >= :from AND t.created_at < :to"
                            + " GROUP BY u.phone_number, u.full_name, u.balance"
                            + " ORDER BY spent DESC LIMIT 50")
    List<Object[]> topCustomersBySpendingBetween(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
