package com.digitaldetox.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDto {

    @Data
    public static class RegisterRequest {

        @NotBlank(message = "Username obbligatorio")
        @Size(min = 3, max = 50, message = "Username deve essere tra 3 e 50 caratteri")
        private String username;

        @NotBlank(message = "Email obbligatoria")
        @Email(message = "Formato email non valido")
        private String email;

        @NotBlank(message = "Password obbligatoria")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
                message = "La password deve contenere almeno 8 caratteri, una maiuscola, una minuscola, un numero e un carattere speciale"
        )
        private String password;

        @NotBlank(message = "Conferma password obbligatoria")
        private String confirmPassword;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Username obbligatorio")
        private String username;

        @NotBlank(message = "Password obbligatoria")
        private String password;
    }

    @Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AuthResponse {
        private String token;
        private String tokenType = "Bearer";
        private Long userId;
        private String username;
        private String role;
        private int totalPoints;
        private int streakDays;
        private boolean emailVerified;
    }

    @Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class UserProfileResponse {
        private Long id;
        private String username;
        private String email;
        private String role;
        private int totalPoints;
        private int streakDays;
        private String lastActive;
        private boolean emailVerified;
    }
}