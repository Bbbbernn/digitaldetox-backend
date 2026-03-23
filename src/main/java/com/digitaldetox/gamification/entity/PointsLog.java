package com.digitaldetox.gamification.entity;

import com.digitaldetox.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "points_log", indexes = {
        @Index(name = "idx_points_log_user", columnList = "user_id"),
        @Index(name = "idx_points_log_date", columnList = "occurred_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PointsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointsSource source;

    @Column(nullable = false)
    private int points;

    private String description;

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();

    public enum PointsSource {
        FOCUS_SESSION,      // sessione focus completata
        GOOD_USAGE,         // utilizzo breve di app sensibili
        STREAK_BONUS,       // bonus streak 7/30/60 giorni
        STREAK_DAILY,       // accesso giornaliero con streak attivo
        ONBOARDING          // bonus benvenuto
    }
}