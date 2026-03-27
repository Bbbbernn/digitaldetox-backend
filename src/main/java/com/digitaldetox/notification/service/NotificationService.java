package com.digitaldetox.notification.service;

import com.digitaldetox.auth.entity.User;
import com.digitaldetox.notification.dto.NotificationDto;
import com.digitaldetox.notification.entity.UserLimit;
import com.digitaldetox.notification.repository.UserLimitRepository;
import com.digitaldetox.tamagotchi.service.TamagotchiService;
import com.digitaldetox.usage.entity.Category;
import com.digitaldetox.usage.repository.AppSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final UserLimitRepository limitRepository;
    private final AppSessionRepository sessionRepository;
    private final TamagotchiService tamagotchiService;

    @Transactional(readOnly = true)
    public List<NotificationDto.LimitAlert> checkLimitsForUser(User user, Category category, LocalDate date) {
        List<NotificationDto.LimitAlert> alerts = new ArrayList<>();

        limitRepository.findByUserIdAndCategoryId(user.getId(), category.getId())
                .ifPresent(limit -> {
                    int usedSec = sessionRepository
                            .sumDurationByCategoryForDate(user.getId(), date)
                            .stream()
                            .filter(row -> category.getName().equals(row[0]))
                            .mapToInt(row -> ((Number) row[1]).intValue())
                            .sum();

                    int limitSec = limit.getDailyLimitMin() * 60;
                    if (usedSec >= limitSec && limit.isNotifyEnabled()) {
                        int usedMin = usedSec / 60;
                        log.info("Limite superato per {}: {} min su {} min di {}",
                                user.getUsername(), usedMin, limit.getDailyLimitMin(), category.getName());
                        tamagotchiService.processOveruse(user);

                        alerts.add(NotificationDto.LimitAlert.builder()
                                .categoryName(category.getName())
                                .limitMin(limit.getDailyLimitMin())
                                .usedMin(usedMin)
                                .message("Hai usato " + category.getName().toLowerCase()
                                        + " per " + usedMin + " minuti oggi. Limite: "
                                        + limit.getDailyLimitMin() + " min.")
                                .build());
                    }
                });

        return alerts;
    }

    @Transactional(readOnly = true)
    public boolean isAnyLimitExceeded(User user, LocalDate date) {
        List<UserLimit> limits = limitRepository.findByUserId(user.getId());
        List<Object[]> usage = sessionRepository.sumDurationByCategoryForDate(user.getId(), date);

        return limits.stream().anyMatch(limit -> {
            int usedSec = usage.stream()
                    .filter(row -> limit.getCategory().getName().equals(row[0]))
                    .mapToInt(row -> ((Number) row[1]).intValue())
                    .sum();
            return usedSec >= limit.getDailyLimitMin() * 60;
        });
    }

    @Transactional(readOnly = true)
    public List<NotificationDto.LimitStatus> getAllLimitStatuses(User user, LocalDate date) {
        List<UserLimit> limits = limitRepository.findByUserId(user.getId());
        List<Object[]> usage = sessionRepository.sumDurationByCategoryForDate(user.getId(), date);
        List<NotificationDto.LimitStatus> statuses = new ArrayList<>();

        for (UserLimit limit : limits) {
            int usedSec = usage.stream()
                    .filter(row -> limit.getCategory().getName().equals(row[0]))
                    .mapToInt(row -> ((Number) row[1]).intValue())
                    .sum();
            int limitSec = limit.getDailyLimitMin() * 60;
            statuses.add(NotificationDto.LimitStatus.builder()
                    .categoryName(limit.getCategory().getName())
                    .limitMin(limit.getDailyLimitMin())
                    .usedMin(usedSec / 60)
                    .percentUsed(limitSec > 0 ? (int)((double) usedSec / limitSec * 100) : 0)
                    .exceeded(usedSec >= limitSec)
                    .build());
        }
        return statuses;
    }
}
