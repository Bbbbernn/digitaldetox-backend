package com.digitaldetox.co2.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

public class Co2Dto {

    @Data @Builder
    public static class Co2Summary {
        private LocalDate from;
        private LocalDate to;
        private int totalUsageMin;
        private int baselineMin;
        private int savedMin;
        private double co2SavedGrams;
        private String co2SavedLabel;
        private String equivalent;
    }

    @Data @Builder
    public static class Co2Consumed {
        private LocalDate from;
        private LocalDate to;
        private int totalUsageMin;
        private double co2Grams;
        private String co2Label;
        private String equivalent;
        private List<DailyCo2> daily;
        private int changeVsPreviousPeriodPercent;
    }

    @Data @Builder
    public static class DailyCo2 {
        private LocalDate date;
        private int usageMin;
        private double co2Grams;
    }
}