package com.netcoffee.repository;

import com.netcoffee.entity.TAppSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppSettingRepository extends JpaRepository<TAppSettingEntity, String> {}
