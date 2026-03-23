package com.digitaldetox.tamagotchi.controller;

import com.digitaldetox.common.dto.ApiResponse;
import com.digitaldetox.common.exception.ResourceNotFoundException;
import com.digitaldetox.tamagotchi.dto.TamagotchiDto;
import com.digitaldetox.tamagotchi.entity.Tamagotchi;
import com.digitaldetox.tamagotchi.repository.TamagotchiRepository;
import com.digitaldetox.tamagotchi.service.TamagotchiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tamagotchi")
@RequiredArgsConstructor
public class TamagotchiController {

    private final TamagotchiService tamagotchiService;
    private final TamagotchiRepository tamagotchiRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<TamagotchiDto.TamagotchiResponse>> getState(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                tamagotchiService.getState(userDetails.getUsername())));
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<TamagotchiDto.EventLog>>> getEventLog(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                tamagotchiService.getEventLog(userDetails.getUsername())));
    }

    @PatchMapping("/name")
    public ResponseEntity<ApiResponse<TamagotchiDto.TamagotchiResponse>> updateName(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Nome non valido"));
        }
        Tamagotchi t = tamagotchiRepository.findByUserUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Tamagotchi non trovato"));
        t.setName(name.trim());
        tamagotchiRepository.save(t);
        return ResponseEntity.ok(ApiResponse.success(
                "Nome aggiornato",
                tamagotchiService.getState(userDetails.getUsername())));
    }
}