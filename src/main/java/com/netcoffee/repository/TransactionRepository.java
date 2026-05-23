package com.netcoffee.repository;

import com.netcoffee.entity.TTransactionEntity;
import com.netcoffee.enumtype.TransactionTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TTransactionEntity, Long>
{

    Page<TTransactionEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<TTransactionEntity> findBySessionId(Long sessionId);

    Optional<TTransactionEntity> findByReferenceCode(String referenceCode);

    boolean existsByReferenceCode(String referenceCode);

    List<TTransactionEntity> findByUserIdAndType(Long userId, TransactionTypeEnum type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TTransactionEntity t WHERE t.type = :type AND t.createdAt >= :from AND t.createdAt < :to")
    BigDecimal sumByTypeBetween(@Param("type") TransactionTypeEnum type, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
