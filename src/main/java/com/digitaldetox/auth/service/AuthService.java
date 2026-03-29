package com.digitaldetox.auth.service;

import com.digitaldetox.auth.dto.AuthDto;
import com.digitaldetox.auth.entity.EmailVerificationToken;
import com.digitaldetox.auth.entity.User;
import com.digitaldetox.auth.repository.EmailVerificationTokenRepository;
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
import java.time.LocalDateTime;
import java.util.UUID;

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
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        // Verifica che le password coincidano
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Le password non coincidono");
        }

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
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        log.info("Nuovo utente registrato: {}", user.getUsername());

        // Crea Tamagotchi
        Tamagotchi tamagotchi = Tamagotchi.builder()
                .user(user)
                .name("Detoxino")
                .evolutionStage(Tamagotchi.EvolutionStage.EGG)
                .happiness(70)
                .health(70)
                .energy(80)
                .build();
        tamagotchiRepository.save(tamagotchi);

        // Genera token di verifica email e invia
        String token = generateVerificationToken(user);
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String jwt = jwtService.generateToken(userDetails);

        return buildAuthResponse(jwt, user);
    }

    @Transactional
    public String verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token non valido"));

        if (verificationToken.isUsed()) {
            throw new IllegalArgumentException("Token già utilizzato");
        }
        if (verificationToken.isExpired()) {
            throw new IllegalArgumentException("Token scaduto");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        log.info("Email verificata per utente: {}", user.getUsername());
        return user.getUsername();
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

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
                .emailVerified(user.isEmailVerified())
                .build();
    }

    private String generateVerificationToken(User user) {
        // Elimina eventuali token precedenti
        tokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        tokenRepository.save(verificationToken);
        return token;
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
                .emailVerified(user.isEmailVerified())
                .build();
    }

    @Transactional
    public void markOnboarded(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));
        user.setOnboarded(true);
        userRepository.save(user);
    }

}