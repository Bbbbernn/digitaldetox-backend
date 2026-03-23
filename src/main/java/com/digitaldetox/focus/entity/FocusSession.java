package com.digitaldetox.focus.entity;

import com.digitaldetox.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "focus_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FocusSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int durationMin;

    private String label;  // es. "Studio", "Lavoro"

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @Builder.Default
    private boolean completed = false;

    @Builder.Default
    private int pointsEarned = 0;
}
