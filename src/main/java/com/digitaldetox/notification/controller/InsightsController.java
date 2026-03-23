package com.digitaldetox.notification.controller;

import com.digitaldetox.common.dto.ApiResponse;
import com.digitaldetox.notification.service.InsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightsController {

    private final InsightsService insightsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> getInsights(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                insightsService.generateInsights(userDetails.getUsername())));
    }
}
