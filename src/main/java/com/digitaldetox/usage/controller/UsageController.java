package com.digitaldetox.usage.controller;

import com.digitaldetox.common.dto.ApiResponse;
import com.digitaldetox.usage.dto.UsageDto;
import com.digitaldetox.usage.service.UsageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
public class UsageController {

    private final UsageService usageService;

    @PostMapping("/session")
    public ResponseEntity<ApiResponse<UsageDto.SessionResponse>> recordSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UsageDto.SessionRequest request) {
        UsageDto.SessionResponse response = usageService.recordSession(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Sessione registrata", response));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<UsageDto.DailySummary>> getToday(
            @AuthenticationPrincipal UserDetails userDetails) {
        UsageDto.DailySummary summary = usageService.getDailySummary(userDetails.getUsername(), LocalDate.now());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<UsageDto.DailySummary>> getByDate(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UsageDto.DailySummary summary = usageService.getDailySummary(userDetails.getUsername(), date);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<UsageDto.WeeklySummary>> getWeekly(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        if (weekStart == null) {
            weekStart = LocalDate.now().with(WeekFields.of(Locale.ITALY).dayOfWeek(), 1);
        }
        UsageDto.WeeklySummary summary = usageService.getWeeklySummary(userDetails.getUsername(), weekStart);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/hourly")
    public ResponseEntity<ApiResponse<List<UsageDto.HourlyDistribution>>> getHourly(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null) from = LocalDate.now().minusDays(7);
        if (to == null) to = LocalDate.now();
        List<UsageDto.HourlyDistribution> result = usageService.getHourlyDistribution(userDetails.getUsername(), from, to);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/first-date")
    public ResponseEntity<ApiResponse<String>> getFirstDate(
            @AuthenticationPrincipal UserDetails userDetails) {
        String firstDate = usageService.getFirstSessionDate(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(firstDate));
    }

}
