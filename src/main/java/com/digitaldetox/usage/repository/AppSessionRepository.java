package com.digitaldetox.usage.repository;

import com.digitaldetox.usage.entity.AppSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppSessionRepository extends JpaRepository<AppSession, Long> {

    List<AppSession> findByUserIdAndSessionDateOrderByStartTimeDesc(Long userId, LocalDate date);

    List<AppSession> findByUserIdAndSessionDateBetweenOrderBySessionDateAsc(
            Long userId, LocalDate from, LocalDate to);

    @Query("""
        SELECT s FROM AppSession s
        WHERE s.user.id = :userId
        AND s.sessionDate = :date
        AND s.app.category.name = :categoryName
    """)
    List<AppSession> findByUserIdAndDateAndCategory(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("categoryName") String categoryName);

    @Query("""
        SELECT s.app.category.name, SUM(s.durationSec)
        FROM AppSession s
        WHERE s.user.id = :userId
        AND s.sessionDate = :date
        GROUP BY s.app.category.name
    """)
    List<Object[]> sumDurationByCategoryForDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("""
        SELECT s.app.displayName, s.app.category.name, SUM(s.durationSec)
        FROM AppSession s
        WHERE s.user.id = :userId
        AND s.sessionDate BETWEEN :from AND :to
        GROUP BY s.app.id, s.app.displayName, s.app.category.name
        ORDER BY SUM(s.durationSec) DESC
    """)
    List<Object[]> sumDurationByAppForPeriod(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
        SELECT s.sessionDate, SUM(s.durationSec)
        FROM AppSession s
        WHERE s.user.id = :userId
        AND s.sessionDate BETWEEN :from AND :to
        GROUP BY s.sessionDate
        ORDER BY s.sessionDate ASC
    """)
    List<Object[]> sumDurationByDateForPeriod(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
        SELECT HOUR(s.startTime), SUM(s.durationSec)
        FROM AppSession s
        WHERE s.user.id = :userId
        AND s.sessionDate BETWEEN :from AND :to
        GROUP BY HOUR(s.startTime)
        ORDER BY HOUR(s.startTime)
    """)
    List<Object[]> usageByHourOfDay(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    Optional<AppSession> findByUserIdAndAppIdAndSessionDate(Long userId, Long appId, LocalDate sessionDate);

    @Query("SELECT MIN(s.sessionDate) FROM AppSession s WHERE s.user.id = :userId")
    Optional<LocalDate> findFirstSessionDate(@Param("userId") Long userId);

    @Query("""
    SELECT s.app.category.name, SUM(s.durationSec)
    FROM AppSession s
    WHERE s.user.id = :userId
    AND s.sessionDate BETWEEN :from AND :to
    GROUP BY s.app.category.name
""")
    List<Object[]> sumDurationByCategoryForPeriod(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
    SELECT s.app.packageName, s.app.displayName, s.app.category.name, SUM(s.durationSec)
    FROM AppSession s
    WHERE s.user.id = :userId
    AND s.sessionDate BETWEEN :from AND :to
    GROUP BY s.app.id, s.app.packageName, s.app.displayName, s.app.category.name
    ORDER BY SUM(s.durationSec) DESC
""")
    List<Object[]> sumDurationByAppForPeriodWithPackage(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

}
