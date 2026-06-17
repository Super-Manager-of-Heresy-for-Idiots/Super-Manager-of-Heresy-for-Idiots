# Deployment Notes — New Content Model Migration

> Scope: operational guidance for deploying the backend **while the non-bestiary PHB
> content migration is in progress** (legacy plural tables → new normalized singular
> tables). These notes exist because the running schema is in a transitional hybrid
> state and a few normally-harmless settings become unsafe.

---

## 1. `ddl-auto` MUST stay `validate` (never `update`)

Schema is owned **exclusively by Liquibase** (`classpath:db/changelog/master.xml`).
Hibernate must never alter the schema.

- `application.yml` → `spring.jpa.hibernate.ddl-auto: validate`
- `k8s/base/app/configmap.yaml` → `SPRING_JPA_HIBERNATE_DDL_AUTO: validate`
- prod overlay (`k8s/overlays/prod/patches/app-config.yaml`) only patches
  `FORMAT_SQL`/logging — it does **not** override `DDL_AUTO`.

**Why `ddl-auto=update` is unsafe during this migration:**

1. During the hybrid period legacy entities (`CharacterClass`, `ProficiencySkill`,
   `ClassLevelReward`, `CharacterAcquiredReward`, …) and the new content entities
   (`character_class`, `class_feature`, `class_level_reward_group`, …) coexist and map
   overlapping concepts. `update` would try to "reconcile" both into one schema and can
   silently create columns/tables or drop the wrong ones.
2. Several runtime→new-content associations are intentionally **constraint-free**
   (see §3). `update` would attempt to materialize FK constraints Hibernate thinks are
   missing, against data that has not been migrated yet — producing constraint
   violations or orphan FKs.
3. `update` is order-dependent and non-idempotent across pods; with rolling deploys
   (multi-pod) two instances could race on DDL. Liquibase changesets are versioned and
   locked, so they are safe; Hibernate auto-DDL is not.

`validate` only checks that the mapped entities match the Liquibase-managed schema and
fails fast on drift. This is exactly what we want during the migration.

---

## 2. Do NOT set `hibernate.dialect` explicitly

There is **no** `hibernate.dialect=PostgreSQLDialect` anywhere in
`application*.yml`, `application.properties`, `Dockerfile`, `docker-compose.yml`, or the
`k8s/` configs — and none should be added.

Spring Boot + Hibernate 6 auto-detect the correct PostgreSQL dialect from the JDBC
connection. Pinning a legacy dialect class (e.g. `PostgreSQLDialect` /
`PostgreSQL10Dialect`) can downgrade type handling and provoke spurious
schema-validation mismatches against the Liquibase schema, which then surfaces as
startup `validate` failures. Leave dialect resolution to auto-detection.

---

## 3. Why transitional associations use `NO_CONSTRAINT`

The following entity associations point from **legacy runtime tables** to the **new
content tables** while data has not yet been migrated (Phase 10). They are annotated
with `@JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))` so
Hibernate does **not** generate a physical FK constraint:

| Entity | Association |
|---|---|
| `CharacterStat` | `statType` |
| `CharacterClass` | `primaryAbilityStat` |
| `CharacterClass` | `spellcastingStat` |
| `ProficiencySkill` | `governingStat` |
| `BuffDebuff` | `targetStat` |
| `CharacterWallet` | `currencyType` |
| `WalletTransaction` | `currencyType` |
| `QuestReward` | `currencyType` |
| `BlueprintReward` | `currencyType` |
| `PlayerCharacter` | `background` |
| `CharacterKnownSpell` | `spell` |
| `Skill` | `damageType` |
| `ItemType` | `damageType` |
| `ItemTemplate` | `damageType` |
| `ItemTemplate` | `rarity` |
| `EnchantmentType` | `damageType` |

**Reason:** the legacy rows still hold legacy IDs. A real FK would reject those rows the
moment we point the association at the new singular tables. `NO_CONSTRAINT` lets the app
boot and read both worlds during the transition. The constraints are added back
**only after** Phase 10 migrates the runtime IDs and Phase 12 validates there are no
dangling references.

---

## 4. Expected startup warnings (and when they disappear)

During the hybrid period the following are **expected** and not errors:

- Hibernate may log informational notes about associations without backing FK
  constraints — expected for every association in §3.
- Liquibase reports already-applied changesets (`054`–`059`) as ran; no checksum errors
  should appear (the `054` checksum issue was fixed in Phase 1).

These disappear once:

- the runtime data migration (Phase 10) maps legacy IDs to new content IDs, and
- the transitional `NO_CONSTRAINT` markers are removed and real FK constraints are added
  (Phase 12).

**A clean startup during migration means:** no Liquibase checksum errors, no Spring Data
query-derivation errors, no `GenerationTarget encountered exception accepting command`
for transitional FKs, and `GET /actuator/health` returns `200`.

---

## 5. Deploy checklist (migration period)

- [ ] `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` in every environment (verify the active
      ConfigMap/overlay, not just `application.yml`).
- [ ] No `hibernate.dialect` override injected via env/secret/JVM args.
- [ ] Liquibase master changelog is the single schema source.
- [ ] Build the jar before building the image (`./gradlew bootJar -x test`).
- [ ] Health endpoint green before serving traffic.
- [ ] Do not enable any "auto-fix schema" tooling against the migration database.
