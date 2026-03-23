package com.digitaldetox.co2.service;

import com.digitaldetox.auth.entity.User;
import com.digitaldetox.auth.repository.UserRepository;
import com.digitaldetox.co2.dto.Co2Dto;
import com.digitaldetox.usage.repository.AppSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class Co2Service {

    private final AppSessionRepository sessionRepository;
    private final UserRepository userRepository;

    /*
     * Formula di stima CO₂:
     * Un dispositivo mobile consuma in media ~0.5W in uso attivo.
     * La rete mobile (4G) aggiunge ~0.1W per MB trasferito.
     * Stima semplificata: ~0.6W per ora di utilizzo.
     * Intensità carbonica media europea: ~300g CO₂/kWh (2024).
     * → CO₂ per ora di utilizzo: 0.6W × 300g/kWh = 0.18g CO₂/ora ≈ 0.003g/min
     *
     * Valore arrotondato e conservativo: 0.005g CO₂ per minuto di utilizzo
     * (include data center, CDN e consumo rete)
     */
    private static final double CO2_GRAMS_PER_MINUTE = 0.005;

    public Co2Dto.Co2Summary getToday(String username) {
        return getSummary(username, LocalDate.now(), LocalDate.now());
    }

    public Co2Dto.Co2Summary getWeek(String username) {
        LocalDate today = LocalDate.now();
        return getSummary(username, today.minusDays(6), today);
    }

    public Co2Dto.Co2Summary getPeriod(String username, LocalDate from, LocalDate to) {
        return getSummary(username, from, to);
    }

    private Co2Dto.Co2Summary getSummary(String username, LocalDate from, LocalDate to) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        List<Object[]> dailyRaw = sessionRepository.sumDurationByDateForPeriod(user.getId(), from, to);

        int totalUsedSec = dailyRaw.stream()
                .mapToInt(r -> ((Number) r[1]).intValue())
                .sum();

        // Stima baseline: utilizzo medio di riferimento = 6 ore/giorno (media italiana smartphone 2024)
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        int baselineSec = (int)(days * 6 * 3600);

        int savedSec = Math.max(0, baselineSec - totalUsedSec);
        double savedMinutes = savedSec / 60.0;
        double co2SavedGrams = savedMinutes * CO2_GRAMS_PER_MINUTE;

        return Co2Dto.Co2Summary.builder()
                .from(from)
                .to(to)
                .totalUsageMin((int)(totalUsedSec / 60))
                .baselineMin((int)(baselineSec / 60))
                .savedMin((int) savedMinutes)
                .co2SavedGrams(Math.round(co2SavedGrams * 100.0) / 100.0)
                .co2SavedLabel(formatCo2(co2SavedGrams))
                .equivalent(buildEquivalent(co2SavedGrams))
                .build();
    }

    private String formatCo2(double grams) {
        if (grams >= 1000) return String.format("%.2f kg CO₂", grams / 1000);
        return String.format("%.1f g CO₂", grams);
    }

    private String buildEquivalent(double grams) {
        // 1 km in auto ≈ 120g CO₂; 1 ora di streaming ≈ 36g CO₂
        if (grams >= 120) return String.format("Come non percorrere %.1f km in auto", grams / 120.0);
        if (grams >= 36)  return String.format("Come %.1f ore di streaming in meno", grams / 36.0);
        return String.format("%.0f caricabatterie del telefono", grams / 0.005);
    }
}