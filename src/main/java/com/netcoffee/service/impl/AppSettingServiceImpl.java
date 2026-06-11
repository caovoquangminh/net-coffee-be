package com.netcoffee.service.impl;

import com.netcoffee.entity.TAppSettingEntity;
import com.netcoffee.repository.AppSettingRepository;
import com.netcoffee.service.AppSettingService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppSettingServiceImpl implements AppSettingService {

    private final AppSettingRepository repository;

    @Override
    @Transactional(readOnly = true)
    public String get(String key, String defaultValue) {
        return repository
                .findById(key)
                .map(TAppSettingEntity::getSettingValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        String v = get(key, null);
        if (v == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(v.trim());
        } catch (NumberFormatException e) {
            log.warn("Setting {} không phải số hợp lệ: {} — dùng mặc định", key, v);
            return defaultValue;
        }
    }

    @Override
    @Transactional
    public void set(String key, String value) {
        TAppSettingEntity e =
                repository
                        .findById(key)
                        .orElseGet(() -> TAppSettingEntity.builder().settingKey(key).build());
        e.setSettingValue(value);
        repository.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getAll() {
        Map<String, String> out = new LinkedHashMap<>();
        repository.findAll().forEach(s -> out.put(s.getSettingKey(), s.getSettingValue()));
        return out;
    }
}
