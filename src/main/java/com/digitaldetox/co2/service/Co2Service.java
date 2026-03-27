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

    // 0.005g CO₂ per minuto di utilizzo smartphone
    // (consumo dispositivo ~0.6W + rete + data center, intensità carbonica EU ~300g/kWh)
    private static final double CO2_GRAMS_PER_MINUTE = 0.005;

    // ── Endpoint esistenti (CO2 risparmiata vs baseline) ──────────────────

    public Co2Dto.Co2Summary getToday(String username) {
        return getSavedSummary(username, LocalDate.now(), LocalDate.now());
    }

    public Co2Dto.Co2Summary getWeek(String username) {
        LocalDate today = LocalDate.now();
        return getSavedSummary(username, today.minusDays(6), today);
    }

    public Co2Dto.Co2Summary getPeriod(String username, LocalDate from, LocalDate to) {
        return getSavedSummary(username, from, to);
    }

    // ── Nuovi endpoint (CO2 consumata — dato reale) ───────────────────────

    public Co2Dto.Co2Consumed getTodayConsumed(String username) {
        return getConsumedSummary(username, LocalDate.now(), LocalDate.now());
    }

    public Co2Dto.Co2Consumed getWeekConsumed(String username) {
        LocalDate today = LocalDate.now();
        return getConsumedSummary(username, today.minusDays(6), today);
    }

    // ── Implementazioni ───────────────────────────────────────────────────

    private Co2Dto.Co2Summary getSavedSummary(String username, LocalDate from, LocalDate to) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        List<Object[]> dailyRaw = sessionRepository.sumDurationByDateForPeriod(user.getId(), from, to);

        int totalUsedSec = dailyRaw.stream()
                .mapToInt(r -> ((Number) r[1]).intValue())
                .sum();

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

    private Co2Dto.Co2Consumed getConsumedSummary(String username, LocalDate from, LocalDate to) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        List<Object[]> dailyRaw = sessionRepository.sumDurationByDateForPeriod(user.getId(), from, to);

        int totalUsedSec = dailyRaw.stream()
                .mapToInt(r -> ((Number) r[1]).intValue())
                .sum();

        double totalUsedMin = totalUsedSec / 60.0;
        double co2Grams = Math.round(totalUsedMin * CO2_GRAMS_PER_MINUTE * 100.0) / 100.0;

        // Giornaliero per il grafico
        List<Co2Dto.DailyCo2> daily = dailyRaw.stream()
                .map(r -> {
                    LocalDate date = (LocalDate) r[0];
                    int secs = ((Number) r[1]).intValue();
                    double grams = Math.round((secs / 60.0) * CO2_GRAMS_PER_MINUTE * 100.0) / 100.0;
                    return Co2Dto.DailyCo2.builder()
                            .date(date)
                            .usageMin(secs / 60)
                            .co2Grams(grams)
                            .build();
                })
                .toList();

        // Settimana precedente per confronto
        LocalDate prevFrom = from.minusDays(java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1);
        LocalDate prevTo = from.minusDays(1);
        List<Object[]> prevRaw = sessionRepository.sumDurationByDateForPeriod(user.getId(), prevFrom, prevTo);
        int prevSec = prevRaw.stream().mapToInt(r -> ((Number) r[1]).intValue()).sum();
        double prevGrams = Math.round((prevSec / 60.0) * CO2_GRAMS_PER_MINUTE * 100.0) / 100.0;

        int changePercent = prevGrams > 0
                ? (int)(((co2Grams - prevGrams) / prevGrams) * 100)
                : 0;

        return Co2Dto.Co2Consumed.builder()
                .from(from)
                .to(to)
                .totalUsageMin((int) totalUsedMin)
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
        if (grams >= 120) return String.format("Come percorrere %.1f km in auto", grams / 120.0);
        if (grams >= 36)  return String.format("Come %.1f ore di streaming", grams / 36.0);
        if (grams >= 1)   return String.format("Come caricare il telefono %.0f volte", grams / 0.005 / 60);
        return "Ottimo utilizzo oggi! 🌱";
    }

    private String buildEquivalent(double grams) {
        if (grams >= 120) return String.format("Come non percorrere %.1f km in auto", grams / 120.0);
        if (grams >= 36)  return String.format("Come %.1f ore di streaming in meno", grams / 36.0);
        return String.format("%.0f caricabatterie del telefono", grams / 0.005);
    }
}