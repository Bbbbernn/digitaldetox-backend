package com.digitaldetox.tamagotchi.entity;

import com.digitaldetox.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tamagotchi")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tamagotchi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EvolutionStage evolutionStage = EvolutionStage.EGG;

    @Builder.Default
    private int happiness = 70;   // 0-100

    @Builder.Default
    private int health = 70;      // 0-100

    @Builder.Default
    private int energy = 80;      // 0-100

    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();

    // --- Metodi di comodo ---

    public void applyDelta(int deltaHappiness, int deltaHealth, int deltaEnergy) {
        this.happiness = clamp(this.happiness + deltaHappiness);
        this.health    = clamp(this.health    + deltaHealth);
        this.energy    = clamp(this.energy    + deltaEnergy);
        this.lastUpdated = LocalDateTime.now();
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public boolean isUnhappy() {
        return happiness < 30 || health < 30;
    }

    public String getMoodEmoji() {
        if (happiness >= 70) return "😄";
        if (happiness >= 40) return "😐";
        if (happiness >= 20) return "😟";
        return "😢";
    }

    public enum EvolutionStage {
        EGG,         // 0-2 giorni streak
        BABY,        // 3-6 giorni streak
        CHILD,       // 7-13 giorni streak
        TEEN,        // 14-29 giorni streak
        ADULT,       // 30+ giorni streak
        LEGENDARY    // 60+ giorni streak
    }
}
