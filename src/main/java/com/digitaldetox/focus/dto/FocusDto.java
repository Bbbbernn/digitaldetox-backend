package com.digitaldetox.focus.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class FocusDto {

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class StartRequest {
        @NotNull
        @Min(5) @Max(180)
        private Integer durationMin;

        @NotBlank
        private String label;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class FocusResponse {
        private Long id;
        private int durationMin;
        private String label;
        private String startedAt;
        private String endedAt;
        private boolean completed;
        private int pointsEarned;
        private String status;  // ACTIVE, COMPLETED, ABANDONED
    }
}
