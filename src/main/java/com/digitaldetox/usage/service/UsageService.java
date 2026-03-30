package com.digitaldetox.usage.service;

import com.digitaldetox.auth.entity.User;
import com.digitaldetox.auth.repository.UserRepository;
import com.digitaldetox.common.exception.ResourceNotFoundException;
import com.digitaldetox.gamification.service.GamificationService;
import com.digitaldetox.notification.service.NotificationService;
import com.digitaldetox.usage.dto.UsageDto;
import com.digitaldetox.usage.entity.App;
import com.digitaldetox.usage.entity.AppSession;
import com.digitaldetox.usage.entity.Category;
import com.digitaldetox.usage.repository.AppRepository;
import com.digitaldetox.usage.repository.AppSessionRepository;
import com.digitaldetox.usage.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageService {

    private final AppSessionRepository sessionRepository;
    private final AppRepository appRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final GamificationService gamificationService;
    private final NotificationService notificationService;
    private final PlayStoreCategoryResolver playStoreCategoryResolver;

    @Transactional
    public UsageDto.SessionResponse recordSession(String username, UsageDto.SessionRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));

        Category category = categoryRepository.findByName(request.getCategoryName().toUpperCase())
                .orElseGet(() -> categoryRepository.save(
                        Category.builder()
                                .name(request.getCategoryName().toUpperCase())
                                .icon("📱")
                                .build()
                ));

        boolean[] isNewApp = {false};

        App app = appRepository.findByPackageName(request.getPackageName())
                .orElseGet(() -> {
                    isNewApp[0] = true;
                    return appRepository.save(
                            App.builder()
                                    .packageName(request.getPackageName())
                                    .displayName(request.getDisplayName())
                                    .category(category)
                                    .build()
                    );
                });

// Se è una nuova app, risolvi la categoria in background
        if (isNewApp[0]) {
            playStoreCategoryResolver.resolveAndUpdate(app.getId());
        }

        LocalDate sessionDate = request.getSessionDate() != null ? request.getSessionDate() : LocalDate.now();

        // Upsert: aggiorna se esiste già una riga per (user, app, data), altrimenti crea
        AppSession session = sessionRepository
                .findByUserIdAndAppIdAndSessionDate(user.getId(), app.getId(), sessionDate)
                .orElse(null);

        boolean isNewSession = session == null;

        if (session != null) {
            session.setDurationSec(request.getDurationSec());
            session.setEndTime(LocalDateTime.now());
        } else {
            LocalDateTime now = LocalDateTime.now();
            session = AppSession.builder()
                    .user(user)
                    .app(app)
                    .startTime(now.minusSeconds(request.getDurationSec()))
                    .endTime(now)
                    .durationSec(request.getDurationSec())
                    .sessionDate(sessionDate)
                    .build();
        }

        session = sessionRepository.save(session);

        gamificationService.processUsageSession(user, app.getCategory(), request.getDurationSec());
        notificationService.checkLimitsForUser(user, category, sessionDate);

        return UsageDto.SessionResponse.builder()
                .id(session.getId())
                .appName(app.getDisplayName())
                .categoryName(category.getName())
                .durationSec(session.getDurationSec())
                .startTime(session.getStartTime().toString())
                .sessionDate(session.getSessionDate())
                .build();
    }

    @Transactional(readOnly = true)
    public UsageDto.DailySummary getDailySummary(String username, LocalDate date) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        List<Object[]> byCategoryRaw = sessionRepository.sumDurationByCategoryForDate(user.getId(), date);

        Map<String, Integer> byCategory = new LinkedHashMap<>();
        int total = 0;
        for (Object[] row : byCategoryRaw) {
            String cat = (String) row[0];
            int secs = ((Number) row[1]).intValue();
            byCategory.put(cat, secs);
            total += secs;
        }

        List<Object[]> topAppsRaw = sessionRepository.sumDurationByAppForPeriodWithPackage(user.getId(), date, date);
        int finalTotal = total;
        List<UsageDto.AppUsageItem> topApps = topAppsRaw.stream()
                .limit(5)
                .map(row -> UsageDto.AppUsageItem.builder()
                        .packageName((String) row[0])
                        .appName((String) row[1])
                        .categoryName((String) row[2])
                        .totalSeconds(((Number) row[3]).intValue())
                        .percentOfTotal(finalTotal > 0 ? ((Number) row[3]).doubleValue() / finalTotal * 100 : 0)
                        .build())
                .collect(Collectors.toList());

        boolean limitExceeded = notificationService.isAnyLimitExceeded(user, date);

        return UsageDto.DailySummary.builder()
                .date(date)
                .totalSeconds(total)
                .byCategory(byCategory)
                .topApps(topApps)
                .limitExceeded(limitExceeded)
                .build();
    }

    @Transactional(readOnly = true)
    public UsageDto.WeeklySummary getWeeklySummary(String username, LocalDate weekStart) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate prevWeekStart = weekStart.minusDays(7);

        List<Object[]> dailyRaw = sessionRepository.sumDurationByDateForPeriod(user.getId(), weekStart, weekEnd);
        List<Object[]> prevWeekRaw = sessionRepository.sumDurationByDateForPeriod(user.getId(), prevWeekStart, weekStart.minusDays(1));
        List<Object[]> byCategoryRaw = sessionRepository.sumDurationByAppForPeriod(user.getId(), weekStart, weekEnd);

        int totalCurrent = dailyRaw.stream().mapToInt(r -> ((Number) r[1]).intValue()).sum();
        int totalPrev = prevWeekRaw.stream().mapToInt(r -> ((Number) r[1]).intValue()).sum();

        int changePercent = totalPrev > 0
                ? (int) (((double)(totalCurrent - totalPrev) / totalPrev) * 100)
                : 0;

        Map<String, Integer> byCategory = new LinkedHashMap<>();
        for (Object[] row : byCategoryRaw) {
            String cat = (String) row[1];
            int secs = ((Number) row[2]).intValue();
            byCategory.merge(cat, secs, Integer::sum);
        }

        List<UsageDto.DailyTotal> dailyTotals = dailyRaw.stream()
                .map(row -> UsageDto.DailyTotal.builder()
                        .date((LocalDate) row[0])
                        .totalSeconds(((Number) row[1]).intValue())
                        .build())
                .collect(Collectors.toList());

        return UsageDto.WeeklySummary.builder()
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .totalSeconds(totalCurrent)
                .byCategory(byCategory)
                .dailyTotals(dailyTotals)
                .changeVsPreviousWeekPercent(changePercent)
                .build();
    }

    @Transactional(readOnly = true)
    public List<UsageDto.HourlyDistribution> getHourlyDistribution(String username, LocalDate from, LocalDate to) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        List<Object[]> raw = sessionRepository.usageByHourOfDay(user.getId(), from, to);

        Map<Integer, Integer> hourMap = new LinkedHashMap<>();
        for (int i = 0; i < 24; i++) hourMap.put(i, 0);
        for (Object[] row : raw) {
            hourMap.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
        }

        return hourMap.entrySet().stream()
                .map(e -> UsageDto.HourlyDistribution.builder()
                        .hour(e.getKey())
                        .totalSeconds(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String getFirstSessionDate(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));
        return sessionRepository.findFirstSessionDate(user.getId())
                .map(LocalDate::toString)
                .orElse(LocalDate.now().toString());
    }
}
