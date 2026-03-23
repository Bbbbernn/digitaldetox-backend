package com.digitaldetox.focus.service;

import com.digitaldetox.auth.entity.User;
import com.digitaldetox.auth.repository.UserRepository;
import com.digitaldetox.common.exception.ResourceNotFoundException;
import com.digitaldetox.focus.dto.FocusDto;
import com.digitaldetox.focus.entity.FocusSession;
import com.digitaldetox.focus.repository.FocusSessionRepository;
import com.digitaldetox.gamification.service.GamificationService;
import com.digitaldetox.tamagotchi.service.TamagotchiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FocusService {

    private final FocusSessionRepository focusRepository;
    private final UserRepository userRepository;
    private final GamificationService gamificationService;
    private final TamagotchiService tamagotchiService;

    @Transactional
    public FocusDto.FocusResponse startSession(String username, FocusDto.StartRequest request) {
        User user = resolveUser(username);

        // Chiude eventuale sessione aperta non completata senza punti
        focusRepository.findFirstByUserIdAndCompletedFalseAndEndedAtIsNullOrderByStartedAtDesc(user.getId())
                .ifPresent(old -> {
                    old.setCompleted(false);
                    old.setEndedAt(LocalDateTime.now());
                    focusRepository.save(old);
                });

        FocusSession session = FocusSession.builder()
                .user(user)
                .durationMin(request.getDurationMin())
                .label(request.getLabel())
                .startedAt(LocalDateTime.now())
                .completed(false)
                .pointsEarned(0)
                .build();

        session = focusRepository.save(session);
        log.info("Focus avviato: {} - {} min - '{}'", username, request.getDurationMin(), request.getLabel());
        return toResponse(session);
    }

    @Transactional
    public FocusDto.FocusResponse endSession(String username, boolean completed) {
        User user = resolveUser(username);

        FocusSession session = focusRepository
                .findFirstByUserIdAndCompletedFalseAndEndedAtIsNullOrderByStartedAtDesc(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nessuna sessione focus attiva"));

        LocalDateTime now = LocalDateTime.now();
        session.setEndedAt(now);
        session.setCompleted(completed);

        // Calcola minuti effettivamente trascorsi
        int elapsedMin = (int) ChronoUnit.MINUTES.between(session.getStartedAt(), now);
        int plannedMin = session.getDurationMin();

        int points;
        if (completed) {
            // Completata → usa la durata pianificata con bonus 50%
            points = gamificationService.calculateFocusPoints(plannedMin, true);
            tamagotchiService.processFocusComplete(user);
            log.info("Focus completato: {} → +{} pt ({}min)", username, points, plannedMin);
        } else {
            // Abbandonata → punti proporzionali al tempo trascorso, senza bonus
            points = gamificationService.calculateFocusPoints(elapsedMin, false);
            log.info("Focus abbandonato: {}/{} min → +{} pt", elapsedMin, plannedMin, points);
        }

        session.setPointsEarned(points);
        focusRepository.save(session);

        // Assegna punti (sia per completato che abbandonato)
        if (completed) {
            gamificationService.processFocusComplete(user, plannedMin);
        } else {
            gamificationService.processFocusAbandoned(user, plannedMin, elapsedMin);
        }

        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public FocusDto.FocusResponse getActiveSession(String username) {
        User user = resolveUser(username);
        return focusRepository
                .findFirstByUserIdAndCompletedFalseAndEndedAtIsNullOrderByStartedAtDesc(user.getId())
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<FocusDto.FocusResponse> getHistory(String username) {
        User user = resolveUser(username);
        return focusRepository.findByUserIdOrderByStartedAtDesc(user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private FocusDto.FocusResponse toResponse(FocusSession s) {
        String status = !s.isCompleted() && s.getEndedAt() == null ? "ACTIVE"
                : s.isCompleted() ? "COMPLETED" : "ABANDONED";
        return FocusDto.FocusResponse.builder()
                .id(s.getId())
                .durationMin(s.getDurationMin())
                .label(s.getLabel())
                .startedAt(s.getStartedAt().toString())
                .endedAt(s.getEndedAt() != null ? s.getEndedAt().toString() : null)
                .completed(s.isCompleted())
                .pointsEarned(s.getPointsEarned())
                .status(status)
                .build();
    }

    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
    }
}