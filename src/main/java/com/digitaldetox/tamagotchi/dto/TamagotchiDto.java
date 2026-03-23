package com.digitaldetox.tamagotchi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class TamagotchiDto {

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class TamagotchiResponse {
        private Long id;
        private String name;
        private String evolutionStage;
        private int happiness;
        private int health;
        private int energy;
        private String moodEmoji;
        private boolean isUnhappy;
        private String lastUpdated;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class EventLog {
        private String eventType;
        private int deltaHappiness;
        private int deltaHealth;
        private int deltaEnergy;
        private String note;
        private String occurredAt;
    }
}
