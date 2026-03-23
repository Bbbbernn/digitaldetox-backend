package com.digitaldetox.simulation;

import com.digitaldetox.auth.entity.User;
import com.digitaldetox.auth.repository.UserRepository;
import com.digitaldetox.gamification.service.GamificationService;
import com.digitaldetox.notification.service.NotificationService;
import com.digitaldetox.usage.entity.App;
import com.digitaldetox.usage.entity.AppSession;
import com.digitaldetox.usage.entity.Category;
import com.digitaldetox.usage.repository.AppRepository;
import com.digitaldetox.usage.repository.AppSessionRepository;
import com.digitaldetox.usage.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class MockUsageGenerator {

    private final AppSessionRepository sessionRepository;
    private final AppRepository appRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final GamificationService gamificationService;
    private final NotificationService notificationService;

    private final Random random = new Random();

    // Catalogo app simulate con peso probabilistico (più alto = più probabile)
    private static final List<AppTemplate> APP_TEMPLATES = List.of(
            new AppTemplate("com.instagram.android",    "Instagram",        "SOCIAL",  30),
            new AppTemplate("com.zhiliaoapp.musically",  "TikTok",          "SOCIAL",  25),
            new AppTemplate("com.twitter.android",       "Twitter/X",       "SOCIAL",  15),
            new AppTemplate("com.facebook.katana",       "Facebook",        "SOCIAL",  10),
            new AppTemplate("com.whatsapp",              "WhatsApp",        "SOCIAL",  20),
            new AppTemplate("org.telegram.messenger",    "Telegram",        "SOCIAL",  15),
            new AppTemplate("com.google.youtube",        "YouTube",         "VIDEO",   30),
            new AppTemplate("com.netflix.mediaclient",   "Netflix",         "VIDEO",   15),
            new AppTemplate("com.twitch.android",        "Twitch",          "VIDEO",   10),
            new AppTemplate("com.king.candycrushsaga",   "Candy Crush",     "GAMES",   20),
            new AppTemplate("com.supercell.clashofclans","Clash of Clans",  "GAMES",   15),
            new AppTemplate("com.roblox.client",         "Roblox",          "GAMES",   10),
            new AppTemplate("com.google.chrome",         "Chrome",          "ALTRO",   15),
            new AppTemplate("com.spotify.music",         "Spotify",         "ALTRO",   12),
            new AppTemplate("com.google.maps",           "Google Maps",     "ALTRO",    8)
    );

    /**
     * Genera N sessioni casuali per un utente in una data specifica.
     * Usato dal DataSeeder e dallo ScheduledEmitter.
     */
    @Transactional
    public void generateSessionsForUser(User user, LocalDate date, int count) {
        ensureCategoriesAndApps();

        for (int i = 0; i < count; i++) {
            AppTemplate template = pickWeighted();
            Category category = categoryRepository.findByName(template.categoryName())
                    .orElseThrow();
            App app = appRepository.findByPackageName(template.packageName())
                    .orElseThrow();

            // Durata: distribuzione realistica
            // Social: 5-45 min, Video: 10-90 min, Games: 5-60 min
            int durationSec = generateDuration(template.categoryName());

            // Orario casuale nella giornata (ponderato verso sera)
            LocalDateTime startTime = randomTimeForDate(date);

            AppSession session = AppSession.builder()
                    .user(user)
                    .app(app)
                    .startTime(startTime)
                    .endTime(startTime.plusSeconds(durationSec))
                    .durationSec(durationSec)
                    .sessionDate(date)
                    .build();

            sessionRepository.save(session);

            // Trigger gamification
            gamificationService.processUsageSession(user, category, durationSec);
        }

        log.debug("Generate {} sessioni simulate per {} in data {}", count, user.getUsername(), date);
    }

    /**
     * Genera una singola sessione immediata (per test manuali via API).
     */
    @Transactional
    public AppSession generateSingleSession(User user, String categoryFilter) {
        ensureCategoriesAndApps();

        AppTemplate template = categoryFilter != null
                ? pickByCategory(categoryFilter)
                : pickWeighted();

        Category category = categoryRepository.findByName(template.categoryName()).orElseThrow();
        App app = appRepository.findByPackageName(template.packageName()).orElseThrow();

        int durationSec = generateDuration(template.categoryName());
        LocalDateTime now = LocalDateTime.now();

        AppSession session = AppSession.builder()
                .user(user)
                .app(app)
                .startTime(now.minusSeconds(durationSec))
                .endTime(now)
                .durationSec(durationSec)
                .sessionDate(LocalDate.now())
                .build();

        session = sessionRepository.save(session);
        gamificationService.processUsageSession(user, category, durationSec);
        notificationService.checkLimitsForUser(user, category, LocalDate.now());

        log.info("Sessione simulata: {} - {} - {}s", user.getUsername(), app.getDisplayName(), durationSec);
        return session;
    }

    // --- Helpers ---

    private void ensureCategoriesAndApps() {
        for (AppTemplate t : APP_TEMPLATES) {
            if (!categoryRepository.existsByName(t.categoryName())) {
                categoryRepository.save(Category.builder()
                        .name(t.categoryName())
                        .icon(categoryIcon(t.categoryName()))
                        .description(t.categoryName().toLowerCase())
                        .build());
            }
            if (!appRepository.existsByPackageName(t.packageName())) {
                Category cat = categoryRepository.findByName(t.categoryName()).orElseThrow();
                appRepository.save(App.builder()
                        .packageName(t.packageName())
                        .displayName(t.displayName())
                        .category(cat)
                        .build());
            }
        }
    }

    private AppTemplate pickWeighted() {
        int totalWeight = APP_TEMPLATES.stream().mapToInt(AppTemplate::weight).sum();
        int pick = random.nextInt(totalWeight);
        int cumulative = 0;
        for (AppTemplate t : APP_TEMPLATES) {
            cumulative += t.weight();
            if (pick < cumulative) return t;
        }
        return APP_TEMPLATES.get(0);
    }

    private AppTemplate pickByCategory(String category) {
        List<AppTemplate> filtered = APP_TEMPLATES.stream()
                .filter(t -> t.categoryName().equalsIgnoreCase(category))
                .toList();
        if (filtered.isEmpty()) return pickWeighted();
        return filtered.get(random.nextInt(filtered.size()));
    }

    private int generateDuration(String category) {
        return switch (category) {
            case "VIDEO"  -> (30 + random.nextInt(90)) * 60;
            case "GAMES"  -> (20 + random.nextInt(70)) * 60;
            case "SOCIAL" -> (25 + random.nextInt(65)) * 60;
            case "ALTRO"  -> (5  + random.nextInt(25)) * 60;
            default       -> (5  + random.nextInt(20)) * 60;
        };
    }

    private LocalDateTime randomTimeForDate(LocalDate date) {
        // Distribuzione ponderata: più attività la sera (18-23)
        int[] hours = {7,8,9,10,11,12,13,14,15,16,17,18,18,19,19,20,20,21,21,22,22,23};
        int hour = hours[random.nextInt(hours.length)];
        int minute = random.nextInt(60);
        return date.atTime(hour, minute);
    }

    private String categoryIcon(String name) {
        return switch (name) {
            case "SOCIAL" -> "👥";
            case "VIDEO"  -> "🎬";
            case "GAMES"  -> "🎮";
            case "ALTRO"  -> "📱";
            default       -> "📱";
        };
    }

    public com.digitaldetox.usage.entity.App findAppByCategory(String categoryName) {
        ensureCategoriesAndApps();
        return APP_TEMPLATES.stream()
                .filter(t -> t.categoryName().equalsIgnoreCase(categoryName))
                .findFirst()
                .map(t -> appRepository.findByPackageName(t.packageName()).orElse(null))
                .orElse(null);
    }

    public record AppTemplate(String packageName, String displayName, String categoryName, int weight) {}
}