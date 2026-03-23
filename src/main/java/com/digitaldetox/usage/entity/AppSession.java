package com.digitaldetox.usage.entity;

import com.digitaldetox.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_sessions", indexes = {
        @Index(name = "idx_session_user_date", columnList = "user_id, session_date"),
        @Index(name = "idx_session_date", columnList = "session_date")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "app_id", nullable = false)
    private App app;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer durationSec;

    @Column(nullable = false)
    private LocalDate sessionDate;

    @PrePersist
    public void prePersist() {
        if (sessionDate == null) {
            sessionDate = startTime != null ? startTime.toLocalDate() : LocalDate.now();
        }
    }
}
