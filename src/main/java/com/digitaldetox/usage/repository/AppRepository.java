package com.digitaldetox.usage.repository;

import com.digitaldetox.usage.entity.App;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppRepository extends JpaRepository<App, Long> {
    Optional<App> findByPackageName(String packageName);
    boolean existsByPackageName(String packageName);
}
