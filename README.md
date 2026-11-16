# MyDayboard

One Android app built from independent Gradle modules (a dashboard shell plus one module per feature), with strict, automated quality rules.

| Tool | Version |
|---|---|
| Android Gradle Plugin | 9.5.1 (built-in Kotlin) |
| Kotlin | 2.4.20 |
| Gradle | 9.7.0 |
| SDK levels | compile 37 · target 37 · min 26 |
| Compose BOM | 2026.08.00 |
| JDK | 25 to run Gradle (see gradle/gradle-daemon-jvm.properties), bytecode target 17 |

## Layout

```
app/               The application: a thin shell that owns navigation and the dashboard
feature/
  counter/         Event counter (placeholder for the first real tracker)
core/
  domain/          Pure Kotlin business logic (no Android)
  designsystem/    Theme and shared components
build-logic/       Convention plugins: the single source of truth for all rules
config/            detekt.yml, lint.xml, Compose stability config (one copy for everything)
scripts/           new-module.sh, git hooks
.github/           CI, E2E, releases, CODEOWNERS, Dependabot
```

### Architecture rules (enforced)

```
app      →  feature, core
feature  →  core
core     →  core
```

Nothing depends on `:app`. No feature depends on another feature. `core` never depends on a feature. Code that two features need goes into `core`. The dashboard lives in `:app`, the only layer allowed to know every feature.

Every module runs `checkModuleDependencies` as part of `check` and CI. It fails the build with a message like:

```
:feature:counter -> :feature:alarms: feature/* may only depend on core/*.
```

The rules are defined in `build-logic/.../ModuleRules.kt`, with unit tests.

### Same rules everywhere

All configuration lives in `build-logic`, `config/` and `gradle/libs.versions.toml`. Module build files only declare what's unique to them. For example, a complete feature module build file is just this:

```kotlin
plugins { alias(libs.plugins.mydayboard.android.feature) }
android { namespace = "io.github.ddiling.mydayboard.feature.counter" }
```

| Convention plugin | Used by | Adds |
|---|---|---|
| `mydayboard.android.application` | `:app` | SDK levels, R8, strict Lint across all dependencies, versioning and signing from CI environment variables, `.debug` suffix |
| `mydayboard.android.feature` | `feature/*` | Library + Compose + screenshot tests + core dependencies |
| `mydayboard.android.library` | Android `core/*` | SDK levels, Java 17 |
| `mydayboard.jvm.library` | `core/domain` | Plain Kotlin/JVM |
| `mydayboard.android.compose` | Compose modules | Compose compiler, BOM, stability config |
| `mydayboard.android.screenshot` | Features, design system | Roborazzi: every `@Preview` becomes a screenshot test |
| `mydayboard.quality` | Every module | Detekt, ktlint (via Spotless), module rules, CI lifecycle tasks |
| `mydayboard.root` | Root project | Affected-module detection and the `:affected*` tasks |

All modules share **one version catalog**, so Kotlin, AGP, Compose and library upgrades land everywhere in a single PR. Do not add per-module version overrides.

## Adding a feature

```bash
./scripts/new-module.sh payments   # feature/payments, io.github.ddiling.mydayboard.feature.payments
```

The script creates the module from the conventions, registers it in `settings.gradle.kts` and prints the next steps (use it from `:app`, record its screenshot baselines).

## CI: only what changed

On pull requests, CI checks **only the modules affected by the change**:

1. `git diff origin/main...HEAD` lists the changed files.
2. Each file is mapped to its module.
3. Every module that depends on it, directly or indirectly, is added.

| You change | CI checks |
|---|---|
| `feature/dashboard/...` | `feature:counter`, `app` |
| `core/designsystem/...` | The design system, every feature and the app |
| `build-logic/`, `gradle/`, `config/`, root build files, `.editorconfig`, workflows | Everything |
| Only `*.md` | No module checks (formatting only) |

Pushes to `main` always check everything, which also fills the build cache.

```bash
./gradlew -q :printAffectedModules -PaffectedBase=origin/main   # what would CI run?
./gradlew :affectedStaticAnalysis -PaffectedBase=origin/main    # Spotless, Detekt, module rules, Lint
./gradlew :affectedTest -PaffectedBase=origin/main              # unit + screenshot tests
./gradlew :affectedAssemble -PaffectedBase=origin/main          # debug + release APKs
./gradlew :affectedTest                                         # no base = everything
```

Always use the leading `:`. Without it, Gradle also runs same-named tasks in subprojects.

Each module has three lifecycle tasks: `ciStaticAnalysis`, `ciTest` and `ciAssemble`. The convention plugins attach the right work to each one for that module type, so the workflows never need to know module types. `./gradlew ciTest` runs the tests in every module.

### Workflows

| Workflow | Trigger | What it does |
|---|---|---|
| `ci.yml` | Every PR and push to `main` | Static analysis, tests and assembly for affected modules, plus build-logic tests. On `main` it also publishes the merged coverage report as an artifact. Ends with one **`CI passed`** gate job. |
| `e2e.yml` | `main`, nightly, manual, or PRs labelled `e2e` | One Maestro emulator job running the flows in `app/maestro/`. On PRs only with the `e2e` label. |
| `screenshots-record.yml` | Manual | Re-records all screenshot baselines on Linux and commits them to the branch |
| `gemini-review.yml` | PR opened or ready for review (not Dependabot), or a `/gemini-review` comment | Posts one advisory review comment, written by the Gemini API. Dependabot PRs are reviewed only on demand. Needs the `GEMINI_API_KEY` secret. |
| `release.yml` | Tag `vX.Y.Z` | Signed AAB → Play **internal** track → GitHub Release |
| `dependabot.yml` | Weekly | Grouped dependency and Actions update PRs |

### Branch protection on `main`

- Require pull requests.
- Require **only** the `CI passed` status check. Jobs whose modules weren't affected still succeed, and the gate collects every result.
- Do not require Code Owner review while you are the only maintainer: you cannot approve your own pull requests.

## Releases

```bash
git tag v1.4.0 && git push origin v1.4.0
```

Create one **GitHub Environment** named `release` under Settings → Environments. Give it required reviewers and these:

- **Secrets:** `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, `PLAY_SERVICE_ACCOUNT_JSON`
- **Variable:** `PLAY_PACKAGE_NAME`

The signing key lives only in GitHub Secrets, never in the repo. Use Play App Signing.

`versionName` comes from the tag. `versionCode` is `1000 + run number`, which only ever increases.

## Code quality

| Layer | Tool | Config | Fix it with |
|---|---|---|---|
| Formatting | ktlint (`ktlint_official`) via Spotless | `.editorconfig` | `./gradlew spotlessApply` |
| Code smells, complexity | Detekt 2 | `config/detekt/detekt.yml` | Fix it, or `./gradlew detektBaseline` |
| Android issues | Android Lint, every warning treated as an error, run once for the app across all its dependencies | `config/lint/lint.xml` | Fix it, or `./gradlew updateLintBaseline` |
| Architecture | `checkModuleDependencies` | `ModuleRules.kt` | Move shared code to `core/` |
| Visual regressions | Roborazzi screenshot tests from `@Preview`s | `*/src/test/screenshots/` | `./gradlew recordRoborazziDebug` (or the workflow) |
| E2E | Maestro | `app/maestro/` | `maestro test app/maestro/` |
| Coverage | Kover per module, merged at the root. Minimum 80% on `core:domain` and on every feature's `*ViewModel` classes; UI is reported, never gated | `core/domain/build.gradle.kts` | `./gradlew :coverageReport` |
| Compiler | Kotlin warnings treated as errors in CI, plus `progressiveMode` | `gradle.properties` | — |

### Coverage

Minimums apply to logic only: `core:domain` and each feature's ViewModels (`koverVerifyViewModels`, from the feature convention).
Screenshot tests prove the UI renders, not that it behaves, so Composables are reported but never gated.

Always use `./gradlew :coverageReport` (report in `build/reports/kover/html`). It runs every module's `ciTest` first.
Roborazzi only renders the previews when a verify/record task is in the task graph, so calling `:koverHtmlReport` alone
runs the screenshot tests as no-ops and under-reports UI coverage.

## Getting started

```bash
./scripts/install-git-hooks.sh
./gradlew wrapper --gradle-version 9.7.0   # refresh the wrapper jar
./gradlew spotlessApply                    # normalise formatting once
./gradlew recordRoborazziDebug             # create initial screenshot baselines (or use the workflow)
./gradlew check                            # everything, locally
```


## Recommended next steps

- **Dependency injection:** Hilt with KSP, with a `core:di` or per-feature modules. Features then stop creating their own ViewModels.
- **Data layer:** `core:data` (repositories), `core:network` and `core:database`. Features depend on interfaces in `core:domain`.
- **Navigation:** Navigation Compose. Each feature exposes a `NavGraphBuilder` extension, and `:app` composes the graphs.
- **Build speed:** a remote build cache (Develocity or a self-hosted Gradle cache node). It matters more in a monorepo than anywhere else.
- **Security:** CodeQL, GitHub's dependency-review action, and pinning third-party actions to commit SHAs.
