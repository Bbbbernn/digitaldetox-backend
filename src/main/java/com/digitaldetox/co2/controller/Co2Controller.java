package com.digitaldetox.co2.controller;

import com.digitaldetox.co2.dto.Co2Dto;
import com.digitaldetox.co2.service.Co2Service;
import com.digitaldetox.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/co2")
@RequiredArgsConstructor
public class Co2Controller {

    private final Co2Service co2Service;

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Co2Dto.Co2Summary>> getToday(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                co2Service.getToday(userDetails.getUsername())));
    }

    @GetMapping("/week")
    public ResponseEntity<ApiResponse<Co2Dto.Co2Summary>> getWeek(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                co2Service.getWeek(userDetails.getUsername())));
    }

    @GetMapping("/period")
    public ResponseEntity<ApiResponse<Co2Dto.Co2Summary>> getPeriod(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                co2Service.getPeriod(userDetails.getUsername(), from, to)));
    }

    @GetMapping("/today/consumed")
    public ResponseEntity<ApiResponse<Co2Dto.Co2Consumed>> getTodayConsumed(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                co2Service.getTodayConsumed(userDetails.getUsername())));
    }

    @GetMapping("/week/consumed")
    public ResponseEntity<ApiResponse<Co2Dto.Co2Consumed>> getWeekConsumed(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                co2Service.getWeekConsumed(userDetails.getUsername())));
    }

}
