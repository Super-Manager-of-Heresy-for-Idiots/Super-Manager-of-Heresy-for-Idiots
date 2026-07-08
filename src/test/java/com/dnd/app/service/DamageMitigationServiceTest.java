package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.CombatantType;
import com.dnd.app.service.DamageMitigationService.DamageModifier;
import com.dnd.app.service.DamageMitigationService.Mitigation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DamageMitigationService: резист/иммунитет/уязвимость для персонажа и монстра (BTL-07)")
class DamageMitigationServiceTest {

    @Mock private ModifierAggregator modifierAggregator;

    private DamageMitigationService service;

    private final UUID fireId = UUID.randomUUID();
    private final UUID charId = UUID.randomUUID();
    private DamageType fire;

    @BeforeEach
    void setUp() {
        service = new DamageMitigationService(modifierAggregator);
        fire = DamageType.builder().id(fireId).build();
    }

    private BattleCombatant monster(List<MonsterDamageImmunity> immun,
                                    List<MonsterDamageResistance> resist,
                                    List<MonsterDamageVulnerability> vuln) {
        Monster m = Monster.builder().id(UUID.randomUUID()).nameRusloc("Демон")
                .damageImmunities(immun).damageResistances(resist).damageVulnerabilities(vuln).build();
        return BattleCombatant.builder().id(UUID.randomUUID()).type(CombatantType.MONSTER).monster(m).build();
    }

    private BattleCombatant character(double multiplier) {
        PlayerCharacter ch = PlayerCharacter.builder().id(charId).name("Герой").build();
        lenient().when(modifierAggregator.damageMultiplier(charId, fireId)).thenReturn(multiplier);
        return BattleCombatant.builder().id(UUID.randomUUID()).type(CombatantType.CHARACTER).character(ch).build();
    }

    private MonsterDamageImmunity immunity() {
        return MonsterDamageImmunity.builder().damageType(fire).build();
    }

    private MonsterDamageResistance resistance() {
        return MonsterDamageResistance.builder().damageType(fire).build();
    }

    private MonsterDamageVulnerability vulnerability() {
        return MonsterDamageVulnerability.builder().damageType(fire).build();
    }

    @Test
    @DisplayName("Монстр: none → без изменений; resist → пополам; immune → 0; vuln → вдвое")
    void monsterMatrix() {
        assertMitigation(service.mitigate(monster(List.of(), List.of(), List.of()), 10, fireId), 10, DamageModifier.NONE);
        assertMitigation(service.mitigate(monster(List.of(), List.of(resistance()), List.of()), 10, fireId), 5, DamageModifier.RESISTED);
        assertMitigation(service.mitigate(monster(List.of(immunity()), List.of(), List.of()), 10, fireId), 0, DamageModifier.IMMUNE);
        assertMitigation(service.mitigate(monster(List.of(), List.of(), List.of(vulnerability())), 10, fireId), 20, DamageModifier.VULNERABLE);
    }

    @Test
    @DisplayName("Персонаж: 1.0 → none; 0.5 → resist; 0.0 → immune; 2.0 → vuln")
    void characterMatrix() {
        assertMitigation(service.mitigate(character(1.0), 10, fireId), 10, DamageModifier.NONE);
        assertMitigation(service.mitigate(character(0.5), 10, fireId), 5, DamageModifier.RESISTED);
        assertMitigation(service.mitigate(character(0.0), 10, fireId), 0, DamageModifier.IMMUNE);
        assertMitigation(service.mitigate(character(2.0), 10, fireId), 20, DamageModifier.VULNERABLE);
    }

    @Test
    @DisplayName("Резист округляет вниз: 7 → 3")
    void resistFloors() {
        assertMitigation(service.mitigate(monster(List.of(), List.of(resistance()), List.of()), 7, fireId), 3, DamageModifier.RESISTED);
    }

    @Test
    @DisplayName("Урон без типа не модифицируется (осознанное правило) и не трогает аггрегатор")
    void untypedDamageUnchanged() {
        PlayerCharacter ch = PlayerCharacter.builder().id(charId).name("Герой").build();
        BattleCombatant target = BattleCombatant.builder()
                .id(UUID.randomUUID()).type(CombatantType.CHARACTER).character(ch).build();
        Mitigation m = service.mitigate(target, 10, null);
        assertMitigation(m, 10, DamageModifier.NONE);
        verifyNoInteractions(modifierAggregator);
    }

    private void assertMitigation(Mitigation m, int expectedDamage, DamageModifier expectedModifier) {
        assertEquals(expectedDamage, m.finalDamage());
        assertEquals(expectedModifier, m.modifier());
    }
}
