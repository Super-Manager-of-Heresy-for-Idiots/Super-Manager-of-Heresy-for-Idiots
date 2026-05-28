package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "homebrew_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomebrewTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
