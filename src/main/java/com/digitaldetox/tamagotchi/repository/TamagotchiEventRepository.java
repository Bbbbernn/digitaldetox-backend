package com.digitaldetox.tamagotchi.repository;

import com.digitaldetox.tamagotchi.entity.TamagotchiEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TamagotchiEventRepository extends JpaRepository<TamagotchiEvent, Long> {
    List<TamagotchiEvent> findByTamagotchiIdOrderByOccurredAtDesc(Long tamagotchiId);
    List<TamagotchiEvent> findTop10ByTamagotchiIdOrderByOccurredAtDesc(Long tamagotchiId);
}
