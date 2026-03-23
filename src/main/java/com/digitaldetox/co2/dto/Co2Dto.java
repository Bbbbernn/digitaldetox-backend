package com.digitaldetox.co2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

public class Co2Dto {

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
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
}
