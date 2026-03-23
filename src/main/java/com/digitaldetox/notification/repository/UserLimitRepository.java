package com.digitaldetox.notification.repository;

import com.digitaldetox.notification.entity.UserLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLimitRepository extends JpaRepository<UserLimit, Long> {
    List<UserLimit> findByUserId(Long userId);
    Optional<UserLimit> findByUserIdAndCategoryId(Long userId, Long categoryId);
    void deleteByUserIdAndCategoryId(Long userId, Long categoryId);
}
