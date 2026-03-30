package com.digitaldetox.tamagotchi.service;

import com.digitaldetox.auth.entity.User;
import com.digitaldetox.auth.repository.UserRepository;
import com.digitaldetox.tamagotchi.entity.TamagotchiEvent;
import com.digitaldetox.tamagotchi.repository.TamagotchiEventRepository;
import com.digitaldetox.usage.repository.AppSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyTamagotchiScheduler {

    private final UserRepository userRepository;
    private final AppSessionRepository sessionRepository;
    private final TamagotchiService tamagotchiService;
    private final TamagotchiEventRepository tamagotchiEventRepository;

    // Ogni giorno alle 23:59 ora italiana
    @Scheduled(cron = "0 59 23 * * *", zone = "Europe/Rome")
    @Transactional
    public void applyDailyTamagotchiEffect() {
        log.info("DailyTamagotchiScheduler → avvio elaborazione giornaliera");

        List<User> users = userRepository.findAll();
        LocalDate today = LocalDate.now();

        for (User user : users) {
            try {
                // Salta se già applicato oggi
                long eventsToday = tamagotchiEventRepository
                        .countUsageEventsByUserIdAndDate(user.getId(), today);
                if (eventsToday > 0) {
                    log.debug("Effetto già applicato oggi per {}", user.getUsername());
                    continue;
                }

                // Calcola totale uso oggi
                List<Object[]> dailyRaw = sessionRepository
                        .sumDurationByDateForPeriod(user.getId(), today, today);
                int totalMin = dailyRaw.stream()
                        .mapToInt(r -> ((Number) r[1]).intValue())
                        .sum() / 60;

                if (totalMin == 0) {
                    log.debug("Nessun utilizzo oggi per {}", user.getUsername());
                    continue;
                }

                if (totalMin > 360) {
                    tamagotchiService.processOveruse(user);
                    log.info("Overuse applicato a {}: {}min", user.getUsername(), totalMin);
                } else {
                    tamagotchiService.processGoodHabit(user);
                    log.info("GoodHabit applicato a {}: {}min", user.getUsername(), totalMin);
                }

            } catch (Exception e) {
                log.error("Errore elaborazione tamagotchi per {}: {}", user.getUsername(), e.getMessage());
            }
        }

        log.info("DailyTamagotchiScheduler → completato per {} utenti", users.size());
    }
}