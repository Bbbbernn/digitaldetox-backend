package com.digitaldetox.usage.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class UsageDto {

    @Data
    public static class SessionRequest {
        @NotBlank
        private String packageName;

        @NotBlank
        private String displayName;

        @NotBlank
        private String categoryName;

        @NotNull
        @Min(1)
        private Integer durationSec;

        private LocalDate sessionDate;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionResponse {
        private Long id;
        private String appName;
        private String categoryName;
        private Integer durationSec;
        private String startTime;
        private LocalDate sessionDate;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailySummary {
        private LocalDate date;
        private int totalSeconds;
        private Map<String, Integer> byCategory;
        private List<AppUsageItem> topApps;
        private boolean limitExceeded;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WeeklySummary {
        private LocalDate weekStart;
        private LocalDate weekEnd;
        private int totalSeconds;
        private Map<String, Integer> byCategory;
        private List<DailyTotal> dailyTotals;
        private int changeVsPreviousWeekPercent;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AppUsageItem {
        private String appName;
        private String categoryName;
        private int totalSeconds;
        private double percentOfTotal;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyTotal {
        private LocalDate date;
        private int totalSeconds;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HourlyDistribution {
        private int hour;
        private int totalSeconds;
    }
}
