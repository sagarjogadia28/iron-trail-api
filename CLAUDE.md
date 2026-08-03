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
| 4 | IronTrail Domain Modeling | In progress | Translate Android domain models (Split, WorkoutDay, Template, Session, Set, Exercise, Profile) into normalized, multi-user JPA entities with ownership |
| 5 | Auth & Multi-User | Not started | Spring Security, JWT, password hashing, register/login, per-user data isolation |
| 6 | Service Layer & Business Logic | Not started | Service pattern, `@Transactional`, DTO↔entity mapping, validation |
| 7 | Testing | Not started | JUnit5 + Mockito, `@DataJpaTest`/`@WebMvcTest`, Testcontainers |
| 8 | Dockerization | Not started | Dockerfile, full docker-compose (app + Postgres), env-based config |
| 9 | CI/CD | Not started | GitHub Actions: build → test → lint → docker build |
| 10 | AWS Deployment | Not started | ECS/EC2/Elastic Beanstalk choice, RDS, secrets, live URL |

Deferred (only if time allows after Module 10): OpenAPI/Swagger docs, pagination/rate-limiting, offline-first sync design for the future Android client, Wger API import job (populate `exercises` from the external Wger catalog — moves this responsibility out of `IronTrailApp`'s Room population code and into this backend; revisit after Module 4 once the `Exercise` entity shape is settled, since building the mapping against a schema that's about to change would mean redoing it twice; do not remove the existing Wger-populate code from `IronTrailApp` until it is updated to sync against this API instead of Room directly); PR (personal record) tracking — `pr_records`/`session_prs` tables plus PR-detection service logic, cut from the active plan 2026-08-03 to reduce Module 6/7 risk given the 3-week timeline. Pure scope cut, not a regression — Android's `PrRecordEntity`/`SessionPrEntity` are already dead schema (registered, no domain model, no DAO). If revisited: design `pr_records.session_id` as `NOT NULL` + `ON DELETE CASCADE` rather than the nullable/`SET NULL` shape in the Android source — deleting a session should delete its PR outright (the only way to let a user correct a bad log entry, since there's no other edit/delete-PR path) and let the ongoing PR-detection logic naturally refill the slot on the next logged set, rather than needing a full-history recalculation job on every session delete. That design also removes the need for a direct `owner_id` exception, since the FK back to `workout_sessions` is never null.

**Current state (2026-07-31):** `GET /api/v1/health` is live — `HealthService`/`HealthController` in `com.irontrail.api.health`, package-by-feature layout (each feature owns its own controller/service/etc., cross-cutting stuff goes in `common`/`config`). Versioning is per-controller (`@RequestMapping("/v1/...")`), not global — `server.servlet.context-path` is just `/api`.

**Package layout update (2026-08-02):** Within each feature package, split further into layer sub-packages — `controller/`, `service/`, `repository/`, `dto/`, `model/` (entities + enums), `exception/`. Reason: flat feature packages meant every file started with the feature name (`ExerciseController`, `ExerciseService`, `ExerciseRepository`, ...), which made the project tree hard to scan at a glance. `exercise` was reorganized this way; **apply the same sub-package split to every feature going forward** (`health` can stay flat unless/until it grows past a couple of files).

**Module 3 update (2026-08-02):** Persistence is fully wired up. `docker-compose.yaml` runs Postgres 16 (`irontrail-postgres`, db/user/password `irontrail`/`irontrail`/`irontrail_dev`, port 5432). `ApiApplication.kt`'s `DataSourceAutoConfiguration`/`HibernateJpaAutoConfiguration` exclusion is removed. `application.properties` has the datasource URL/credentials and `spring.jpa.hibernate.ddl-auto=validate` (Flyway owns schema, Hibernate only validates the mapping against it — never `update` in this project). `Exercise` is a real `@Entity` (`exercises` table, `muscleGroups` as an `@ElementCollection` into `exercise_muscle_groups`, watch the `@Enumerated(EnumType.STRING)` + explicit `@Column` name on collection elements — Hibernate's default snake_case pluralization of the property name won't match a migration's column name by default). `ExerciseRepository` extends `JpaRepository<Exercise, Long>` with a custom `findByMuscleGroupsContaining`. `ExerciseService` now uses the repository instead of the old in-memory `ConcurrentHashMap`/`AtomicLong`, is `@Transactional`, and no longer seeds data in code — seeding is `V1__create_exercises_table.sql` (schema) + `V2__seed_exercises.sql` (4 fixture rows, `INSERT ... SELECT` deriving `exercise_id` by `name` rather than hardcoding IDs) under `src/main/resources/db/migration/`, applied automatically by Flyway on boot. Full CRUD verified against the real Postgres container via Postman/curl, including a direct `psql` check that writes persist. `build.gradle.kts` has `flyway-core` + `flyway-database-postgresql` + `spring-boot-flyway`.

**Module 4 update (2026-08-03, in progress):** Ownership design decided — direct `owner_id`/`user_id` FK on tables that are independently meaningful and only ever filtered by id (`users`, `exercises`, `user_profile`, and the still-to-come `splits`, `workout_sessions`); no owner column on tables that only exist through an owned parent, ownership enforced via joining through the parent instead (`workout_days`, `template_exercises`, `template_sets`, `session_exercises`, and the still-to-come `session_sets`). PR tracking (`pr_records`/`session_prs`) has been cut from the active plan — see Deferred section below. Module 4 scope is migration + entity + a bare `JpaRepository<X, Long>` per table only — no DTOs/services/controllers for the new entities, those are Module 6.

Done so far: `V3` (`users`), `V4` (`exercises.owner_id`, drops `is_custom`), `V5` (`user_profile` — 1:1 with `users` via a shared `user_id` PK/FK, `ON DELETE CASCADE`, no separate surrogate id), `V6` (`splits` — direct `owner_id`, `NOT NULL`, `ON DELETE CASCADE`, indexed), `V7` (`workout_days` — transitive via `split_id`, no `owner_id`). Entities: `User.kt`, `Exercise.kt` (updated: `isCustom` → `ownerId: Long?`), `UserProfile.kt` (note: `@Id` with **no** `@GeneratedValue` and no default value on the constructor param — `user_id` isn't sequence-generated, it must be supplied by the caller from an existing `User`, and the missing default forces every call site to actually do that), `Split.kt` (plain FK `ownerId: Long`, no `@ManyToOne` — nothing navigates `Split` → `User` as an object graph yet, every query is "splits where owner_id = x", not "given a split, get its user"), `WorkoutDay.kt` (same plain-FK shape, `splitId: Long`, lives in `split/model` — a `WorkoutDay` only ever exists in the context of a `Split`, same reasoning as why it has no `owner_id`, it gets no separate feature package either). New enums in `user/model`: `Gender`, `WeightUnit`, `MeasurementUnit` (no `displayName` — that's an Android UI-only concern, dropped same as it was for `Equipment`/`MuscleGroup`). `ExerciseResponse`/`ExerciseService` updated to match `Exercise.kt` (`create()` currently hardcodes `ownerId = null` since there's no authenticated caller yet to attribute it to — Module 5 will change just that one line). Repositories: `UserRepository`, `UserProfileRepository`, `SplitRepository`, `WorkoutDayRepository` (all bare, no custom methods yet — `UserRepository` will need `findByEmail` when Module 5 builds login, `SplitRepository` will need `findByOwnerId` when Module 6 builds the service layer).

Remaining migration plan, in order: `V6` `splits` (owner_id) → `V7` `workout_days` (transitive via `split_id`) → `V8` `template_exercises` (transitive) → `V9` `template_sets` (transitive) → `V10` alter `user_profile` to add `active_split_id` FK (deferred until `splits` exists) → `V11` `workout_sessions` (user_id — omit `has_prs`, PR tracking is deferred, see below) → `V12` `session_exercises` (transitive) → `V13` `session_sets` (transitive). `template_exercises`/`template_sets` and the session-side entities are the first real use of `@OneToMany`/`@ManyToOne` (true parent-child aggregates, always co-fetched, children meaningless alone) — everything through `V5` deliberately used plain FK columns instead, since nothing navigated them as objects. `pr_records`/`session_prs` (previously `V14`/`V15`) are cut — see Deferred section.

**Next up: `V8` (`template_exercises`).**

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
4. **One file at a time.** Explain the file's purpose → ask what should go in it → provide content → wait for confirmation it's created and reviewed → only then move on. Never hand over multiple files at once.
5. **No copy-paste learning.** Still fine to ask him to predict code before revealing it. Verify understanding through code review (point 6) and the interview Q&A recaps (point 3), not by quizzing him directly.
6. **Code review discipline.** When he shares code: review every line, explain issues (why, not just what), let him fix it rather than handing over the fix, praise good decisions.
7. **Real-world framing.** Where relevant: how this is done at real Canadian companies, what interviewers ask about it, common junior mistakes, best practice vs. shortcut.
8. **Context discipline (token efficiency):** read only the file/function needed, not whole files speculatively; don't re-discover architecture already covered in this file; keep responses concise and direct.
9. **Decide, don't defer.** For standard technical calls (config, package structure, naming, which approach is idiomatic) — make the senior-dev decision yourself and explain the reasoning, don't ask Sagar to choose. He's new to this stack and can't meaningfully weigh options he doesn't have context for yet (2026-07-31 feedback: "you should be thinking about this as a senior and not me as a noob"). Only ask when it's a genuine product/scope/timeline tradeoff he's positioned to judge — not a technical one.
10. **Never write source files directly (Write/Edit tools) during a lesson unless explicitly told to.** Even when he asks to "review and provide the updated file," give the corrected code as a chat code block, not a tool write — he creates/types the file himself. This applies to the teaching flow specifically, not to bug fixes/refactors/other non-lesson work in this repo (2026-08-02 correction — I edited `ExerciseService.kt` directly via Write when he asked me to review and provide it).

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
