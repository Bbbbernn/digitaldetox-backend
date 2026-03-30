package com.digitaldetox.tamagotchi.repository;

import com.digitaldetox.tamagotchi.entity.TamagotchiEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TamagotchiEventRepository extends JpaRepository<TamagotchiEvent, Long> {
    List<TamagotchiEvent> findByTamagotchiIdOrderByOccurredAtDesc(Long tamagotchiId);
    List<TamagotchiEvent> findTop10ByTamagotchiIdOrderByOccurredAtDesc(Long tamagotchiId);

    @Query("""
    SELECT COUNT(e) FROM TamagotchiEvent e
    WHERE e.tamagotchi.user.id = :userId
    AND e.eventType IN ('GOOD_HABIT', 'OVERUSE')
    AND DATE(e.occurredAt) = :date
""")
    long countUsageEventsByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date);

}
