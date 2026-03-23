package com.digitaldetox.focus.repository;

import com.digitaldetox.focus.entity.FocusSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {

    List<FocusSession> findByUserIdOrderByStartedAtDesc(Long userId);

    // Sessione ATTIVA = non completata E non ancora terminata (endedAt è null)
    Optional<FocusSession> findFirstByUserIdAndCompletedFalseAndEndedAtIsNullOrderByStartedAtDesc(Long userId);
}