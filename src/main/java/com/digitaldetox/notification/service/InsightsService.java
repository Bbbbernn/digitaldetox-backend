package com.digitaldetox.notification.service;

import com.digitaldetox.auth.entity.User;
import com.digitaldetox.auth.repository.UserRepository;
import com.digitaldetox.usage.repository.AppSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InsightsService {

    private final AppSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public List<String> generateInsights(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        List<String> insights = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(7);
        LocalDate fourteenDaysAgo = today.minusDays(14);

        // Insight 1: ora di punta
        List<Object[]> hourly = sessionRepository.usageByHourOfDay(user.getId(), sevenDaysAgo, today);
        if (!hourly.isEmpty()) {
            Object[] peak = hourly.stream()
                    .max(Comparator.comparingLong(r -> ((Number) r[1]).longValue()))
                    .orElse(null);
            if (peak != null) {
                int peakHour = ((Number) peak[0]).intValue();
                insights.add(String.format(
                        "📊 Tendi a usare il telefono di più tra le %02d:00 e le %02d:00.",
                        peakHour, (peakHour + 1) % 24));
            }
        }

        // Insight 2: confronto settimana corrente vs precedente
        List<Object[]> thisWeek = sessionRepository.sumDurationByDateForPeriod(user.getId(), sevenDaysAgo, today);
        List<Object[]> lastWeek = sessionRepository.sumDurationByDateForPeriod(user.getId(), fourteenDaysAgo, sevenDaysAgo.minusDays(1));

        int thisTotal = thisWeek.stream().mapToInt(r -> ((Number) r[1]).intValue()).sum();
        int lastTotal = lastWeek.stream().mapToInt(r -> ((Number) r[1]).intValue()).sum();

        if (lastTotal > 0) {
            int changePercent = (int)(((double)(thisTotal - lastTotal) / lastTotal) * 100);
            if (changePercent < -10) {
                insights.add(String.format(
                        "📉 Questa settimana hai ridotto l'utilizzo del %d%% rispetto alla scorsa. Ottimo lavoro!",
                        Math.abs(changePercent)));
            } else if (changePercent > 10) {
                insights.add(String.format(
                        "📈 Questa settimana hai aumentato l'utilizzo del %d%% rispetto alla scorsa. Prova a ridurlo!",
                        changePercent));
            } else {
                insights.add("➡️ Il tuo utilizzo questa settimana è rimasto stabile rispetto alla scorsa.");
            }
        }

        // Insight 3: categoria dominante
        List<Object[]> byCategory = sessionRepository.sumDurationByAppForPeriod(user.getId(), sevenDaysAgo, today);
        Map<String, Integer> catTotals = new HashMap<>();
        for (Object[] row : byCategory) {
            String cat = (String) row[1];
            int secs = ((Number) row[2]).intValue();
            catTotals.merge(cat, secs, Integer::sum);
        }
        catTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(e -> {
                    int minutes = e.getValue() / 60;
                    insights.add(String.format(
                            "📱 La categoria più usata questa settimana è %s con %d minuti totali.",
                            e.getKey().toLowerCase(), minutes));
                });

        // Insight 4: streak motivazionale
        if (user.getStreakDays() >= 7) {
            insights.add(String.format(
                    "🔥 Sei in streak da %d giorni! Continua così per far evolvere il tuo Tamagotchi.",
                    user.getStreakDays()));
        } else if (user.getStreakDays() == 0) {
            insights.add("💪 Inizia oggi la tua streak! Rispetta il limite giornaliero per guadagnare punti.");
        }

        if (insights.isEmpty()) {
            insights.add("📋 Accumula più dati di utilizzo per ricevere insights personalizzati.");
        }

        return insights;
    }
}
