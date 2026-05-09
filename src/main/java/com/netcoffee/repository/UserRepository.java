package com.netcoffee.repository;

import com.netcoffee.entity.TUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<TUserEntity, Long>
{

    Optional<TUserEntity> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Dùng @Modifying + @Query thay vì load entity rồi save để tránh race
     * condition khi nhiều request cùng update balance
     * 
     * @Version trên entity sẽ handle optimistic locking
     */
    @Modifying @Query("UPDATE TUserEntity u SET u.balance = u.balance + :amount WHERE u.id = :id")
    int increaseBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    @Modifying @Query("UPDATE TUserEntity u SET u.balance = u.balance - :amount, u.totalSpent = u.totalSpent + :amount WHERE u.id = :id AND u.balance >= :amount")
    int decreaseBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
