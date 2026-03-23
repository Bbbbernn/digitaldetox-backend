package com.digitaldetox.tamagotchi.repository;

import com.digitaldetox.tamagotchi.entity.Tamagotchi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TamagotchiRepository extends JpaRepository<Tamagotchi, Long> {
    Optional<Tamagotchi> findByUserId(Long userId);
    Optional<Tamagotchi> findByUserUsername(String username);
}
