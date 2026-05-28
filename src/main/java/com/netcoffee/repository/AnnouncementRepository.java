package com.netcoffee.repository;

import com.netcoffee.entity.TAnnouncementEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<TAnnouncementEntity, Long> {
    List<TAnnouncementEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
