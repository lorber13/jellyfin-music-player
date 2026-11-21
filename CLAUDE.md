# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

An Android music player for Jellyfin with offline playback and Android Auto support. The project skeleton (TECH-01) is in place: the app builds and shows an empty shell with four tabs. CI (TECH-02, `.github/workflows/ci.yml`) runs the done-checks below on every push to `main` and on every PR. A local Jellyfin test server with a generated music library (TECH-03) lives in `dev/test-server/`. The app opens on the server address screen (ACC-01), which rejects servers older than Jellyfin 10.9 with an "unsupported version" error (ACC-07), then signs in with username and password (ACC-02) or Quick Connect (ACC-03) and shows the tabs. `SessionRepository` in `:core:data` stores the session (ACC-04) in a DataStore file encrypted with Tink and an Android Keystore key (`SessionStoreModule`), and restores it at startup without asking the server, so the app also opens offline; the splash screen stays up until it is read. Features are implemented in the order of `docs/BACKLOG.md`.

## Commands

```bash
./gradlew assembleDebug                 # build the debug APK
./gradlew spotlessApply                 # format Kotlin and Gradle files (ktlint)
./gradlew spotlessCheck lint            # formatting check + Android Lint (warnings are errors)
./gradlew testDebugUnitTest             # all JVM unit tests of the Android modules
./gradlew :lint-rules:test              # tests of the project's own Lint checks
./gradlew :app:testDebugUnitTest --tests '*.NavigatorTest'                        # one test class
./gradlew :app:testDebugUnitTest --tests '*.NavigatorTest.goBack_closesTheCurrentScreen'   # one test
./gradlew connectedDebugAndroidTest     # UI tests on a connected device (set ANDROID_SERIAL to pick one)
./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=io.github.lorber13.jellyfinmusic.AppShellNavigationTest

dev/test-server/up.sh                   # local Jellyfin server (Docker) at localhost:8096, user test/test; also runs adb reverse
JELLYFIN_VERSION=10.8.13 dev/test-server/up.sh   # another server version (10.9.11 = oldest supported, 10.8.x for ACC-07)
dev/test-server/down.sh                 # stop it; reset.sh deletes its data (--all also the generated music)
```

Run `spotlessCheck lint testDebugUnitTest :lint-rules:test assembleDebug` before calling a change done.

## Architecture

Single-activity Jetpack Compose app, application ID and base package `io.github.lorber13.jellyfinmusic`. minSdk 29, compileSdk and targetSdk 37, Java 17 bytecode.

- **Modules**: `:app` (application, `MainActivity`, shell, navigation), `:core:designsystem` (theme, shared composables), `:core:data` (data layer, Hilt bindings; the only module that uses the Jellyfin SDK), `:feature:albums`, `:feature:login` (sign-in flow) (one module per feature area; add more as backlog items need them). Features depend on `core` modules, never on each other or on `:app`. `:lint-rules` (plain JVM) holds the project's own Lint checks; the convention plugins add it to every Android module with `lintChecks`.
- **Build logic**: `build-logic/convention` holds convention plugins (`jellyfinmusic.android.application`, `.android.library`, `.android.compose`, `.hilt`). SDK levels, lint settings and common dependencies live there, not in module build files. All versions are in `gradle/libs.versions.toml`. AGP 9 has built-in Kotlin, so `org.jetbrains.kotlin.android` and kapt are not used; annotation processing is KSP.
- **Navigation**: Navigation 3. Routes are `@Serializable` `NavKey`s. `JellyfinMusicRoot` holds the top-level back stack: the sign-in screens, then `AppShellRoute` (signing in replaces the whole stack, so Back never returns to sign-in; without a session it goes back to the server screen). `TopLevelDestination` lists the tabs; `NavigationState` keeps one back stack per tab and `Navigator` mutates it (`app/.../navigation`). A feature module exposes its routes and an `EntryProviderScope<NavKey>.xxxEntries(...)` function that `JellyfinMusicApp` calls. The shell is a `NavigationSuiteScaffold` (bar on phones, rail on wider windows).
- **DI**: Hilt. `JellyfinMusicApplication` is the `@HiltAndroidApp`.
- **Jellyfin API**: the official Kotlin SDK (`org.jellyfin.sdk:jellyfin-core`), provided as a singleton in `core/data/.../di/JellyfinModule.kt` with `minimumServerVersion` 10.9 (the SDK itself targets 12+). Repositories expose the data layer's own models, not SDK types. The SDK logs through SLF4J, which `slf4j-android` sends to Logcat.
- **Network**: cleartext HTTP is allowed for every host (home servers often have no HTTPS). From API 37, LAN connections need the `ACCESS_LOCAL_NETWORK` runtime permission (`feature/login/.../LocalNetworkAccess.kt`); `localhost` through `adb reverse` works without it.
- **Resources**: strings and drawables in library modules are prefixed with the module name (`feature_albums_`, `designsystem_`). Icons are vector drawables; the `material-icons` artifacts are deprecated and not used.
- **Strings (UI-05)**: every user-facing text, including content descriptions, is a string resource read with `stringResource`; the `HardcodedComposeText` Lint check enforces it in Compose code (`@Preview` functions are exempt; other text users never see needs `@Suppress` with a comment). Also:
  - Add a `<!-- comment -->` for translators when the context isn't obvious (where it appears, length limits).
  - Wrap placeholders in `<xliff:g id="…" example="…">` and use positional formats (`%1$s`). Build whole sentences in one string; never concatenate pieces.
  - Counts use `<plurals>` and `pluralStringResource`. Text that must not be translated gets `translatable="false"`.
  - Debug builds include the en-XA and ar-XB pseudolocales. To check text expansion and right-to-left layout on a device: `adb shell cmd locale set-app-locales io.github.lorber13.jellyfinmusic --locales en-XA` (`ar-XB`; `--locales ""` resets).
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
