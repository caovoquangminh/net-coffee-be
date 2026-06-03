package com.netcoffee.entity;

import com.netcoffee.enumtype.MachineStatusEnum;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "machines",
        indexes = {
            @Index(name = "idx_machines_status", columnList = "status"),
            @Index(name = "idx_machines_machine_code", columnList = "machine_code", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TMachineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_code", nullable = false, unique = true, length = 20)
    private String machineCode;

    @Column(name = "machine_name", nullable = false, length = 100)
    private String machineName;

    @Column(name = "room_zone", length = 50)
    private String roomZone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MachineStatusEnum status = MachineStatusEnum.AVAILABLE;

    /**
     * Soft reference để tránh circular FK với sessions. Dùng raw ID thay vì @OneToOne để tránh vấn
     * đề bidirectional cascade.
     */
    @Column(name = "current_session_id")
    private Long currentSessionId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "mac_address", length = 17)
    private String macAddress;

    /**
     * JSON string chứa thông tin specs: { "gpu": "...", "ram": "...", "monitor": "..." } Dùng TEXT
     * thay vì JSONB để đơn giản hóa, có thể migrate sang JSONB sau
     */
    @Column(name = "specs", columnDefinition = "TEXT")
    private String specs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
