package com.digitaldetox.simulation;

import com.digitaldetox.auth.entity.User;
import com.digitaldetox.auth.repository.UserRepository;
import com.digitaldetox.notification.entity.UserLimit;
import com.digitaldetox.notification.repository.UserLimitRepository;
import com.digitaldetox.tamagotchi.entity.Tamagotchi;
import com.digitaldetox.tamagotchi.repository.TamagotchiRepository;
import com.digitaldetox.usage.entity.Category;
import com.digitaldetox.usage.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TamagotchiRepository tamagotchiRepository;
    private final CategoryRepository categoryRepository;
    private final UserLimitRepository limitRepository;
    private final MockUsageGenerator generator;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("DataSeeder: database già popolato, skip.");
            return;
        }

        log.info("DataSeeder: popolamento database iniziale...");

        // --- Utenti ---
        User mario = createUser("mario", "mario@test.com", "password123", User.Role.USER, 340, 12);
        User lucia = createUser("lucia", "lucia@test.com", "password123", User.Role.USER, 890, 28);
        User admin = createUser("admin", "admin@test.com", "admin123",    User.Role.ADMIN, 0, 0);

        // --- Tamagotchi (mario: CHILD, lucia: TEEN) ---
        tamagotchiRepository.save(Tamagotchi.builder()
                .user(mario).name("Pixelino")
                .evolutionStage(Tamagotchi.EvolutionStage.CHILD)
                .happiness(65).health(70).energy(75).build());

        tamagotchiRepository.save(Tamagotchi.builder()
                .user(lucia).name("Detoxina")
                .evolutionStage(Tamagotchi.EvolutionStage.TEEN)
                .happiness(85).health(90).energy(88).build());

        tamagotchiRepository.save(Tamagotchi.builder()
                .user(admin).name("AdminPet")
                .evolutionStage(Tamagotchi.EvolutionStage.EGG)
                .happiness(50).health(50).energy(50).build());

        // --- Sessioni storiche: 30 giorni ---
        log.info("DataSeeder: generazione sessioni storiche per mario...");
        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            // mario: utilizzo moderato (4-7 sessioni/giorno)
            generator.generateSessionsForUser(mario, date, 4 + (int)(Math.random() * 4));
        }

        log.info("DataSeeder: generazione sessioni storiche per lucia...");
        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            // lucia: utilizzo più basso (2-4 sessioni/giorno) — utente virtuoso
            generator.generateSessionsForUser(lucia, date, 2 + (int)(Math.random() * 3));
        }

        // --- Limiti utente ---
        setLimitForCategory(mario, "SOCIAL", 60);    // 1 ora social
        setLimitForCategory(mario, "VIDEO", 90);     // 1.5 ore video
        setLimitForCategory(mario, "GAMES", 45);     // 45 min games
        setLimitForCategory(lucia, "SOCIAL", 30);    // lucia è più disciplinata
        setLimitForCategory(lucia, "VIDEO", 60);

        log.info("DataSeeder completato. Utenti creati: mario / lucia / admin (password: password123 / admin123)");
        log.info("H2 Console disponibile su: http://localhost:8080/h2-console");
    }

    private User createUser(String username, String email, String password,
                             User.Role role, int points, int streak) {
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .totalPoints(points)
                .streakDays(streak)
                .lastActive(LocalDate.now())
                .build();
        return userRepository.save(user);
    }

    private void setLimitForCategory(User user, String categoryName, int limitMin) {
        categoryRepository.findByName(categoryName).ifPresent(category -> {
            UserLimit limit = UserLimit.builder()
                    .user(user)
                    .category(category)
                    .dailyLimitMin(limitMin)
                    .notifyEnabled(true)
                    .build();
            limitRepository.save(limit);
        });
    }
}
