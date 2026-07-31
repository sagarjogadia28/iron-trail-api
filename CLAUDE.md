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
| 2 | REST Layer | Not started | `@RestController`, mapping annotations, path/query params, `ResponseEntity`, global exception handling (`@ControllerAdvice`) |
| 3 | Persistence Basics | Not started | PostgreSQL via docker-compose, JPA `@Entity` vs Room `@Entity`, Spring Data JPA repos vs Room DAOs, schema migrations (Flyway) |
| 4 | IronTrail Domain Modeling | Not started | Translate Android domain models (Split, WorkoutDay, Template, Session, Set, PR, Exercise, Profile) into normalized, multi-user JPA entities with ownership |
| 5 | Auth & Multi-User | Not started | Spring Security, JWT, password hashing, register/login, per-user data isolation |
| 6 | Service Layer & Business Logic | Not started | Service pattern, `@Transactional`, PR-detection logic, DTO↔entity mapping, validation |
| 7 | Testing | Not started | JUnit5 + Mockito, `@DataJpaTest`/`@WebMvcTest`, Testcontainers |
| 8 | Dockerization | Not started | Dockerfile, full docker-compose (app + Postgres), env-based config |
| 9 | CI/CD | Not started | GitHub Actions: build → test → lint → docker build |
| 10 | AWS Deployment | Not started | ECS/EC2/Elastic Beanstalk choice, RDS, secrets, live URL |

Deferred (only if time allows after Module 10): OpenAPI/Swagger docs, pagination/rate-limiting, offline-first sync design for the future Android client.

**Current state (2026-07-31):** `GET /api/v1/health` is live — `HealthService`/`HealthController` in `com.irontrail.api.health`, package-by-feature layout (each feature owns its own controller/service/etc., cross-cutting stuff goes in `common`/`config`). Versioning is per-controller (`@RequestMapping("/v1/...")`), not global — `server.servlet.context-path` is just `/api`. `ApiApplication.kt` currently excludes `DataSourceAutoConfiguration`/`HibernateJpaAutoConfiguration` (also flagged with a `TODO` in the code) because no DB is wired up yet — **remove that exclusion as part of Module 3**, it's a temporary placeholder, not a real decision to skip persistence config.

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
