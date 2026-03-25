package com.digitaldetox.auth.controller;

import com.digitaldetox.auth.dto.AuthDto;
import com.digitaldetox.auth.service.AuthService;
import com.digitaldetox.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {
        AuthDto.AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Registrazione completata. Controlla la tua email per confermare l'account.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        AuthDto.AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login effettuato", response));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String token) {
        String username = authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(
                "Email verificata con successo! Puoi ora accedere all'app.", username));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDto.UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        AuthDto.UserProfileResponse profile = authService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profilo utente", profile));
    }
}