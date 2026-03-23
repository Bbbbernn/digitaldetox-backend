package com.digitaldetox.focus.controller;

import com.digitaldetox.common.dto.ApiResponse;
import com.digitaldetox.focus.dto.FocusDto;
import com.digitaldetox.focus.service.FocusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/focus")
@RequiredArgsConstructor
public class FocusController {

    private final FocusService focusService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<FocusDto.FocusResponse>> start(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FocusDto.StartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sessione focus avviata",
                focusService.startSession(userDetails.getUsername(), request)));
    }

    @PostMapping("/end")
    public ResponseEntity<ApiResponse<FocusDto.FocusResponse>> end(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "true") boolean completed) {
        return ResponseEntity.ok(ApiResponse.success(
                completed ? "Sessione completata!" : "Sessione abbandonata",
                focusService.endSession(userDetails.getUsername(), completed)));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<FocusDto.FocusResponse>> getActive(
            @AuthenticationPrincipal UserDetails userDetails) {
        FocusDto.FocusResponse active = focusService.getActiveSession(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(active));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<FocusDto.FocusResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                focusService.getHistory(userDetails.getUsername())));
    }
}
