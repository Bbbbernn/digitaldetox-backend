package com.digitaldetox.notification.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class NotificationDto {

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class LimitAlert {
        private String categoryName;
        private int limitMin;
        private int usedMin;
        private String message;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class LimitStatus {
        private String categoryName;
        private int limitMin;
        private int usedMin;
        private int percentUsed;
        private boolean exceeded;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class LimitRequest {
        @NotNull
        private Long categoryId;

        @NotNull
        @Min(1)
        private Integer dailyLimitMin;

        private boolean notifyEnabled = true;
    }
}
