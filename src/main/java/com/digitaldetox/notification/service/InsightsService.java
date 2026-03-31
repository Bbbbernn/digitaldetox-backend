package com.digitaldetox.notification.service;

import com.digitaldetox.auth.entity.User;
import com.digitaldetox.auth.repository.UserRepository;
import com.digitaldetox.usage.repository.AppSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

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

        List<Object[]> thisWeek = sessionRepository.sumDurationByDateForPeriod(user.getId(), sevenDaysAgo, today);
        List<Object[]> lastWeek = sessionRepository.sumDurationByDateForPeriod(user.getId(), fourteenDaysAgo, sevenDaysAgo.minusDays(1));
        List<Object[]> hourly = sessionRepository.usageByHourOfDay(user.getId(), sevenDaysAgo, today);
        List<Object[]> byCategory = sessionRepository.sumDurationByAppForPeriod(user.getId(), sevenDaysAgo, today);

        int thisTotal = thisWeek.stream().mapToInt(r -> ((Number) r[1]).intValue()).sum();
        int lastTotal = lastWeek.stream().mapToInt(r -> ((Number) r[1]).intValue()).sum();

        Map<String, Integer> catTotals = new HashMap<>();
        for (Object[] row : byCategory) {
            String cat = (String) row[1];
            int secs = ((Number) row[2]).intValue();
            catTotals.merge(cat, secs, Integer::sum);
        }

        // Insight 1: ora di punta con contesto
        if (!hourly.isEmpty()) {
            Object[] peak = hourly.stream()
                    .max(Comparator.comparingLong(r -> ((Number) r[1]).longValue()))
                    .orElse(null);
            if (peak != null) {
                int peakHour = ((Number) peak[0]).intValue();
                String moment = peakHour >= 6 && peakHour < 12 ? "mattina" :
                        peakHour >= 12 && peakHour < 18 ? "pomeriggio" :
                                peakHour >= 18 && peakHour < 22 ? "sera" : "notte";
                insights.add(String.format(
                        "🕐 Il tuo momento di picco è alle %02d:00 (%s). Considera di mettere il telefono a faccia in giù in quell'ora.",
                        peakHour, moment));
            }
        }

        // Insight 2: confronto settimane con ore
        if (lastTotal > 0) {
            int changePercent = (int)(((double)(thisTotal - lastTotal) / lastTotal) * 100);
            int thisHours = thisTotal / 3600;
            int thisMins = (thisTotal % 3600) / 60;
            if (changePercent < -10) {
                insights.add(String.format(
                        "📉 Questa settimana hai usato il telefono %dh %dm, il %d%% in meno della scorsa. Stai migliorando!",
                        thisHours, thisMins, Math.abs(changePercent)));
            } else if (changePercent > 10) {
                insights.add(String.format(
                        "📈 Questa settimana hai usato il telefono %dh %dm, il %d%% in più della scorsa. Prova a impostare un limite.",
                        thisHours, thisMins, changePercent));
            } else {
                insights.add(String.format(
                        "➡️ Questa settimana hai usato il telefono %dh %dm, in linea con la settimana scorsa.",
                        thisHours, thisMins));
            }
        } else if (thisTotal > 0) {
            int thisHours = thisTotal / 3600;
            int thisMins = (thisTotal % 3600) / 60;
            insights.add(String.format(
                    "📊 Questa settimana hai totalizzato %dh %dm di utilizzo schermo.",
                    thisHours, thisMins));
        }

        // Insight 3: categoria dominante con percentuale
        catTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(e -> {
                    int mins = e.getValue() / 60;
                    int pct = thisTotal > 0 ? (int)((double) e.getValue() / thisTotal * 100) : 0;
                    int hours = mins / 60;
                    int remainMins = mins % 60;
                    String cat = e.getKey().toLowerCase();
                    insights.add(String.format(
                            "📱 %s occupa il %d%% del tuo tempo schermo (%dh %dm questa settimana). È la categoria più usata.",
                            cat.substring(0, 1).toUpperCase() + cat.substring(1), pct, hours, remainMins));
                });

        // Insight 4: CO2 settimanale — calcolo per categoria come Co2Service
        if (!byCategory.isEmpty()) {
            double co2 = calculateCo2Grams(byCategory);
            if (co2 >= 1) {
                insights.add(String.format(
                        "🌱 Questa settimana il tuo utilizzo ha prodotto circa %.1fg di CO₂. Equivale a tenere una lampadina LED accesa per %.0f ore.",
                        co2, co2));
            }
        }

        // Insight 5: giorno più pesante
        if (!thisWeek.isEmpty()) {
            Object[] heaviest = thisWeek.stream()
                    .max(Comparator.comparingLong(r -> ((Number) r[1]).longValue()))
                    .orElse(null);
            if (heaviest != null) {
                int secs = ((Number) heaviest[1]).intValue();
                if (secs > 3600) {
                    LocalDate date = (LocalDate) heaviest[0];
                    String dayName = date.getDayOfWeek().getDisplayName(
                            java.time.format.TextStyle.FULL, java.util.Locale.ITALIAN);
                    int hours = secs / 3600;
                    int mins = (secs % 3600) / 60;
                    insights.add(String.format(
                            "📅 Il tuo giorno più intenso questa settimana è stato %s con %dh %dm di schermo.",
                            dayName, hours, mins));
                }
            }
        }

        // Insight 6: streak motivazionale
        if (user.getStreakDays() >= 30) {
            insights.add(String.format(
                    "🏆 %d giorni di streak consecutivi! Sei tra i più costanti — il tuo compagno ti adora.",
                    user.getStreakDays()));
        } else if (user.getStreakDays() >= 7) {
            insights.add(String.format(
                    "🔥 %d giorni di streak! Ancora %d giorni e raggiungi il prossimo milestone.",
                    user.getStreakDays(), 30 - user.getStreakDays()));
        } else if (user.getStreakDays() >= 3) {
            insights.add(String.format(
                    "⚡ %d giorni consecutivi senza superare i limiti. Sei sulla buona strada!",
                    user.getStreakDays()));
        }

        // Insight 7: confronto con media italiana
        if (thisTotal > 0) {
            int avgMinPerDay = (thisTotal / 60) / 7;
            int italianAvg = 360;
            if (avgMinPerDay < italianAvg) {
                insights.add(String.format(
                        "🇮🇹 La tua media giornaliera è %dh %dm, sotto la media italiana di 6h. Ottimo controllo!",
                        avgMinPerDay / 60, avgMinPerDay % 60));
            } else {
                insights.add(String.format(
                        "🇮🇹 La tua media giornaliera è %dh %dm, sopra la media italiana di 6h. Hai margine di miglioramento.",
                        avgMinPerDay / 60, avgMinPerDay % 60));
            }
        }

        if (insights.isEmpty()) {
            insights.add("📋 Usa l'app per qualche giorno per ricevere insights personalizzati sul tuo utilizzo.");
        }

        return insights;
    }

    private double calculateCo2Grams(List<Object[]> byCategory) {
        double total = 0;
        for (Object[] row : byCategory) {
            String cat = ((String) row[1]).toUpperCase();
            int secs = ((Number) row[2]).intValue();
            double perMinute = switch (cat) {
                case "MUSIC/VIDEO" -> 0.025;
                case "SOCIAL"      -> 0.015;
                case "GAMES"       -> 0.010;
                default            -> 0.005;
            };
            total += (secs / 60.0) * perMinute;
        }
        return Math.round(total * 100.0) / 100.0;
    }
}