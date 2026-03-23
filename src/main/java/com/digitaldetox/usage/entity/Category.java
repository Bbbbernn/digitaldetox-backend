package com.digitaldetox.usage.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;  // es. SOCIAL, GAMES, PRODUCTIVITY, OTHER

    private String icon;  // emoji o nome icona frontend

    private String description;
}
