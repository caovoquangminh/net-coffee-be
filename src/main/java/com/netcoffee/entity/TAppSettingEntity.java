package com.netcoffee.entity;

import com.netcoffee.constant.AppConstant;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Cấu hình hệ thống dạng key-value: Telegram chat id, tham số thưởng/phạt... */
@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TAppSettingEntity {

    @Id
    @Column(name = "setting_key", length = 100)
    private String settingKey;

    @Column(name = "setting_value", length = 500)
    private String settingValue;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now(AppConstant.VN_ZONE);
    }
}
