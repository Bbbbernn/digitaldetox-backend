package com.digitaldetox.gamification.repository;

import com.digitaldetox.gamification.entity.PointsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointsLogRepository extends JpaRepository<PointsLog, Long> {
    List<PointsLog> findByUserIdOrderByOccurredAtDesc(Long userId);
    List<PointsLog> findTop20ByUserIdOrderByOccurredAtDesc(Long userId);
}