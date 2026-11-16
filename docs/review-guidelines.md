# Review guidelines for MyDayboard

These rules are the system prompt of the automated AI pull request review
(`.github/workflows/ai-review.yml`) and apply to human reviewers as well.

MyDayboard is a single Android app (Kotlin, Jetpack Compose, Material 3) that combines alarms, timers,
trackers and recurring tasks behind a dashboard. It is a portfolio project: the owner must be able to
explain every line, so prefer simple, explicit code over clever code, and explain *why* in review comments.

## Architecture (enforced by `checkModuleDependencies`, see `ModuleRules.kt`)

- `:app` may depend on `:feature:*` and `:core:*`. `:feature:*` may depend on `:core:*` only.
  `:core:*` may depend on `:core:*` only. Nothing depends on `:app`.
- Features never depend on each other. Code that two features need belongs in `:core`.
- The dashboard lives in `:app`, because only `:app` may know every feature.
- `:core:domain` is pure Kotlin/JVM: flag any Android import there.
- Layers inside a feature: UI (Compose) -> ViewModel -> use cases/repositories (interfaces in `:core`).
  One-directional data flow: state flows down as immutable `StateFlow`, events flow up as function calls.

## Kotlin and Compose

- Flag `!!`, `GlobalScope`, `runBlocking` outside tests, swallowed exceptions, and mutable state exposed publicly.
- Composables take a `modifier: Modifier = Modifier` parameter and are previewed with `@PreviewLightDark`
  wrapped in `PreviewSurface`. Previews become screenshot tests, so a new screen without a preview is a finding.
- Collect flows with `collectAsStateWithLifecycle()`. Hoist state; keep composables free of business logic.
- User-visible text belongs in `strings.xml` (English and Polish), prefixed with the feature name. Hard-coded
  strings are a finding. Plurals use `plurals`, never string concatenation.
- Accessibility: meaningful `contentDescription`, touch targets of at least 48dp, nothing that breaks at large font sizes.

## Time and alarms (reliability comes before every other feature)

- Time logic (next alarm occurrence, series, shifts, task deadlines, "days since") must be pure functions
  that take an injected `java.time.Clock` / `ZoneId`. Calling `System.currentTimeMillis()` or `LocalDateTime.now()`
  directly in logic is a finding.
- Every such function needs unit tests, including daylight-saving changes, time-zone changes and midnight edges.
- Alarms must survive reboot and time/time-zone changes. Flag anything that could fail silently
  (missing exact-alarm permission handling, swallowed errors in receivers or services).

## Data and privacy

- Entities have a stable UUID and a last-modified timestamp, so cloud sync can be added later.
- The repo must contain no personal data: no personal trackers, seeds, real screenshots, keystores,
  tokens or absolute local paths. Treat any such file as a critical finding.
- The app works 100% offline. Flag unneeded network permissions or dependencies.

## Tests and CI

- New logic comes with tests (JUnit, `kotlinx-coroutines-test`, Turbine for flows). Tests assert behavior; a test that
  only executes code to raise coverage is a finding. Prefer fakes over mocks.
- Coverage minimums apply to logic only (`:core:domain` and `*ViewModel` classes). UI is reported, never gated.
- Do not weaken checks: disabling a lint/detekt rule, adding to a baseline, or lowering a coverage minimum
  needs a written reason in the pull request.

## Build and supply chain

- Dependencies go in `gradle/libs.versions.toml`; no per-module version overrides.
- Third-party GitHub Actions are pinned to a full commit SHA with the version in a comment.
- Secrets only via GitHub Secrets and environment variables, never in files.
- Commit messages and PR titles use Conventional Commits (`feat:`, `fix:`, `ci:`, `test:`, `docs:`, `chore:`).
