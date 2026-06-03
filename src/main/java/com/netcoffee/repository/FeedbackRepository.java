package com.netcoffee.repository;

import com.netcoffee.entity.TFeedbackEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<TFeedbackEntity, Long> {

    Page<TFeedbackEntity> findByMachineIdOrderByCreatedAtDesc(Long machineId, Pageable pageable);

    Page<TFeedbackEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
