package com.netcoffee.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "work_shifts",
        indexes = {@Index(name = "idx_work_shifts_date", columnList = "shift_date")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TWorkShiftEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_number", nullable = false)
    private Integer shiftNumber;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
}
