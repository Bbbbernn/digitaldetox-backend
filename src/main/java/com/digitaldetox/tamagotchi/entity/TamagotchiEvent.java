package com.digitaldetox.tamagotchi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tamagotchi_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TamagotchiEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tamagotchi_id", nullable = false)
    private Tamagotchi tamagotchi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    private int deltaHappiness;
    private int deltaHealth;
    private int deltaEnergy;

    private String note;

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();

    public enum EventType {
        GOOD_HABIT,        // uso ridotto, streak mantenuto
        FOCUS_COMPLETE,    // sessione focus completata
        OVERUSE,           // uso eccessivo
        STREAK_BONUS,      // bonus streak (7, 30 giorni)
        DAILY_CHECK,       // controllo giornaliero automatico
        FEED,              // alimentato con punti
        EVOLUTION          // evoluzione avvenuta
    }
}
