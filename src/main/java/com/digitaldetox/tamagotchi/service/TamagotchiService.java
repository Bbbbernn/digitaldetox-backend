package com.digitaldetox.tamagotchi.service;

import com.digitaldetox.auth.entity.User;
import com.digitaldetox.common.exception.ResourceNotFoundException;
import com.digitaldetox.tamagotchi.dto.TamagotchiDto;
import com.digitaldetox.tamagotchi.entity.Tamagotchi;
import com.digitaldetox.tamagotchi.entity.TamagotchiEvent;
import com.digitaldetox.tamagotchi.repository.TamagotchiEventRepository;
import com.digitaldetox.tamagotchi.repository.TamagotchiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TamagotchiService {

    private final TamagotchiRepository tamagotchiRepository;
    private final TamagotchiEventRepository eventRepository;

    @Transactional(readOnly = true)
    public TamagotchiDto.TamagotchiResponse getState(String username) {
        Tamagotchi t = tamagotchiRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Tamagotchi non trovato"));
        return toResponse(t);
    }

    @Transactional
    public TamagotchiDto.TamagotchiResponse applyEvent(
            User user, TamagotchiEvent.EventType eventType,
            int deltaHappiness, int deltaHealth, int deltaEnergy, String note) {

        Tamagotchi t = tamagotchiRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tamagotchi non trovato"));

        t.applyDelta(deltaHappiness, deltaHealth, deltaEnergy);

        TamagotchiEvent event = TamagotchiEvent.builder()
                .tamagotchi(t)
                .eventType(eventType)
                .deltaHappiness(deltaHappiness)
                .deltaHealth(deltaHealth)
                .deltaEnergy(deltaEnergy)
                .note(note)
                .build();
        eventRepository.save(event);

        // Controlla evoluzione in base allo streak
        checkAndEvolve(t, user.getStreakDays());
        tamagotchiRepository.save(t);

        log.debug("Evento Tamagotchi: {} → {} | Δhap={} Δhea={} Δene={}",
                user.getUsername(), eventType, deltaHappiness, deltaHealth, deltaEnergy);

        return toResponse(t);
    }

    @Transactional
    public void processGoodHabit(User user) {
        applyEvent(user, TamagotchiEvent.EventType.GOOD_HABIT, +2, +1, +2,
                "Meno di 6 ore di schermo oggi 🌟");
    }

    @Transactional
    public void processOveruse(User user) {
        applyEvent(user, TamagotchiEvent.EventType.OVERUSE, -10, -7, -5,
                "Più di 6 ore di schermo oggi 📱");
    }

    @Transactional
    public void processFocusComplete(User user) {
        applyEvent(user, TamagotchiEvent.EventType.FOCUS_COMPLETE, +8, +5, +6,
                "Sessione focus completata! 🎯");
    }

    @Transactional
    public void processStreakBonus(User user, int streakDays) {
        int bonus = streakDays >= 30 ? 20 : 10;
        applyEvent(user, TamagotchiEvent.EventType.STREAK_BONUS, bonus, bonus, bonus,
                "🔥 " + streakDays + " giorni di streak!");
    }

    @Transactional(readOnly = true)
    public List<TamagotchiDto.EventLog> getEventLog(String username) {
        Tamagotchi t = tamagotchiRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Tamagotchi non trovato"));
        return eventRepository.findTop10ByTamagotchiIdOrderByOccurredAtDesc(t.getId())
                .stream()
                .map(e -> TamagotchiDto.EventLog.builder()
                        .eventType(e.getEventType().name())
                        .deltaHappiness(e.getDeltaHappiness())
                        .deltaHealth(e.getDeltaHealth())
                        .deltaEnergy(e.getDeltaEnergy())
                        .note(e.getNote())
                        .occurredAt(e.getOccurredAt().toString())
                        .build())
                .collect(Collectors.toList());
    }

    private void checkAndEvolve(Tamagotchi t, int streakDays) {
        Tamagotchi.EvolutionStage newStage = calculateStage(streakDays);
        if (newStage.ordinal() > t.getEvolutionStage().ordinal()) {
            log.info("Tamagotchi evoluto: {} → {}", t.getEvolutionStage(), newStage);
            t.setEvolutionStage(newStage);
            TamagotchiEvent evo = TamagotchiEvent.builder()
                    .tamagotchi(t)
                    .eventType(TamagotchiEvent.EventType.EVOLUTION)
                    .deltaHappiness(15)
                    .deltaHealth(15)
                    .deltaEnergy(15)
                    .note("Evoluzione a " + newStage.name() + "!")
                    .build();
            eventRepository.save(evo);
        }
    }

    private Tamagotchi.EvolutionStage calculateStage(int streakDays) {
        if (streakDays >= 60) return Tamagotchi.EvolutionStage.LEGENDARY;
        if (streakDays >= 30) return Tamagotchi.EvolutionStage.ADULT;
        if (streakDays >= 14) return Tamagotchi.EvolutionStage.TEEN;
        if (streakDays >= 7)  return Tamagotchi.EvolutionStage.CHILD;
        if (streakDays >= 3)  return Tamagotchi.EvolutionStage.BABY;
        return Tamagotchi.EvolutionStage.EGG;
    }

    private TamagotchiDto.TamagotchiResponse toResponse(Tamagotchi t) {
        return TamagotchiDto.TamagotchiResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .evolutionStage(t.getEvolutionStage().name())
                .happiness(t.getHappiness())
                .health(t.getHealth())
                .energy(t.getEnergy())
                .moodEmoji(t.getMoodEmoji())
                .isUnhappy(t.isUnhappy())
                .lastUpdated(t.getLastUpdated().toString())
                .build();
    }
}
