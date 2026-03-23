package com.digitaldetox.auth.service;

import com.digitaldetox.auth.dto.AuthDto;
import com.digitaldetox.auth.entity.User;
import com.digitaldetox.auth.repository.UserRepository;
import com.digitaldetox.auth.security.JwtService;
import com.digitaldetox.tamagotchi.entity.Tamagotchi;
import com.digitaldetox.tamagotchi.repository.TamagotchiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final TamagotchiRepository tamagotchiRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username già in uso: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email già in uso: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .totalPoints(0)
                .streakDays(0)
                .lastActive(LocalDate.now())
                .build();

        user = userRepository.save(user);
        log.info("Nuovo utente registrato: {}", user.getUsername());

        // Crea automaticamente il Tamagotchi per il nuovo utente
        Tamagotchi tamagotchi = Tamagotchi.builder()
                .user(user)
                .name("Detoxino")
                .evolutionStage(Tamagotchi.EvolutionStage.EGG)
                .happiness(70)
                .health(70)
                .energy(80)
                .build();
        tamagotchiRepository.save(tamagotchi);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, user);
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        // Aggiorna last active
        user.setLastActive(LocalDate.now());
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);

        log.info("Login effettuato: {}", user.getUsername());
        return buildAuthResponse(token, user);
    }

    public AuthDto.UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        return AuthDto.UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .totalPoints(user.getTotalPoints())
                .streakDays(user.getStreakDays())
                .lastActive(user.getLastActive() != null ? user.getLastActive().toString() : null)
                .build();
    }

    private AuthDto.AuthResponse buildAuthResponse(String token, User user) {
        return AuthDto.AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .totalPoints(user.getTotalPoints())
                .streakDays(user.getStreakDays())
                .build();
    }
}
