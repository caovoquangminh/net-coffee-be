package com.netcoffee.repository;

import com.netcoffee.entity.TPricingPlanEntity;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PricingPlanRepository extends JpaRepository<TPricingPlanEntity, Long> {

    List<TPricingPlanEntity> findByIsActiveTrue();

    // machineZone-specific plan wins over null-zone fallback; highest id breaks ties
    @Query(
            "SELECT p FROM TPricingPlanEntity p WHERE p.isActive = true AND (p.machineZone = :zone OR p.machineZone IS NULL) AND (p.applicableFrom IS NULL OR p.applicableFrom <= :time) AND (p.applicableTo IS NULL OR p.applicableTo >= :time) ORDER BY CASE WHEN p.machineZone IS NOT NULL THEN 0 ELSE 1 END ASC, p.id DESC LIMIT 1")
    Optional<TPricingPlanEntity> findApplicablePlan(
            @Param("zone") String zone, @Param("time") LocalTime time);
}
