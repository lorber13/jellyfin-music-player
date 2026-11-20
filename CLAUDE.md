# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

An Android music player for Jellyfin with offline playback and Android Auto support. The project skeleton (TECH-01) is in place: the app builds and shows an empty shell with four tabs. CI (TECH-02, `.github/workflows/ci.yml`) runs the done-checks below on every push to `main` and on every PR. Features are implemented in the order of `docs/BACKLOG.md`.

## Commands

```bash
./gradlew assembleDebug                 # build the debug APK
./gradlew spotlessApply                 # format Kotlin and Gradle files (ktlint)
./gradlew spotlessCheck lint            # formatting check + Android Lint (warnings are errors)
./gradlew testDebugUnitTest             # all JVM unit tests
./gradlew :app:testDebugUnitTest --tests '*.NavigatorTest'                        # one test class
./gradlew :app:testDebugUnitTest --tests '*.NavigatorTest.goBack_closesTheCurrentScreen'   # one test
./gradlew connectedDebugAndroidTest     # UI tests on a connected device (set ANDROID_SERIAL to pick one)
./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=io.github.lorber13.jellyfinmusic.AppShellNavigationTest
```

Run `spotlessCheck lint testDebugUnitTest assembleDebug` before calling a change done.

## Architecture

Single-activity Jetpack Compose app, application ID and base package `io.github.lorber13.jellyfinmusic`. minSdk 29, compileSdk and targetSdk 37, Java 17 bytecode.

- **Modules**: `:app` (application, `MainActivity`, shell, navigation), `:core:designsystem` (theme, shared composables), `:core:data` (data layer, Hilt bindings), `:feature:albums` (one module per feature area; add more as backlog items need them). Features depend on `core` modules, never on each other or on `:app`.
- **Build logic**: `build-logic/convention` holds convention plugins (`jellyfinmusic.android.application`, `.android.library`, `.android.compose`, `.hilt`). SDK levels, lint settings and common dependencies live there, not in module build files. All versions are in `gradle/libs.versions.toml`. AGP 9 has built-in Kotlin, so `org.jetbrains.kotlin.android` and kapt are not used; annotation processing is KSP.
- **Navigation**: Navigation 3. Routes are `@Serializable` `NavKey`s. `TopLevelDestination` lists the tabs; `NavigationState` keeps one back stack per tab and `Navigator` mutates it (`app/.../navigation`). A feature module exposes its routes and an `EntryProviderScope<NavKey>.xxxEntries(...)` function that `JellyfinMusicApp` calls. The shell is a `NavigationSuiteScaffold` (bar on phones, rail on wider windows).
- **DI**: Hilt. `JellyfinMusicApplication` is the `@HiltAndroidApp`.
- **Resources**: strings and drawables in library modules are prefixed with the module name (`feature_albums_`, `designsystem_`). Icons are vector drawables; the `material-icons` artifacts are deprecated and not used.
- **Backups are disabled** (`data_extraction_rules.xml`, `full_backup_content.xml`) because app data will hold server credentials.

## Working agreement

**Ask the user before making decisions.** Whenever a choice isn't already settled by the user or by the docs (scope, priorities, sizes, naming, file locations, dependencies, architecture, conventions), ask first and offer options with a recommendation. Don't pick one and report it afterwards.

**Fetch the latest Android Developers docs before implementing.** Android APIs, Jetpack libraries and recommended patterns change often. Before writing or changing Android code, Gradle config or dependency versions, read the relevant pages on developer.android.com (including AndroidX release notes and the Media3 and Android Auto guides) and follow what they currently say. Don't rely on memory. Say which pages you checked.

**Deploy to the Pixel phone after implementing a feature.** Once a feature is implemented and the build passes, install the debug build on the user's Pixel phone over adb and launch it, so the user can check it on a real device. Pick the phone explicitly by its serial from `adb devices -l` when an emulator is also running. If the phone isn't connected or isn't authorized, say so and ask; don't silently deploy to an emulator instead. Report what was installed.

## Documents

- `docs/FEATURES.md` describes **what** the app does. It is grouped by area, and every feature has a stable ID (`ACC-`, `LIB-`, `PL-`, `PLY-`, `OFF-`, `AUTO-`, `SET-`, `UI-`). It also holds the out-of-scope list and the product constraints. It deliberately has no architecture and no release assignments.
- `docs/BACKLOG.md` says **when**. It orders every feature ID into milestones M1–M6, each ending in a working app. Each item has only an ID, a title and a size (S ≤ 1 day, M 2–3 days, L ~1 week, XL = split first). `TECH-xx` items are technical enablers that exist only in the backlog.

Keep them consistent:
- New features get a new ID in FEATURES.md (never renumber existing IDs) **and** a row in exactly one backlog milestone.
- The milestone summary table at the top of BACKLOG.md shows item counts. Update them when rows move.
- Check that every spec ID appears exactly once in the backlog:
  ```bash
  comm -3 <(grep -oE '\*\*[A-Z]+-[0-9]+' docs/FEATURES.md | tr -d '*' | sort) \
          <(grep -oE '^\| [A-Z]+-[0-9]+' docs/BACKLOG.md | tr -d '| ' | grep -v TECH | sort)
  ```
  The command should print nothing.
- Issues and PRs should reference backlog item IDs (e.g. `PLY-04`).

## Commits

Use [Conventional Commits](https://www.conventionalcommits.org/): `<type>(<optional scope>): <summary>`
- Types: `feat`, `fix`, `docs`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `style`, `revert`.
- Summary in the imperative mood, lowercase, no trailing period.
- Put backlog item IDs in a `Refs:` footer (e.g. `Refs: PLY-04, PLY-05`).
- Breaking changes use `!` after the type/scope and a `BREAKING CHANGE:` footer.

Example: `feat(player): add queue reordering` with the footer `Refs: PLY-04`.

## Product constraints that drive technical choices

These come from FEATURES.md §10 and §9. Check new dependencies and designs against them:
- **FOSS-only dependencies**: the app ships on Google Play **and F-Droid** under **GPL-3.0**. Do not use Google Play Services, Firebase, the Cast SDK or other proprietary libraries.
- **No telemetry**: no analytics or crash reporting. Diagnostics go through the user-triggered log export (SET-07).
- **Android 10+** (minSdk 29) on phones, tablets and foldables. Android Auto is in scope. Android Automotive OS, TV and Wear OS are not.
- **Jellyfin server 10.9+.** Older servers must get the "unsupported version" message (ACC-07), not a workaround.
- **All user-facing text in string resources** from the start (UI-05), so the app can be translated.
- **Offline is a core requirement**, not an add-on. Screens must work from locally stored data (LIB-11, OFF-09), and Android Auto must work offline (AUTO-08).
