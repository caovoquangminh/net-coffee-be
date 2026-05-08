package com.netcoffee.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(
    name = "price_plans",
    indexes = {
        @Index(name = "idx_price_plans_is_active", columnList = "is_active"),
        @Index(name = "idx_price_plans_machine_zone", columnList = "machine_zone")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TPricingPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Đơn vị: VND/giờ
     * Dùng BigDecimal thay Double để tránh floating point error
     */
    @Column(name = "price_per_hour", nullable = false, precision = 15, scale = 2)
    private BigDecimal pricePerHour;

    /**
     * Dùng LocalTime thay String để:
     * 1. Validate đúng format tự động
     * 2. Compare time dễ dàng trong business logic
     * Ví dụ: 08:00, 22:00
     */
    @Column(name = "applicable_from")
    private LocalTime applicableFrom;

    @Column(name = "applicable_to")
    private LocalTime applicableTo;

    @Column(name = "machine_zone", length = 50)
    private String machineZone;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
