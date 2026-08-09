# CLAUDE.md

This file guides Claude Code when working in this repository (IronTrailAPI).

## Project

IronTrail API is a Spring Boot/Kotlin backend for IronTrail, a workout tracking app. It's a learning project (first Spring Boot backend) built to be the primary portfolio piece for Canadian job applications — timeline matters, applying in ~3 weeks.

The Android app (`IronTrailApp`) currently stores everything locally in Room, single-user, no auth. This API replaces that: multi-user, PostgreSQL as the permanent source of truth, deployed on AWS. The Android app will be updated later to sync against it (Room becomes an offline-first cache, not the source of truth).

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| Framework | Spring Boot (latest stable) |
| Database | PostgreSQL |
| ORM | Spring Data JPA + Hibernate |
| Build | Gradle (Kotlin DSL) |
| Containerization | Docker + docker-compose |
| Testing | JUnit + Mockito |
| CI/CD | GitHub Actions |
| Deployment | AWS |

## Project Resources

| What | Path |
|---|---|
| Android domain models (business objects) | `D:\IronTrail\IronTrailApp\app\src\main\java\com\irontrail\app\core\domain\model\` |
| Android Room entities (current local schema) | `D:\IronTrail\IronTrailApp\app\src\main\java\com\irontrail\app\core\data\local\entity\` |
| Android Room DAOs (current query patterns) | `D:\IronTrail\IronTrailApp\app\src\main\java\com\irontrail\app\core\data\local\dao\` |
| Android app's own CLAUDE.md (build commands, its skills) | `D:\IronTrail\IronTrailApp\CLAUDE.md` |
| UI mockups (7 HTML flows: home, onboarding, splits, active-workout, exercise-picker, history, settings) | `D:\IronTrail\mockups\` |
| V1 feature spec | `D:\IronTrail\IronTrail-V1-Spec.md` |
| Project spec (V1/V2/V3 roadmap) | `D:\IronTrail\IronTrail-Project-Spec.md` |
| Exercise library source | Wger API (external) |

**Do not re-read the Android domain model/entity/DAO directories** — the full inventory is captured below in "Android Domain Model Snapshot" (2026-08-02) and is the source of truth for backend schema design. That part of `IronTrailApp` is frozen until this project (IronTrailAPI) is done, so the snapshot won't go stale. Only re-read Android source directly if you need to verify one specific field/detail the snapshot doesn't cover, or if Sagar says the Android side changed. The mockups and spec docs are lighter-weight and fine to read on demand — no snapshot needed for those.

**Caveat on the two spec docs:** they describe the Android app's *current* single-user, offline-only, no-auth architecture — that part is superseded here. Use them for **domain/feature understanding** (what a Split, WorkoutDay, Template, Session, Set, PR actually mean and how they relate), not for backend architectural decisions. This API is deliberately the opposite: multi-user, Postgres-authoritative, real auth.

## Android Domain Model Snapshot (captured 2026-08-02)

Facts only — pulled from a full read of the three directories above. Room is currently single-user/single-tenant: **no `userId`/`ownerId`/`profileId` exists on any entity except `ProfileEntity` itself**, which is a hardcoded singleton row (`profileId = 1`, not autoincrement). No composite keys or UUIDs anywhere — every other PK is `Long` autoincrement. No many-to-many cross-ref tables — every parent-child link is a direct FK on the child.

**Enums** (all stored via `Converters.kt` as `.name` strings, except `MuscleGroup` — see below):
`Gender`(MALE, FEMALE, PREFER_NOT_TO_SAY) · `WeightUnit`(KG, LBS) · `MeasurementUnit`(METRIC, IMPERIAL) · `MuscleGroup`(CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS, CORE, GLUTES, QUADS, HAMSTRINGS, CALVES) · `Equipment`(BARBELL, DUMBBELL, CABLE, MACHINE, BODYWEIGHT, KETTLEBELL, BAND, BENCH, OTHER) · `ExerciseInputType`(REPS, TIMED) · `SetType`(NORMAL, WARMUP, DROP_SET, FAILURE) · `SessionStatus`(ACTIVE, PAUSED, COMPLETED) · `PrType`(MAX_WEIGHT, MAX_REPS_AT_WEIGHT, MAX_VOLUME, ESTIMATED_1RM, MAX_DURATION)

**Profile** (`ProfileEntity`, table `profile`): `profileId: Int = 1` (fixed singleton, not autoincrement), `name`, `gender`, `weightUnit`, `measurementUnit`, `profileImagePath: String?`, `activeSplitId: Long?` (FK → Split, `SET_NULL`), `createdAt: Long`. Closest thing to "ownership" in the current app, but it's a settings row, not an identity.

**Exercise** (`ExerciseEntity`, table `exercise`): `exerciseId: Long` (PK), `wgerId: Int?` (unique index — wger-seeded), `name`, `muscleGroups: List<MuscleGroup>` (comma-joined string via converter — **already superseded in this repo**, backend has a real `exercise_muscle_groups` collection table since Module 3), `equipment`, `inputType`, `description: String?`, `imageUrl: String?`, `isCustom: Boolean`. No owner FK — exercises are global/shared.

**Split** (`SplitEntity`, table `split`): only `splitId` (PK) + `name` persisted. Domain model's `workoutDayCount`/`totalExerciseCount`/`isActive` are all computed via joins (`SplitDao.getSplits()` → `SplitWithStats` projection), not columns.

**WorkoutDay** (`WorkoutDayEntity`, table `workout_day`): `workoutDayId` (PK), `splitId` (FK → Split, `CASCADE`), `name`, `sortOrder`. `exerciseCount` is computed via join, not stored.

**TemplateExercise** (`TemplateExerciseEntity`, table `template_exercise`): `templateExerciseId` (PK), `workoutDayId` (FK → WorkoutDay, `CASCADE`), `exerciseId` (FK → Exercise, `CASCADE`), `sortOrder`, `restDurationSeconds: Int = 90` (from `WorkoutDefaults.kt`), `isRepRange: Boolean = true`, `notes: String?`. Does NOT snapshot `exerciseName`/`inputType` — joins live to `exercise` (unlike session-side, see below).

**TemplateSet** (`TemplateSetEntity`, table `template_set`): `templateSetId` (PK), `templateExerciseId` (FK, `CASCADE`), `sortOrder`, `targetReps: Int?`, `targetRepsMax: Int?` (rep-range), `targetDurationSeconds: Int?` (TIMED exercises), `setType: SetType = NORMAL`.

**WorkoutSession** (`WorkoutSessionEntity`, table `workout_session`): `sessionId` (PK), `workoutDayId: Long?` (FK → WorkoutDay, `SET_NULL` — session survives day deletion), `splitNameSnapshot`/`workoutDayNameSnapshot: String?` (denormalized), `startedAt: Long`, `endedAt: Long?`, `durationSeconds: Long`, `totalVolumeKg: Float?`, `completedSets/totalSets: Int?`, `hasPrs: Boolean`, `notes: String?`, `status: SessionStatus`. `getActiveSession()` DAO query assumes only one ACTIVE/PAUSED session system-wide — that assumption breaks in multi-user and needs to become per-user.

**SessionExercise** (`SessionExerciseEntity`, table `session_exercise`): `sessionExerciseId` (PK), `sessionId` (FK → WorkoutSession, `CASCADE`), `exerciseId: Long?` (FK → Exercise, `SET_NULL` — nullable so session history survives exercise deletion), `exerciseNameSnapshot`, `inputTypeSnapshot` (both denormalized, unlike TemplateExercise), `isRepRange`, `restDurationSeconds`, `sortOrder`, `notes: String?`.

**SessionSet** (`SessionSetEntity`, table `session_set`): `sessionSetId` (PK), `sessionExerciseId` (FK, `CASCADE`), `sortOrder`, `setType`, `targetReps/targetRepsMax/targetDurationSeconds: Int?` (planned, copied from template), `reps/weightKg/durationSeconds` (actual logged values, all nullable), `isCompleted: Boolean`. `SessionSetDao.getPreviousPerformance(exerciseId, workoutDayId)` finds the last completed session for the same day+exercise to pre-fill "last time" values — currently a single global-history query, will need user scoping.

**PR (personal record)** — two entities exist but are **currently unused**: registered in `IronTrailDatabase.kt` (schema exists) but no domain model and no DAO reads/writes them.
- `PrRecordEntity` (table `pr_record`, "current best"): `prRecordId` (PK), `exerciseId` (FK → Exercise, `CASCADE`), `sessionId: Long?` (FK → WorkoutSession, `SET_NULL`), `prType`, `weightContextKg: Float`, `value: Float`, `achievedAt: Long`. Composite unique index on `(exerciseId, prType, weightContextKg)`.
- `SessionPrEntity` (table `session_pr`, append-only log): `sessionPrId` (PK), `sessionId` (FK → WorkoutSession, `CASCADE`), `exerciseId: Long?` (FK, `SET_NULL`), `exerciseNameSnapshot`, `prType`, `weightContextKg`, `previousValue`, `newValue: Float`, `achievedAt: Long`.

**Denormalization pattern to preserve in the backend:** session-side entities snapshot names/types (`exerciseNameSnapshot`, `splitNameSnapshot`, etc.) and use nullable `SET_NULL` FKs back to mutable planning entities, so historical session data survives edits/deletes upstream. Template-side entities join live instead. Keep this distinction when designing the Postgres schema.

## Curriculum Roadmap

Locked in 2026-07-31. Modules 0-7 alone produce a fully working, tested, resume-demoable API — treat that as the fallback scope if the 3-week clock gets tight. Mark progress here as modules complete.

| # | Module | Status | Covers |
|---|---|---|---|
| 0 | Project Bootstrap | Done | Spring Initializr, Gradle Kotlin DSL structure, running the app, `application.yml` vs `AndroidManifest`, git init + first push |
| 1 | Spring Core & DI | Done | IoC container, `@Component`/`@Service`/`@Repository`/`@Configuration`, constructor injection vs Hilt, bean scopes, profiles (dev/prod) vs Android build variants |
| 2 | REST Layer | Done | `@RestController`, mapping annotations, path/query params, `ResponseEntity`, global exception handling (`@ControllerAdvice`) |
| 3 | Persistence Basics | Done | PostgreSQL via docker-compose, JPA `@Entity` vs Room `@Entity`, Spring Data JPA repos vs Room DAOs, schema migrations (Flyway) |
| 4 | IronTrail Domain Modeling | Done | Translate Android domain models (Split, WorkoutDay, Template, Session, Set, Exercise, Profile) into normalized, multi-user JPA entities with ownership |
| 5 | Auth & Multi-User | Done | Spring Security, JWT, password hashing, register/login, per-user data isolation |
| 6 | Service Layer & Business Logic | Not started | Service pattern, `@Transactional`, DTO↔entity mapping, validation |
| 7 | Testing | Not started | JUnit5 + Mockito, `@DataJpaTest`/`@WebMvcTest`, Testcontainers |
| 8 | Dockerization | Not started | Dockerfile, full docker-compose (app + Postgres), env-based config |
| 9 | CI/CD | Not started | GitHub Actions: build → test → lint → docker build |
| 10 | AWS Deployment | Not started | ECS/EC2/Elastic Beanstalk choice, RDS, secrets, live URL |

Deferred (only if time allows after Module 10): OpenAPI/Swagger docs, pagination/rate-limiting, offline-first sync design for the future Android client, Wger API import job (populate `exercises` from the external Wger catalog — moves this responsibility out of `IronTrailApp`'s Room population code and into this backend; revisit after Module 4 once the `Exercise` entity shape is settled, since building the mapping against a schema that's about to change would mean redoing it twice; do not remove the existing Wger-populate code from `IronTrailApp` until it is updated to sync against this API instead of Room directly); PR (personal record) tracking — `pr_records`/`session_prs` tables plus PR-detection service logic, cut from the active plan 2026-08-03 to reduce Module 6/7 risk given the 3-week timeline. Pure scope cut, not a regression — Android's `PrRecordEntity`/`SessionPrEntity` are already dead schema (registered, no domain model, no DAO). If revisited: design `pr_records.session_id` as `NOT NULL` + `ON DELETE CASCADE` rather than the nullable/`SET NULL` shape in the Android source — deleting a session should delete its PR outright (the only way to let a user correct a bad log entry, since there's no other edit/delete-PR path) and let the ongoing PR-detection logic naturally refill the slot on the next logged set, rather than needing a full-history recalculation job on every session delete. That design also removes the need for a direct `owner_id` exception, since the FK back to `workout_sessions` is never null.

**Current state (2026-07-31):** `GET /api/v1/health` is live — `HealthService`/`HealthController` in `com.irontrail.api.health`, package-by-feature layout (each feature owns its own controller/service/etc., cross-cutting stuff goes in `common`/`config`). Versioning is per-controller (`@RequestMapping("/v1/...")`), not global — `server.servlet.context-path` is just `/api`.

**Package layout update (2026-08-02):** Within each feature package, split further into layer sub-packages — `controller/`, `service/`, `repository/`, `dto/`, `model/` (entities + enums), `exception/`. Reason: flat feature packages meant every file started with the feature name (`ExerciseController`, `ExerciseService`, `ExerciseRepository`, ...), which made the project tree hard to scan at a glance. `exercise` was reorganized this way; **apply the same sub-package split to every feature going forward** (`health` can stay flat unless/until it grows past a couple of files).

**Module 3 update (2026-08-02):** Persistence is fully wired up. `docker-compose.yaml` runs Postgres 16 (`irontrail-postgres`, db/user/password `irontrail`/`irontrail`/`irontrail_dev`, port 5432). `ApiApplication.kt`'s `DataSourceAutoConfiguration`/`HibernateJpaAutoConfiguration` exclusion is removed. `application.properties` has the datasource URL/credentials and `spring.jpa.hibernate.ddl-auto=validate` (Flyway owns schema, Hibernate only validates the mapping against it — never `update` in this project). `Exercise` is a real `@Entity` (`exercises` table, `muscleGroups` as an `@ElementCollection` into `exercise_muscle_groups`, watch the `@Enumerated(EnumType.STRING)` + explicit `@Column` name on collection elements — Hibernate's default snake_case pluralization of the property name won't match a migration's column name by default). `ExerciseRepository` extends `JpaRepository<Exercise, Long>` with a custom `findByMuscleGroupsContaining`. `ExerciseService` now uses the repository instead of the old in-memory `ConcurrentHashMap`/`AtomicLong`, is `@Transactional`, and no longer seeds data in code — seeding is `V1__create_exercises_table.sql` (schema) + `V2__seed_exercises.sql` (4 fixture rows, `INSERT ... SELECT` deriving `exercise_id` by `name` rather than hardcoding IDs) under `src/main/resources/db/migration/`, applied automatically by Flyway on boot. Full CRUD verified against the real Postgres container via Postman/curl, including a direct `psql` check that writes persist. `build.gradle.kts` has `flyway-core` + `flyway-database-postgresql` + `spring-boot-flyway`.

**Module 4 update (2026-08-03, done):** Ownership design decided — direct `owner_id`/`user_id` FK on tables that are independently meaningful and only ever filtered by id (`users`, `exercises`, `user_profile`, and the still-to-come `splits`, `workout_sessions`); no owner column on tables that only exist through an owned parent, ownership enforced via joining through the parent instead (`workout_days`, `template_exercises`, `template_sets`, `session_exercises`, and the still-to-come `session_sets`). PR tracking (`pr_records`/`session_prs`) has been cut from the active plan — see Deferred section below. Module 4 scope is migration + entity + a bare `JpaRepository<X, Long>` per table only — no DTOs/services/controllers for the new entities, those are Module 6.

Module 4 is complete: `V3` through `V14` all migrated, entity-mapped, and repository-backed. In order: `V3` (`users`), `V4` (`exercises.owner_id`, drops `is_custom`), `V5` (`user_profile` — 1:1 with `users` via a shared `user_id` PK/FK, `ON DELETE CASCADE`, no separate surrogate id), `V6` (`splits` — direct `owner_id`, `NOT NULL`, `ON DELETE CASCADE`, indexed), `V7` (`workout_days` — transitive via `split_id`, no `owner_id`), `V8` (`template_exercises` — transitive), `V9` (`template_sets` — transitive), `V10` (alters `user_profile` to add `active_split_id`, nullable FK → `splits`, `SET NULL` — deliberately **not** indexed, since the query pattern is one profile pointing at one split, not "find profiles by split"), `V11` (`workout_sessions` — direct `owner_id`, root of the history tree, same reasoning as `splits`; `workout_day_id` nullable/`SET NULL`; carries a partial unique index `idx_one_active_session_per_owner ON workout_sessions (owner_id) WHERE status IN ('ACTIVE', 'PAUSED')` enforcing "one active session per user" at the DB level rather than trusting application code; `has_prs` intentionally omitted, PR tracking is cut), `V12` (`session_exercises` — transitive via `session_id`; `exercise_id` nullable/`SET NULL` with `exercise_name_snapshot`/`input_type_snapshot` columns — the snapshot pattern, so session history survives the source exercise being edited or deleted), `V13` (`session_sets` — transitive via `session_exercise_id`; splits planned values copied from the template at logging time — `target_reps`/`target_reps_max`/`target_duration_seconds` — from the actual logged values — `reps`/`weight_kg`/`duration_seconds`), `V14` (alters `users.created_at` and `user_profile.created_at` from `TIMESTAMP` to `TIMESTAMPTZ`, `USING created_at AT TIME ZONE 'UTC'` — fixes the earlier audit finding that naive timestamps are ambiguous once deployed off a single-timezone machine; corresponding entity fields changed `LocalDateTime` → `OffsetDateTime`. This is now the standing rule for every "instant" column in the schema going forward, not a one-off for `workout_sessions`).

New feature package: `com.irontrail.api.session` (`model`/`repository`) — `WorkoutSession.kt`, `SessionStatus.kt` (enum: `ACTIVE`, `PAUSED`, `COMPLETED`), `SessionExercise.kt`, `SessionSet.kt`. Separate from `split` deliberately — planning-side (`Split`/`WorkoutDay`/`TemplateExercise`/`TemplateSet`) and history-side (`WorkoutSession`/`SessionExercise`/`SessionSet`) are two cohesive features, not one. `WorkoutSession ↔ SessionExercise` and `SessionExercise ↔ SessionSet` are both real `@OneToMany`/`@ManyToOne` pairs, same pattern as `TemplateExercise`/`TemplateSet` (relation fields in the class body, `MutableList` not `List`, explicit `fetch = FetchType.LAZY` both sides, `lateinit var` on the owning `@ManyToOne` side rather than a nullable type, since the underlying FK is `NOT NULL` and a nullable Kotlin type would misrepresent that guarantee). `SessionExercise.inputTypeSnapshot` reuses `ExerciseInputType` from `exercise.model`, `SessionSet.setType` reuses `SetType` from `split.model` — snapshot columns still get real enum types, not raw strings, even though they're denormalized copies.

Entities from `V3`–`V10` unchanged from before except: `UserProfile.kt`/`User.kt` now use `OffsetDateTime` (was `LocalDateTime`) for `createdAt`, matching `V14`.

Repositories: `UserRepository`, `UserProfileRepository`, `SplitRepository`, `WorkoutDayRepository`, `TemplateExerciseRepository`, `TemplateSetRepository`, `WorkoutSessionRepository`, `SessionExerciseRepository`, `SessionSetRepository` (all bare, no custom methods yet — `UserRepository` will need `findByEmail` when Module 5 builds login, `SplitRepository`/`WorkoutSessionRepository` will need `findByOwnerId` when Module 6 builds the service layer).

**Verified 2026-08-03:** full app boot against the real `docker-compose` Postgres — Flyway applied `V5`–`V14` cleanly, Hibernate `ddl-auto=validate` passed against every entity (would fail loudly on any `@Column`/`@JoinColumn`/enum mismatch), `GET /api/v1/health` returned `200 UP`.

**Module 5 update (2026-08-08, done):** Auth is fully wired — Spring Security + JWT, stateless, no server-side sessions. `build.gradle.kts` adds `spring-boot-starter-security` and `io.jsonwebtoken:jjwt-api`/`jjwt-impl`/`jjwt-jackson` (0.13.0). `V15` adds `users.password_hash` (`NOT NULL`, backfilled `''` for the alter). New feature package `com.irontrail.api.auth` (`config`/`filter`/`service`/`dto`/`exception`, same sub-package split as `exercise`):
- `SecurityConfig` — `@EnableWebSecurity`, stateless session policy, CSRF disabled (no cookies in a bearer-token API, so nothing to forge), `/v1/auth/**` permitted, everything else `authenticated()`. Exposes `PasswordEncoder` (`BCryptPasswordEncoder`) and `AuthenticationManager` (via `AuthenticationConfiguration`) beans, registers `JwtAuthenticationFilter` via `addFilterBefore<UsernamePasswordAuthenticationFilter>` (Kotlin DSL's reified generic form — the `Class`-argument overload is deprecated in Spring Security 7.1, the version this project is on).
- `JwtService` — `jjwt`-based; `generateToken(userId)` signs a token with `sub`/`iat`/`exp` claims (key is 48 random bytes from `jwt.secret`, env-overridable via `JWT_SECRET`, dev default in `application.yml`; expiry from `jwt.expiration` bound as a `Duration`, e.g. `1d`, no manual ms math needed). `jjwt`'s `signWith(key)` auto-selects the algorithm by key strength — our 48-byte key resolves to HS384, not HS256. `extractUserId(token)` parses and verifies in one call, returns `null` on any failure (bad signature, expired, malformed) rather than a separate validate step, so callers never double-parse.
- `CustomUserDetailsService` — `UserDetailsService` implementation used **only** by the login path (via `AuthenticationManager`/`DaoAuthenticationProvider`), keyed on email. Wraps `User` into Spring Security's own `org.springframework.security.core.userdetails.User` (aliased `SecurityUser` on import to avoid colliding with our own `User`) rather than making the JPA entity implement `UserDetails` directly. Added `UserRepository.findByEmail`.
- `JwtAuthenticationFilter` (`OncePerRequestFilter`) — per-request check, no `UserDetailsService`/DB call: reads `Authorization: Bearer`, resolves the id via `JwtService.extractUserId`, sets a `UsernamePasswordAuthenticationToken(userId, null, [ROLE_USER])` into `SecurityContextHolder`. Deliberate design fork from the more common tutorial pattern (store email in the token, reload full `UserDetails` from the DB every request): token subject is the numeric `userId` instead, since that's what `owner_id` filtering (Module 6) keys off, and it means per-request auth costs zero DB round-trips. Tradeoff: a revoked permission wouldn't take effect until the token naturally expires — moot for now, no roles/permissions system exists yet.
- `AuthService`/`AuthController` — `POST /v1/auth/register` (checks email uniqueness → `EmailAlreadyInUseException` → `409`; hashes via `PasswordEncoder`; returns a token) and `POST /v1/auth/login` (delegates credential-checking to `AuthenticationManager.authenticate`, which is what actually exercises `CustomUserDetailsService` + `PasswordEncoder.matches`; re-fetches the user by email afterward since the returned `Authentication`'s principal doesn't carry `userId`). `GlobalExceptionHandler` gained handlers for `EmailAlreadyInUseException` (409) and `BadCredentialsException` (401, deliberately generic message — covers both "wrong password" and "no such user" identically so the endpoint can't be used to enumerate registered emails, matching `DaoAuthenticationProvider`'s own default behavior of translating `UsernameNotFoundException` into the same `BadCredentialsException`).

**Verified 2026-08-08:** full end-to-end test against the real Postgres container — register (201), duplicate-email register (409), invalid-payload register (400 with field errors), login success (200), login wrong-password and login nonexistent-email (both 401, identical message), protected endpoint with no token (403), with a valid token (200), with a tampered signature or forged payload (403 either way). One benign edge case found, not a vulnerability: appending trailing junk to an otherwise-valid token still verifies, because the 48-byte HS384 signature's Base64URL encoding has no leftover bits (48 is evenly divisible by 3) and the decoder silently drops characters that don't form a complete group — altering any real character of the signature or payload still correctly fails closed.

**Deployment TODO (2026-08-08):** `jwt.secret` in `application.yml` currently falls back to a placeholder dev value if `JWT_SECRET` isn't set. Must be set as a real env var in `docker-compose.yaml` (Module 8) and as an AWS secret (Module 10, e.g. Secrets Manager/SSM) — same pattern as `DB_PASSWORD`.

**Next up: Module 6 (Service Layer & Business Logic).**

Module 2 added a full CRUD REST layer at `/api/v1/exercises` (`com.irontrail.api.exercise`) — `Exercise`/`ExerciseRequest`/`ExerciseResponse`, `MuscleGroup`/`Equipment`/`ExerciseInputType` enums, `ExerciseService` (in-memory `ConcurrentHashMap` store, pre-seeded, `AtomicLong` id generation), `ExerciseController` (GET list+filter, GET by id, POST, PUT, DELETE), and `GlobalExceptionHandler` in `com.irontrail.api.common` (`@RestControllerAdvice` handling `ExerciseNotFoundException` → 404 and `MethodArgumentNotValidException` → 400). This is intentionally not throwaway — same field shape as Android's `Exercise.kt`, so Module 3 swaps the in-memory store for a JPA repository and Module 4 turns `Exercise` into the real entity, without changing the DTOs or controller contract.

## Environment

Windows 11, IntelliJ IDEA CE, JDK 24 (project toolchain also targets 24 — deliberately not upgrading to LTS 25 to save time; revisit before Docker/AWS), Docker Desktop running, GitHub repo already created (empty — no local project scaffolded yet).

## Documentation Rules

Do not create README, design-doc, or summary `.md` files unless explicitly asked. This file is the only standing doc.

## Teaching Approach — Follow for Every Session

This is a learning project. Sagar is a senior Android developer new to Spring Boot/backend. Act as a senior backend dev + teaching assistant, not just a code generator.

1. **Theory before code, always.** Before any code: explain why the concept exists (what problem it solves), how it works internally, how it maps to Android (table below), and when to use it vs. alternatives.
2. **Explain every new annotation**: what it does, why it exists, what breaks if omitted, common mistakes.
3. **No MCQ quizzes, and don't ask him to explain concepts back either** (both dropped 2026-07-31 — low-value for him). Instead, after covering a concept, present it as a short self-answered interview Q&A ("Interview Q: ... / A: ...") — models a good interview answer without putting him on the spot.
4. **One file at a time.** Explain the file's purpose → ask what should go in it → provide content → wait for confirmation it's created and reviewed → only then move on. Never hand over multiple files at once. When the file being handed over is a *modification* to an existing file (not a new file), mark the changed/new/removed lines with inline comments directly in the code block itself (e.g. `// CHANGED: ...`, `// NEW`, `// REMOVED: ...`) rather than a separate before/after bullet list — a separate list still made him cross-reference against the code, inline markers don't (2026-08-08 feedback, refined same day after a first attempt with a bullet list). State plainly that these markers are scaffolding for review only and he should not copy them into the actual file.
5. **No copy-paste learning.** Still fine to ask him to predict code before revealing it. Verify understanding through code review (point 6) and the interview Q&A recaps (point 3), not by quizzing him directly.
6. **Code review discipline.** When he shares code: review every line, explain issues (why, not just what), let him fix it rather than handing over the fix, praise good decisions.
7. **Real-world framing.** Where relevant: how this is done at real Canadian companies, what interviewers ask about it, common junior mistakes, best practice vs. shortcut.
8. **Context discipline (token efficiency):** read only the file/function needed, not whole files speculatively; don't re-discover architecture already covered in this file; keep responses concise and direct.
9. **Decide, don't defer.** For standard technical calls (config, package structure, naming, which approach is idiomatic) — make the senior-dev decision yourself and explain the reasoning, don't ask Sagar to choose. He's new to this stack and can't meaningfully weigh options he doesn't have context for yet (2026-07-31 feedback: "you should be thinking about this as a senior and not me as a noob"). Only ask when it's a genuine product/scope/timeline tradeoff he's positioned to judge — not a technical one.
10. **Never write source files directly (Write/Edit tools) during a lesson unless explicitly told to.** Even when he asks to "review and provide the updated file," give the corrected code as a chat code block, not a tool write — he creates/types the file himself. This applies to the teaching flow specifically, not to bug fixes/refactors/other non-lesson work in this repo (2026-08-02 correction — I edited `ExerciseService.kt` directly via Write when he asked me to review and provide it).
11. **Top-down build order within a chunk of work (added 2026-08-08):** for a multi-file feature/module, build **Controller → Service → Repository → exception/GlobalExceptionHandler wiring last**, not bottom-up. Earlier files will reference methods/classes that don't exist yet and won't compile until the Repository step lands — that's expected, not a mistake to fix early. Reason: Sagar wants to understand the flow while implementing (why the layer exists, what it needs from the layer below) rather than wiring already-built pieces together after the fact. Still one file at a time per point 4 above, just reordered. Applies to every module going forward, not just Module 6. Commit each logical chunk of work separately (see Git section) rather than bundling multiple chunks into one commit.

### Android → Spring Boot concept map

| Android | Spring Boot |
|---|---|
| Hilt/Koin DI | Spring IoC container |
| `@Inject` | Constructor injection |
| Repository pattern | `@Repository` |
| ViewModel | `@Service` |
| Activity/Fragment | `@RestController` |
| Room `@Entity` | JPA `@Entity` |
| Room `@Dao` | Spring Data Repository interface |
| Retrofit `@GET`/`@POST` | `@GetMapping`/`@PostMapping` |
| `AndroidManifest.xml` | `application.properties`/`.yml` |
| `build.gradle.kts` | Same — Gradle Kotlin DSL, largely unchanged |

## Git

Repo: `https://github.com/sagarjogadia28/iron-trail-api.git` (origin, branch `main`). Never run `git commit` unless explicitly asked. Commit in logical units (one meaningful chunk of work per commit), short imperative subject lines. **No `Co-Authored-By: Claude` trailer or any AI-attribution in commit messages** — Sagar's explicit preference (2026-07-31).
