package com.netcoffee.repository;

import com.netcoffee.entity.TPricingPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PricingPlanRepository extends JpaRepository<TPricingPlanEntity, Long>
{

    List<TPricingPlanEntity> findByIsActiveTrue();

    @Query("SELECT p FROM TPricingPlanEntity p WHERE p.isActive = true AND (p.machineZone = :zone OR p.machineZone IS NULL) AND (p.applicableFrom IS NULL OR p.applicableFrom <= :time) AND (p.applicableTo IS NULL OR p.applicableTo >= :time)")
    Optional<TPricingPlanEntity> findApplicablePlan(@Param("zone") String zone, @Param("time") LocalTime time);
}
