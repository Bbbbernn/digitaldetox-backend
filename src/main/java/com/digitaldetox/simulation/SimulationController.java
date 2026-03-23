package com.digitaldetox.simulation;

import com.digitaldetox.auth.entity.User;
import com.digitaldetox.auth.repository.UserRepository;
import com.digitaldetox.common.dto.ApiResponse;
import com.digitaldetox.focus.entity.FocusSession;
import com.digitaldetox.focus.repository.FocusSessionRepository;
import com.digitaldetox.gamification.entity.PointsLog.PointsSource;
import com.digitaldetox.gamification.service.GamificationService;
import com.digitaldetox.notification.repository.UserLimitRepository;
import com.digitaldetox.tamagotchi.service.TamagotchiService;
import com.digitaldetox.usage.entity.AppSession;
import com.digitaldetox.usage.repository.AppSessionRepository;
import com.digitaldetox.usage.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class SimulationController {

    private final MockUsageGenerator generator;
    private final UserRepository userRepository;
    private final AppSessionRepository sessionRepository;
    private final FocusSessionRepository focusRepository;
    private final GamificationService gamificationService;
    private final TamagotchiService tamagotchiService;
    private final UserLimitRepository limitRepository;
    private final CategoryRepository categoryRepository;

    private final Random rnd = new Random();

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<String>> generate(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "5") int sessionsPerDay) {
        User user = resolveUser(userDetails.getUsername());
        for (int i = days - 1; i >= 0; i--) {
            generator.generateSessionsForUser(user, LocalDate.now().minusDays(i), sessionsPerDay);
        }
        return ResponseEntity.ok(ApiResponse.success("Generati " + days + " giorni", null));
    }

    @PostMapping("/dev-reset")
    public ResponseEntity<ApiResponse<String>> devReset(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails.getUsername());
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);

        // 1. Cancella sessioni app della settimana
        List<AppSession> toDelete = sessionRepository
                .findByUserIdAndSessionDateBetweenOrderBySessionDateAsc(
                        user.getId(), weekStart, today);
        sessionRepository.deleteAll(toDelete);

        // 2. Cancella sessioni focus della settimana
        List<FocusSession> focusToDelete = focusRepository
                .findByUserIdOrderByStartedAtDesc(user.getId())
                .stream()
                .filter(f -> f.getStartedAt() != null &&
                        !f.getStartedAt().toLocalDate().isBefore(weekStart))
                .toList();
        focusRepository.deleteAll(focusToDelete);

        // 3. Genera sessioni app rispettando i limiti
        generateBalancedUsage(user, weekStart, today);

        // 4. Genera 4 sessioni focus completate
        generateFocusSessions(user, today);

        // 5. Streak +5 giorni
        user.setStreakDays(user.getStreakDays() + 5);
        user.setLastActive(today);
        userRepository.save(user);

        // 6. Trigger tamagotchi per ogni focus completato (già fatto in generateFocusSessions)

        log.info("Dev reset completato per {}: streak={}, punti={}",
                user.getUsername(), user.getStreakDays(), user.getTotalPoints());

        return ResponseEntity.ok(ApiResponse.success(
                "Reset: 7gg dati bilanciati, 4 focus, streak +5", null));
    }

    @PostMapping("/session")
    public ResponseEntity<ApiResponse<String>> generateOne(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String category) {
        User user = resolveUser(userDetails.getUsername());
        var session = generator.generateSingleSession(user, category);
        return ResponseEntity.ok(ApiResponse.success(
                "Sessione: " + session.getApp().getDisplayName(), null));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void generateBalancedUsage(User user, LocalDate from, LocalDate to) {
        // Recupera limiti per categoria — usa findByUserId e filtra
        var limits = limitRepository.findByUserId(user.getId());

        int socialLimit = limits.stream()
                .filter(l -> "SOCIAL".equals(l.getCategory().getName()))
                .findFirst().map(l -> l.getDailyLimitMin()).orElse(90);
        int videoLimit = limits.stream()
                .filter(l -> "VIDEO".equals(l.getCategory().getName()))
                .findFirst().map(l -> l.getDailyLimitMin()).orElse(120);
        int gamesLimit = limits.stream()
                .filter(l -> "GAMES".equals(l.getCategory().getName()))
                .findFirst().map(l -> l.getDailyLimitMin()).orElse(60);

        LocalDate date = from;
        while (!date.isAfter(to)) {
            // 50-85% del limite giornaliero
            int socialMin = (int)(socialLimit * (0.50 + rnd.nextDouble() * 0.35));
            int videoMin  = (int)(videoLimit  * (0.50 + rnd.nextDouble() * 0.35));
            int gamesMin  = (int)(gamesLimit  * (0.30 + rnd.nextDouble() * 0.30));
            int altroMin  = 10 + rnd.nextInt(20);

            saveSession(user, date, "SOCIAL", socialMin, 9  + rnd.nextInt(6));
            saveSession(user, date, "VIDEO",  videoMin,  14 + rnd.nextInt(6));
            saveSession(user, date, "GAMES",  gamesMin,  19 + rnd.nextInt(4));
            saveSession(user, date, "ALTRO",  altroMin,  8  + rnd.nextInt(4));

            date = date.plusDays(1);
        }
    }

    private void saveSession(User user, LocalDate date, String catName, int durationMin, int hour) {
        try {
            var app = generator.findAppByCategory(catName);
            if (app == null) return;
            LocalDateTime start = date.atTime(hour, rnd.nextInt(60));
            AppSession session = AppSession.builder()
                    .user(user).app(app)
                    .startTime(start)
                    .endTime(start.plusMinutes(durationMin))
                    .durationSec(durationMin * 60)
                    .sessionDate(date)
                    .build();
            sessionRepository.save(session);
        } catch (Exception e) {
            log.warn("Errore sessione {}: {}", catName, e.getMessage());
        }
    }

    private void generateFocusSessions(User user, LocalDate today) {
        int[]    durations = {25, 50, 20, 30};
        String[] labels    = {"Studio", "Lavoro", "Meditazione", "Lettura"};

        for (int i = 0; i < 4; i++) {
            LocalDate date = today.minusDays(1 + i);
            int durationMin = durations[i];
            LocalDateTime start = date.atTime(10 + i * 2, rnd.nextInt(30));
            int points = gamificationService.calculateFocusPoints(durationMin, true);

            FocusSession fs = FocusSession.builder()
                    .user(user).durationMin(durationMin).label(labels[i])
                    .startedAt(start).endedAt(start.plusMinutes(durationMin))
                    .completed(true).pointsEarned(points)
                    .build();
            focusRepository.save(fs);

            // Assegna punti e aggiorna tamagotchi
            gamificationService.processFocusComplete(user, durationMin);
            tamagotchiService.processFocusComplete(user);
        }
    }

    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente: " + username));
    }
}