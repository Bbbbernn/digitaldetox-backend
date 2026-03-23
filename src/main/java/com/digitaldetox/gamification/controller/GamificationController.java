package com.digitaldetox.gamification.controller;

import com.digitaldetox.common.dto.ApiResponse;
import com.digitaldetox.gamification.dto.GamificationDto;
import com.digitaldetox.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/points")
    public ResponseEntity<ApiResponse<GamificationDto.PointsSummary>> getPoints(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                gamificationService.getPointsSummary(userDetails.getUsername())));
    }

    @GetMapping("/rewards")
    public ResponseEntity<ApiResponse<List<GamificationDto.Reward>>> getRewards(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                gamificationService.getAvailableRewards(userDetails.getUsername())));
    }

    @GetMapping("/log")
    public ResponseEntity<ApiResponse<List<GamificationDto.PointsLogEntry>>> getLog(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                gamificationService.getPointsLog(userDetails.getUsername())));
    }
}
