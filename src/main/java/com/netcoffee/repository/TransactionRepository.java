package com.netcoffee.repository;

import com.netcoffee.entity.TTransactionEntity;
import com.netcoffee.enumtype.TransactionTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
