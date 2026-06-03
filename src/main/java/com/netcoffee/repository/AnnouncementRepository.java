package com.netcoffee.repository;

import com.netcoffee.entity.TAnnouncementEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<TAnnouncementEntity, Long> {
    List<TAnnouncementEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
