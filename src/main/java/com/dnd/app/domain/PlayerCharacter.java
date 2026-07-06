package com.dnd.app.domain;

import com.dnd.app.domain.enums.CharacterStatus;
import com.dnd.app.domain.enums.ScoreMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "characters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "total_level", nullable = false)
    @Builder.Default
    private Integer totalLevel = 1;

    @Column(nullable = false)
    @Builder.Default
    private Long experience = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private CharacterStatus status = CharacterStatus.ACTIVE;

    @Column(name = "current_hp")
    private Integer currentHp;

    @Column(name = "max_hp")
    private Integer maxHp;

    @Column(name = "temp_hp", nullable = false)
    @Builder.Default
    private Integer tempHp = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private com.dnd.app.domain.content.Species race;

    @Column(name = "selected_lineage_id")
    private UUID selectedLineageId;

    @Column(name = "race_snapshot_json", columnDefinition = "text")
    private String raceSnapshotJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    // Set when this character is a pre-built template attached to a blueprint.
    // Invariant (enforced in service): blueprint and campaign cannot both be set.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_id")
    private CampaignBlueprint blueprint;

    @OneToMany(mappedBy = "character", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Builder.Default
    private List<CharacterClassLevel> classLevels = new ArrayList<>();

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CharacterStat> stats = new ArrayList<>();

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CharacterActiveEffect> activeEffects = new ArrayList<>();

    @OneToMany(mappedBy = "ownerCharacter")
    @Builder.Default
    private List<ItemInstance> itemInstances = new ArrayList<>();

    @Column(name = "player_name", length = 100)
    private String playerName;

    @Column(name = "proficiencies", columnDefinition = "text")
    private String proficiencies;

    @Column(name = "equipment", columnDefinition = "text")
    private String equipment;

    @Column(length = 40)
    private String alignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "background_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Background background;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Column(name = "armor_class")
    private Integer armorClass;

    @Column(name = "speed")
    private Integer speed;

    @Column(name = "inspiration")
    @Builder.Default
    private Boolean inspiration = false;

    @Column(name = "hit_dice_type", length = 10)
    private String hitDiceType;

    @Column(name = "hit_dice_total", length = 20)
    private String hitDiceTotal;

    @Column(name = "death_save_successes")
    @Builder.Default
    private Integer deathSaveSuccesses = 0;

    @Column(name = "death_save_failures")
    @Builder.Default
    private Integer deathSaveFailures = 0;

    @Column(name = "saving_throw_proficiency_stat_ids_json", columnDefinition = "text")
    private String savingThrowProficiencyStatIdsJson;

    @Column(name = "biography_json", columnDefinition = "text")
    private String biographyJson;

    @Column(name = "features", columnDefinition = "text")
    private String features;

    @Column(name = "attacks_json", columnDefinition = "text")
    private String attacksJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_method", length = 20)
    private ScoreMethod scoreMethod;

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CharacterSkillProficiency> skillProficiencies = new ArrayList<>();

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CharacterKnownSpell> knownSpells = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Applies a signed HP change in place using D&D damage/heal rules, so every write path stays
     * consistent: a negative delta drains temp HP first and then current HP (floored at 0); a positive
     * delta heals current HP, capped at {@code maxHpCap} when it is positive (a non-positive cap means
     * the max is unknown, so healing is left uncapped). Null current/temp HP are treated as 0.
     *
     * <p>Concurrency: callers must load the row under a pessimistic write lock
     * ({@link com.dnd.app.repository.PlayerCharacterRepository#findByIdForUpdate}) so simultaneous
     * damage/heal events accumulate instead of overwriting one another.
     */
    public void applyHpDelta(int delta, int maxHpCap) {
        int curHp = currentHp != null ? currentHp : 0;
        int tmpHp = tempHp != null ? tempHp : 0;
        if (delta < 0) {
            int damage = -delta;
            int absorbed = Math.min(tmpHp, damage);
            tmpHp -= absorbed;
            curHp = Math.max(0, curHp - (damage - absorbed));
        } else if (delta > 0) {
            curHp = maxHpCap > 0 ? Math.min(curHp + delta, maxHpCap) : curHp + delta;
        }
        this.currentHp = curHp;
        this.tempHp = tmpHp;
    }

    /**
     * Grants temporary hit points. Temp HP does not stack (D&D rule): the character keeps whichever
     * pool is larger, so a smaller grant is ignored and a larger one replaces the current temp HP.
     * A non-positive amount is a no-op. Callers must hold the same pessimistic write lock as
     * {@link #applyHpDelta(int, int)} so concurrent grants/damage stay consistent.
     */
    public void grantTempHp(int amount) {
        if (amount <= 0) {
            return;
        }
        int tmp = tempHp != null ? tempHp : 0;
        if (amount > tmp) {
            this.tempHp = amount;
        }
    }
}
