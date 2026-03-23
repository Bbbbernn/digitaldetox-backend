package com.digitaldetox.usage.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "apps")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class App {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String packageName;  // es. com.instagram.android

    @Column(nullable = false)
    private String displayName;  // es. Instagram

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;
}
