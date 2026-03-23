package com.digitaldetox.notification.controller;

import com.digitaldetox.auth.entity.User;
import com.digitaldetox.auth.repository.UserRepository;
import com.digitaldetox.common.dto.ApiResponse;
import com.digitaldetox.common.exception.ResourceNotFoundException;
import com.digitaldetox.notification.dto.NotificationDto;
import com.digitaldetox.notification.entity.UserLimit;
import com.digitaldetox.notification.repository.UserLimitRepository;
import com.digitaldetox.notification.service.NotificationService;
import com.digitaldetox.usage.entity.Category;
import com.digitaldetox.usage.repository.CategoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/limits")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserLimitRepository limitRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDto.LimitStatus>>> getLimits(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        List<NotificationDto.LimitStatus> statuses =
                notificationService.getAllLimitStatuses(user, LocalDate.now());
        return ResponseEntity.ok(ApiResponse.success(statuses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> setLimit(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NotificationDto.LimitRequest request) {

        User user = resolveUser(userDetails.getUsername());
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", request.getCategoryId()));

        // Upsert: aggiorna se esiste, crea se non esiste
        UserLimit limit = limitRepository
                .findByUserIdAndCategoryId(user.getId(), category.getId())
                .orElseGet(() -> UserLimit.builder().user(user).category(category).build());

        limit.setDailyLimitMin(request.getDailyLimitMin());
        limit.setNotifyEnabled(request.isNotifyEnabled());
        limitRepository.save(limit);

        return ResponseEntity.ok(ApiResponse.success(
                "Limite impostato: " + category.getName() + " → " + request.getDailyLimitMin() + " min/giorno", null));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<String>> deleteLimit(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long categoryId) {
        User user = resolveUser(userDetails.getUsername());
        limitRepository.deleteByUserIdAndCategoryId(user.getId(), categoryId);
        return ResponseEntity.ok(ApiResponse.success("Limite rimosso", null));
    }

    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
    }
}
