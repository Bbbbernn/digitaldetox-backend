package com.digitaldetox.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class GamificationDto {

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class PointsSummary {
        private int totalPoints;
        private int streakDays;
        private int nextStreakMilestone;
        private int pointsToNextReward;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Reward {
        private String id;
        private String name;
        private int cost;
        private String description;
        private boolean unlocked;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class RedeemRequest {
        private String rewardId;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class PointsLogEntry {
        private Long id;
        private String source;
        private int points;
        private String description;
        private String occurredAt;
    }
}