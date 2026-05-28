package com.netcoffee.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.*;

@Entity
@Table(
        name = "feedbacks",
        indexes = {
            @Index(name = "idx_feedbacks_user_id", columnList = "user_id"),
            @Index(name = "idx_feedbacks_machine_id", columnList = "machine_id"),
            @Index(name = "idx_feedbacks_session_id", columnList = "session_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TFeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "content", length = 1000)
    private String content;

    /** Rating từ 1-5, validate ở service layer */
    @Column(name = "rating", columnDefinition = "SMALLINT")
    private Short rating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
