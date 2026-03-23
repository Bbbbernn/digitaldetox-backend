package com.digitaldetox.notification.entity;

import com.digitaldetox.auth.entity.User;
import com.digitaldetox.usage.entity.Category;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_limits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private int dailyLimitMin;

    @Builder.Default
    private boolean notifyEnabled = true;
}
