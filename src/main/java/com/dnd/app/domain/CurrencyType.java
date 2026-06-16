package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "currency")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "currency_id")
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    @Column(name = "name_ru", nullable = false, columnDefinition = "text")
    private String nameRu;

    @Column(name = "name_en", columnDefinition = "text")
    private String nameEn;

    @Column(name = "abbr_ru", nullable = false, columnDefinition = "text")
    private String abbrRu;

    @Column(name = "abbr_en", columnDefinition = "text")
    private String abbrEn;

    @Column(name = "copper_value", nullable = false, precision = 12, scale = 3)
    private BigDecimal copperValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;
}
