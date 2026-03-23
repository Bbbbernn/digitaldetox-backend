package com.digitaldetox.simulation;

import com.digitaldetox.auth.entity.User;
import com.digitaldetox.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
@ConditionalOnProperty(name = "simulation.enabled", havingValue = "true")
public class ScheduledUsageEmitter {

    private final MockUsageGenerator generator;
    private final UserRepository userRepository;
    private final Random random = new Random();

    /**
     * Ogni 5 minuti genera 1-3 sessioni simulate per ogni utente USER attivo.
     * Simula l'utilizzo del telefono in background durante il test del prototipo.
     */
    @Scheduled(cron = "${simulation.schedule-cron:0 */5 * * * *}")
    public void emitSessions() {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.USER)
                .toList();

        for (User user : users) {
            int sessionCount = 1 + random.nextInt(3); // 1-3 sessioni
            generator.generateSessionsForUser(user, LocalDate.now(), sessionCount);
        }

        log.debug("ScheduledEmitter: generate sessioni per {} utenti", users.size());
    }
}
