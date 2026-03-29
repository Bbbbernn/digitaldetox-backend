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

    private static double getCo2PerMinute(String category) {
        return switch (category.toUpperCase()) {
            case "VIDEO"  -> 0.025;
            case "SOCIAL" -> 0.015;
            case "GAMES"  -> 0.010;
            default       -> 0.005;
        };
    }

    public Co2Dto.Co2Summary getToday(String username) {
        return getSavedSummary(username, LocalDate.now(), LocalDate.now());
    }

    public Co2Dto.Co2Summary getWeek(String username) {
        return getSavedSummary(username, LocalDate.now().minusDays(6), LocalDate.now());
    }

    public Co2Dto.Co2Summary getPeriod(String username, LocalDate from, LocalDate to) {
        return getSavedSummary(username, from, to);
    }

    public Co2Dto.Co2Consumed getTodayConsumed(String username) {
        return getConsumedSummary(username, LocalDate.now(), LocalDate.now());
    }

    public Co2Dto.Co2Consumed getWeekConsumed(String username) {
        return getConsumedSummary(username, LocalDate.now().minusDays(6), LocalDate.now());
    }

    private Co2Dto.Co2Summary getSavedSummary(String username, LocalDate from, LocalDate to) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        List<Object[]> dailyRaw = sessionRepository.sumDurationByDateForPeriod(user.getId(), from, to);
        int totalUsedSec = dailyRaw.stream().mapToInt(r -> ((Number) r[1]).intValue()).sum();

        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        int baselineSec = (int)(days * 6 * 3600);
        int savedSec = Math.max(0, baselineSec - totalUsedSec);
        double savedMinutes = savedSec / 60.0;
        double co2SavedGrams = savedMinutes * 0.005;

        return Co2Dto.Co2Summary.builder()
                .from(from).to(to)
                .totalUsageMin((int)(totalUsedSec / 60))
                .baselineMin((int)(baselineSec / 60))
                .savedMin((int) savedMinutes)
                .co2SavedGrams(Math.round(co2SavedGrams * 100.0) / 100.0)
                .co2SavedLabel(formatCo2(co2SavedGrams))
                .equivalent(buildEquivalentSaved(co2SavedGrams))
                .build();
    }

    private Co2Dto.Co2Consumed getConsumedSummary(String username, LocalDate from, LocalDate to) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        // Calcola CO2 per categoria
        List<Object[]> byCategoryRaw = sessionRepository.sumDurationByCategoryForPeriod(user.getId(), from, to);
        double co2Grams = 0;
        int totalUsedSec = 0;
        for (Object[] row : byCategoryRaw) {
            String category = (String) row[0];
            int secs = ((Number) row[1]).intValue();
            totalUsedSec += secs;
            co2Grams += (secs / 60.0) * getCo2PerMinute(category);
        }
        co2Grams = Math.round(co2Grams * 100.0) / 100.0;

        // Giornaliero per il grafico — usa categoria media per semplicità
        List<Object[]> dailyRaw = sessionRepository.sumDurationByDateForPeriod(user.getId(), from, to);
        double finalCo2Grams = co2Grams;
        int finalTotalUsedSec = totalUsedSec;
        List<Co2Dto.DailyCo2> daily = dailyRaw.stream()
                .map(r -> {
                    LocalDate date = (LocalDate) r[0];
                    int secs = ((Number) r[1]).intValue();
                    double ratio = finalTotalUsedSec > 0 ? (double) secs / finalTotalUsedSec : 0;
                    double dayGrams = Math.round(finalCo2Grams * ratio * 100.0) / 100.0;
                    return Co2Dto.DailyCo2.builder()
                            .date(date)
                            .usageMin(secs / 60)
                            .co2Grams(dayGrams)
                            .build();
                })
                .toList();

        // Settimana precedente per confronto
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevFrom = from.minusDays(days);
        LocalDate prevTo = from.minusDays(1);
        List<Object[]> prevByCategory = sessionRepository.sumDurationByCategoryForPeriod(user.getId(), prevFrom, prevTo);
        double prevCo2 = 0;
        for (Object[] row : prevByCategory) {
            String category = (String) row[0];
            int secs = ((Number) row[1]).intValue();
            prevCo2 += (secs / 60.0) * getCo2PerMinute(category);
        }

        int changePercent = prevCo2 > 0
                ? (int)(((co2Grams - prevCo2) / prevCo2) * 100)
                : 0;

        return Co2Dto.Co2Consumed.builder()
                .from(from).to(to)
                .totalUsageMin(totalUsedSec / 60)
                .co2Grams(co2Grams)
                .co2Label(formatCo2(co2Grams))
                .equivalent(buildEquivalentConsumed(co2Grams))
                .daily(daily)
                .changeVsPreviousPeriodPercent(changePercent)
                .build();
    }

    private String formatCo2(double grams) {
        if (grams >= 1000) return String.format("%.2f kg", grams / 1000);
        return String.format("%.2f g", grams);
    }

    private String buildEquivalentConsumed(double grams) {
        if (grams >= 1000) {
            return String.format("Come guidare per %.0f km su un'autostrada 🛣️", grams / 120.0);
        }
        if (grams >= 500) {
            return String.format("Come %.1f ore di volo in aereo ✈️", grams / 200.0);
        }
        if (grams >= 120) {
            return String.format("Come percorrere %.1f km in auto 🚗", grams / 120.0);
        }
        if (grams >= 50) {
            return String.format("Come %.0f minuti di streaming video in 4k 📺", grams / 0.6);
        }
        if (grams >= 20) {
            return String.format("Come bollire %.0f tazze di acqua ☕", grams / 5.0);
        }
        if (grams >= 10) {
            return String.format("Come tenere accesa una lampadina LED per %.0f ore 💡", grams);
        }
        if (grams >= 3) {
            return String.format("Come caricare il telefono %.0f volte 🔋", grams / 2.0);
        }
        if (grams >= 1) {
            return String.format("Come inviare %.0f email 📧", grams / 0.004);
        }
        return "Ottimo utilizzo oggi! Il tuo impatto è minimo 🌱";
    }

    private String buildEquivalentSaved(double grams) {
        if (grams >= 120) return String.format("Come non percorrere %.1f km in auto", grams / 120.0);
        if (grams >= 36)  return String.format("Come %.1f ore di streaming in meno", grams / 36.0);
        return String.format("%.0f caricabatterie del telefono", grams / 0.3);
    }
}