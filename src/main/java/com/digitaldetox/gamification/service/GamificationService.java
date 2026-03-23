package com.digitaldetox.gamification.service;

import com.digitaldetox.auth.entity.User;
import com.digitaldetox.auth.repository.UserRepository;
import com.digitaldetox.gamification.dto.GamificationDto;
import com.digitaldetox.gamification.entity.PointsLog;
import com.digitaldetox.gamification.entity.PointsLog.PointsSource;
import com.digitaldetox.gamification.repository.PointsLogRepository;
import com.digitaldetox.usage.entity.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationService {

    private final UserRepository userRepository;
    private final PointsLogRepository pointsLogRepository;

    private static final int POINTS_GOOD_SESSION   = 5;
    private static final int POINTS_FOCUS_COMPLETE  = 20;
    private static final int POINTS_STREAK_7        = 50;
    private static final int POINTS_STREAK_30       = 200;
    private static final int POINTS_ONBOARDING      = 10;
    private static final int GOOD_SESSION_THRESHOLD_SEC = 1800;

    @Transactional
    public void processUsageSession(User user, Category category, int durationSec) {
        // Nessun punto per utilizzo app — i punti si guadagnano solo con
        // sessioni Focus, streak e onboarding. Usare social/games, anche
        // brevemente, non deve essere premiato.
    }

    @Transactional
    public void processFocusComplete(User user, int durationMin) {
        int points = calculateFocusPoints(durationMin, true);
        addPoints(user, points, PointsSource.FOCUS_SESSION,
                "✅ Focus completato: " + durationMin + " min");
        log.info("Focus completato: {} → +{} punti", user.getUsername(), points);
    }

    @Transactional
    public void processFocusAbandoned(User user, int plannedMin, int elapsedMin) {
        if (elapsedMin <= 0) return;
        int points = calculateFocusPoints(elapsedMin, false);
        addPoints(user, points, PointsSource.FOCUS_SESSION,
                "⏹ Focus interrotto: " + elapsedMin + "/" + plannedMin + " min");
        log.info("Focus abbandonato: {} → +{} punti ({}/{}min)",
                user.getUsername(), points, elapsedMin, plannedMin);
    }

    /**
     * Formula punti Focus:
     * - Completata: 1.5pt per minuto, minimo 5pt
     * - Abbandonata: 1pt per minuto effettivo, nessun minimo
     * Esempi completata:
     *   5 min  → max(5, 5×1.5)  = 8 pt
     *   10 min → max(5, 10×1.5) = 15 pt
     *   25 min → max(5, 25×1.5) = 38 pt
     * Esempi abbandonata:
     *   2 min su 25  → 2×1 = 2 pt
     *   10 min su 25 → 10×1 = 10 pt
     *   25 min su 50 → 25×1 = 25 pt
     */
    public int calculateFocusPoints(int minutes, boolean completed) {
        if (minutes <= 0) return 0;
        if (completed) {
            return Math.max(5, (int) Math.round(minutes * 1.5));
        } else {
            return minutes; // 1pt per minuto, nessun minimo
        }
    }

    @Transactional
    public void processOnboarding(User user) {
        addPoints(user, POINTS_ONBOARDING, PointsSource.ONBOARDING,
                "Benvenuto in DigitalDetox!");
    }

    @Transactional
    public boolean updateStreak(User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastActive = user.getLastActive();

        if (lastActive == null || lastActive.isBefore(today.minusDays(1))) {
            user.setStreakDays(1);
            log.info("Streak resettato per: {}", user.getUsername());
        } else if (lastActive.isEqual(today.minusDays(1))) {
            user.setStreakDays(user.getStreakDays() + 1);
            int streak = user.getStreakDays();
            log.info("Streak aumentato: {} → {} giorni", user.getUsername(), streak);

            if (streak == 7) {
                addPoints(user, POINTS_STREAK_7, PointsSource.STREAK_BONUS,
                        "🔥 Streak di 7 giorni raggiunto!");
            }
            if (streak == 30) {
                addPoints(user, POINTS_STREAK_30, PointsSource.STREAK_BONUS,
                        "🔥 Streak di 30 giorni raggiunto!");
            }
            if (streak % 7 == 0 && streak > 7) {
                addPoints(user, 20, PointsSource.STREAK_BONUS,
                        "🔥 Streak di " + streak + " giorni!");
            }
        }

        user.setLastActive(today);
        userRepository.save(user);
        return true;
    }

    @Transactional
    public void addPoints(User user, int points, PointsSource source, String description) {
        user.setTotalPoints(user.getTotalPoints() + points);
        userRepository.save(user);

        PointsLog log = PointsLog.builder()
                .user(user)
                .source(source)
                .points(points)
                .description(description)
                .build();
        pointsLogRepository.save(log);
    }

    // Retrocompatibilità — chiamato da codice esistente senza source
    @Transactional
    public void addPoints(User user, int points) {
        addPoints(user, points, PointsSource.GOOD_USAGE, "Punti guadagnati");
    }

    @Transactional(readOnly = true)
    public GamificationDto.PointsSummary getPointsSummary(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));
        return GamificationDto.PointsSummary.builder()
                .totalPoints(user.getTotalPoints())
                .streakDays(user.getStreakDays())
                .nextStreakMilestone(nextMilestone(user.getStreakDays()))
                .pointsToNextReward(pointsToNextReward(user.getTotalPoints()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<GamificationDto.PointsLogEntry> getPointsLog(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));
        return pointsLogRepository
                .findTop20ByUserIdOrderByOccurredAtDesc(user.getId())
                .stream()
                .map(l -> GamificationDto.PointsLogEntry.builder()
                        .id(l.getId())
                        .source(l.getSource().name())
                        .points(l.getPoints())
                        .description(l.getDescription())
                        .occurredAt(l.getOccurredAt().toString())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GamificationDto.Reward> getAvailableRewards(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return List.of(
                GamificationDto.Reward.builder().id("skin_blue").name("Skin Oceano").cost(100)
                        .description("Skin blu per il Tamagotchi")
                        .unlocked(user.getTotalPoints() >= 100).build(),
                GamificationDto.Reward.builder().id("skin_fire").name("Skin Fuoco").cost(250)
                        .description("Skin fiammante per il Tamagotchi")
                        .unlocked(user.getTotalPoints() >= 250).build(),
                GamificationDto.Reward.builder().id("puzzle_anime_01").name("Puzzle Anime #1").cost(500)
                        .description("Primo puzzle a tema anime")
                        .unlocked(user.getTotalPoints() >= 500).build(),
                GamificationDto.Reward.builder().id("puzzle_anime_02").name("Puzzle Anime #2").cost(1000)
                        .description("Secondo puzzle a tema anime")
                        .unlocked(user.getTotalPoints() >= 1000).build()
        );
    }

    private int nextMilestone(int streak) {
        if (streak < 7)  return 7;
        if (streak < 30) return 30;
        if (streak < 60) return 60;
        return 100;
    }

    private int pointsToNextReward(int points) {
        int[] thresholds = {100, 250, 500, 1000, 2000};
        for (int t : thresholds) {
            if (points < t) return t - points;
        }
        return 0;
    }
}