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

Read these before making design decisions — don't rely on memory.

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

**Caveat on the two spec docs:** they describe the Android app's *current* single-user, offline-only, no-auth architecture — that part is superseded here. Use them for **domain/feature understanding** (what a Split, WorkoutDay, Template, Session, Set, PR actually mean and how they relate), not for backend architectural decisions. This API is deliberately the opposite: multi-user, Postgres-authoritative, real auth.

## Curriculum Roadmap

Locked in 2026-07-31. Modules 0-7 alone produce a fully working, tested, resume-demoable API — treat that as the fallback scope if the 3-week clock gets tight. Mark progress here as modules complete.

| # | Module | Status | Covers |
|---|---|---|---|
| 0 | Project Bootstrap | Done | Spring Initializr, Gradle Kotlin DSL structure, running the app, `application.yml` vs `AndroidManifest`, git init + first push |
| 1 | Spring Core & DI | Done | IoC container, `@Component`/`@Service`/`@Repository`/`@Configuration`, constructor injection vs Hilt, bean scopes, profiles (dev/prod) vs Android build variants |
| 2 | REST Layer | Done | `@RestController`, mapping annotations, path/query params, `ResponseEntity`, global exception handling (`@ControllerAdvice`) |
| 3 | Persistence Basics | Done | PostgreSQL via docker-compose, JPA `@Entity` vs Room `@Entity`, Spring Data JPA repos vs Room DAOs, schema migrations (Flyway) |
| 4 | IronTrail Domain Modeling | Not started | Translate Android domain models (Split, WorkoutDay, Template, Session, Set, PR, Exercise, Profile) into normalized, multi-user JPA entities with ownership |
| 5 | Auth & Multi-User | Not started | Spring Security, JWT, password hashing, register/login, per-user data isolation |
| 6 | Service Layer & Business Logic | Not started | Service pattern, `@Transactional`, PR-detection logic, DTO↔entity mapping, validation |
| 7 | Testing | Not started | JUnit5 + Mockito, `@DataJpaTest`/`@WebMvcTest`, Testcontainers |
| 8 | Dockerization | Not started | Dockerfile, full docker-compose (app + Postgres), env-based config |
| 9 | CI/CD | Not started | GitHub Actions: build → test → lint → docker build |
| 10 | AWS Deployment | Not started | ECS/EC2/Elastic Beanstalk choice, RDS, secrets, live URL |

Deferred (only if time allows after Module 10): OpenAPI/Swagger docs, pagination/rate-limiting, offline-first sync design for the future Android client, Wger API import job (populate `exercises` from the external Wger catalog — moves this responsibility out of `IronTrailApp`'s Room population code and into this backend; revisit after Module 4 once the `Exercise` entity shape is settled, since building the mapping against a schema that's about to change would mean redoing it twice; do not remove the existing Wger-populate code from `IronTrailApp` until it is updated to sync against this API instead of Room directly).

**Current state (2026-07-31):** `GET /api/v1/health` is live — `HealthService`/`HealthController` in `com.irontrail.api.health`, package-by-feature layout (each feature owns its own controller/service/etc., cross-cutting stuff goes in `common`/`config`). Versioning is per-controller (`@RequestMapping("/v1/...")`), not global — `server.servlet.context-path` is just `/api`.

**Package layout update (2026-08-02):** Within each feature package, split further into layer sub-packages — `controller/`, `service/`, `repository/`, `dto/`, `model/` (entities + enums), `exception/`. Reason: flat feature packages meant every file started with the feature name (`ExerciseController`, `ExerciseService`, `ExerciseRepository`, ...), which made the project tree hard to scan at a glance. `exercise` was reorganized this way; **apply the same sub-package split to every feature going forward** (`health` can stay flat unless/until it grows past a couple of files).

**Module 3 update (2026-08-02):** Persistence is fully wired up. `docker-compose.yaml` runs Postgres 16 (`irontrail-postgres`, db/user/password `irontrail`/`irontrail`/`irontrail_dev`, port 5432). `ApiApplication.kt`'s `DataSourceAutoConfiguration`/`HibernateJpaAutoConfiguration` exclusion is removed. `application.properties` has the datasource URL/credentials and `spring.jpa.hibernate.ddl-auto=validate` (Flyway owns schema, Hibernate only validates the mapping against it — never `update` in this project). `Exercise` is a real `@Entity` (`exercises` table, `muscleGroups` as an `@ElementCollection` into `exercise_muscle_groups`, watch the `@Enumerated(EnumType.STRING)` + explicit `@Column` name on collection elements — Hibernate's default snake_case pluralization of the property name won't match a migration's column name by default). `ExerciseRepository` extends `JpaRepository<Exercise, Long>` with a custom `findByMuscleGroupsContaining`. `ExerciseService` now uses the repository instead of the old in-memory `ConcurrentHashMap`/`AtomicLong`, is `@Transactional`, and no longer seeds data in code — seeding is `V1__create_exercises_table.sql` (schema) + `V2__seed_exercises.sql` (4 fixture rows, `INSERT ... SELECT` deriving `exercise_id` by `name` rather than hardcoding IDs) under `src/main/resources/db/migration/`, applied automatically by Flyway on boot. Full CRUD verified against the real Postgres container via Postman/curl, including a direct `psql` check that writes persist. `build.gradle.kts` has `flyway-core` + `flyway-database-postgresql` + `spring-boot-flyway`.

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
