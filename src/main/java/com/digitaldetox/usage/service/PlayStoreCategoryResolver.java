package com.digitaldetox.usage.service;

import com.digitaldetox.usage.entity.App;
import com.digitaldetox.usage.entity.Category;
import com.digitaldetox.usage.repository.AppRepository;
import com.digitaldetox.usage.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayStoreCategoryResolver {

    private final AppRepository appRepository;
    private final CategoryRepository categoryRepository;

    private static final String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=";

    private String mapPlayStoreCategory(String playStoreCategory) {
        if (playStoreCategory == null) return "ALTRO";
        String cat = playStoreCategory.toLowerCase();
        if (cat.contains("social") || cat.contains("communication") || cat.contains("messaging")) return "SOCIAL";
        if (cat.contains("video") || cat.contains("entertainment") || cat.contains("streaming")) return "VIDEO";
        if (cat.contains("game") || cat.contains("puzzle") || cat.contains("arcade") || cat.contains("racing")) return "GAMES";
        return "ALTRO";
    }

    @Async
    @Transactional
    public void resolveAndUpdate(Long appId) {
        App app = appRepository.findById(appId).orElse(null);
        if (app == null) {
            log.warn("PlayStoreResolver → app non trovata per id {}", appId);
            return;
        }

        try {
            log.debug("PlayStoreResolver → fetch categoria per {}", app.getPackageName());

            Document doc = Jsoup.connect(PLAY_STORE_URL + app.getPackageName())
                    .userAgent("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .timeout(5000)
                    .get();

            Element genreEl = doc.selectFirst("[itemprop=genre]");
            if (genreEl == null) {
                log.debug("PlayStoreResolver → nessun genre trovato per {}", app.getPackageName());
                return;
            }

            String playStoreCategory = genreEl.text();
            String ourCategory = mapPlayStoreCategory(playStoreCategory);
            log.debug("PlayStoreResolver → {} → '{}' → {}", app.getPackageName(), playStoreCategory, ourCategory);

            if (app.getCategory().getName().equals(ourCategory)) {
                log.debug("PlayStoreResolver → categoria già corretta per {}", app.getPackageName());
                return;
            }

            Category category = categoryRepository.findByName(ourCategory)
                    .orElseGet(() -> categoryRepository.save(
                            Category.builder()
                                    .name(ourCategory)
                                    .icon("📱")
                                    .build()
                    ));

            app.setCategory(category);
            appRepository.save(app);
            log.debug("PlayStoreResolver → categoria aggiornata: {} → {}", app.getPackageName(), ourCategory);

        } catch (Exception e) {
            log.warn("PlayStoreResolver → impossibile risolvere categoria per {}: {}", app.getPackageName(), e.getMessage());
        }
    }
}