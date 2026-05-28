package com.netcoffee.repository;

import com.netcoffee.entity.TUserEntity;
import com.netcoffee.enumtype.UserRoleEnum;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<TUserEntity, Long> {

    Optional<TUserEntity> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Dùng @Modifying + @Query thay vì load entity rồi save để tránh race condition khi nhiều
     * request cùng update balance @Version trên entity sẽ handle optimistic locking
     */
    @Modifying
    @Query("UPDATE TUserEntity u SET u.balance = u.balance + :amount WHERE u.id = :id")
    int increaseBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    @Modifying
    @Query(
            "UPDATE TUserEntity u SET u.balance = u.balance - :amount, u.totalSpent = u.totalSpent + :amount WHERE u.id = :id AND u.balance >= :amount")
    int decreaseBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    List<TUserEntity> findByPhoneNumberContainingOrderByCreatedAtDesc(String phoneNumber);

    List<TUserEntity> findByRoleOrderByCreatedAtDesc(UserRoleEnum role);

    List<TUserEntity> findByRoleAndPhoneNumberContainingOrderByCreatedAtDesc(
            UserRoleEnum role, String phoneNumber);

    /** Tất cả hội viên (CUSTOMER + STAFF), không bao gồm ADMIN. */
    @Query("SELECT u FROM TUserEntity u WHERE u.role != :excludeRole ORDER BY u.createdAt DESC")
    List<TUserEntity> findAllExcludingRole(@Param("excludeRole") UserRoleEnum excludeRole);

    @Query(
            "SELECT u FROM TUserEntity u WHERE u.role != :excludeRole AND u.phoneNumber LIKE %:phone% ORDER BY u.createdAt DESC")
    List<TUserEntity> findByPhoneContainingExcludingRole(
            @Param("phone") String phone, @Param("excludeRole") UserRoleEnum excludeRole);
}
