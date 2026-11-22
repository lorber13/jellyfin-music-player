# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

An Android music player for Jellyfin with offline playback and Android Auto support. The project skeleton (TECH-01) is in place: the app builds and shows an empty shell with four tabs. CI (TECH-02, `.github/workflows/ci.yml`) runs the done-checks below on every push to `main` and on every PR. A local Jellyfin test server with a generated music library (TECH-03) lives in `dev/test-server/`. The app opens on the server address screen (ACC-01), which rejects servers older than Jellyfin 10.9 with an "unsupported version" error (ACC-07), then signs in with username and password (ACC-02) or Quick Connect (ACC-03) and shows the tabs. `SessionRepository` in `:core:data` stores the session (ACC-04) in a DataStore file encrypted with Tink and an Android Keystore key (`SessionStoreModule`), and restores it at startup without asking the server, so the app also opens offline; the splash screen stays up until it is read. The Library tab shows the user's albums as a grid with covers (LIB-03), sortable by name, year or date added; the sort is remembered in a Preferences DataStore file (`PreferencesStoreModule`). Tapping an album opens its detail screen (LIB-07): cover, year, song count, total duration and the track list, with disc headings on multi-disc albums. Its Play button, or tapping a track, plays the album (PLY-01) in a Media3 `MediaSessionService`, which keeps playing in the background with a media notification and lock-screen controls (PLY-08); tapping either opens the app. Each track plays as its original file when the phone can decode its format, otherwise as the server's conversion (PLY-13). Features are implemented in the order of `docs/BACKLOG.md`.

## Commands

```bash
./gradlew assembleDebug                 # build the debug APK
./gradlew assembleRelease               # build the release APK (R8, signed with the debug key until TECH-05)
./gradlew spotlessApply                 # format Kotlin and Gradle files (ktlint)
./gradlew spotlessCheck lint            # formatting check + Android Lint (warnings are errors)
./gradlew testDebugUnitTest             # all JVM unit tests of the Android modules
./gradlew :lint-rules:test              # tests of the project's own Lint checks
./gradlew :app:testDebugUnitTest --tests '*.NavigatorTest'                        # one test class
./gradlew :app:testDebugUnitTest --tests '*.NavigatorTest.goBack_closesTheCurrentScreen'   # one test
./gradlew connectedDebugAndroidTest     # UI tests, on the emulator (set ANDROID_SERIAL to its serial, e.g. emulator-5554)
./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=io.github.lorber13.jellyfinmusic.AppShellNavigationTest

dev/test-server/up.sh                   # local Jellyfin server (Docker) at localhost:8096, user test/test; also runs adb reverse
JELLYFIN_VERSION=10.8.13 dev/test-server/up.sh   # another server version (10.9.11 = oldest supported, 10.8.x for ACC-07)
dev/test-server/down.sh                 # stop it; reset.sh deletes its data (--all also the generated music)
```

Run `spotlessCheck lint testDebugUnitTest :lint-rules:test assembleDebug` before calling a change done.

## Architecture

Single-activity Jetpack Compose app, application ID and base package `io.github.lorber13.jellyfinmusic`. Debug builds add the `.debug` suffix and are labeled "JMP Debug", so they install next to the release build, which the user runs every day. minSdk 29, compileSdk and targetSdk 37, Java 17 bytecode.

- **Modules**: `:app` (application, `MainActivity`, shell, navigation), `:core:designsystem` (theme, shared composables), `:core:data` (data layer, Hilt bindings; the only module that uses the Jellyfin SDK), `:core:playback` (playback service; the only module that uses Media3), `:feature:albums`, `:feature:login` (sign-in flow) (one module per feature area; add more as backlog items need them). Features depend on `core` modules, never on each other or on `:app`. `:lint-rules` (plain JVM) holds the project's own Lint checks; the convention plugins add it to every Android module with `lintChecks`.
- **Build logic**: `build-logic/convention` holds convention plugins (`jellyfinmusic.android.application`, `.android.library`, `.android.compose`, `.hilt`). SDK levels, lint settings and common dependencies live there, not in module build files. All versions are in `gradle/libs.versions.toml`. AGP 9 has built-in Kotlin, so `org.jetbrains.kotlin.android` and kapt are not used; annotation processing is KSP.
- **Navigation**: Navigation 3. Routes are `@Serializable` `NavKey`s. `JellyfinMusicRoot` holds the top-level back stack: the sign-in screens, then `AppShellRoute` (signing in replaces the whole stack, so Back never returns to sign-in; without a session it goes back to the server screen). `TopLevelDestination` lists the tabs; `NavigationState` keeps one back stack per tab and `Navigator` mutates it (`app/.../navigation`). ViewModels are scoped to their `NavEntry` (`rememberViewModelStoreNavEntryDecorator`); a screen that needs its route's arguments gets them through Hilt assisted injection (`hiltViewModel<VM, VM.Factory>(creationCallback = …)`, see `AlbumDetailViewModel`). A feature module exposes its routes and an `EntryProviderScope<NavKey>.xxxEntries(...)` function that `JellyfinMusicApp` calls. The shell is a `NavigationSuiteScaffold` (bar on phones, rail on wider windows).
- **DI**: Hilt. `JellyfinMusicApplication` is the `@HiltAndroidApp`. Repository bindings are `internal` (`DataModule`), except `LibraryModule`, which is public so instrumented tests can replace it; its implementations are public classes with internal constructors for the same reason.
- **Instrumented tests in `:app`** run with `HiltTestRunner` (`HiltTestApplication`). Screens with Hilt ViewModels are hosted in `HiltTestActivity`, which lives in `app/src/debug` because test-APK activities don't run in the app's process. `FakeLibraryModule` (`@TestInstallIn`) replaces the library data with fixed albums. Feature modules test their stateless `…Content` composables in a plain `ComponentActivity`.
- **Jellyfin API**: the official Kotlin SDK (`org.jellyfin.sdk:jellyfin-core`), with `minimumServerVersion` 10.9 (the SDK itself targets 12+). Repositories get clients from `JellyfinClients` (`core/data/.../client`), never by creating the SDK themselves: it creates the SDK once, on first use, with the device ID from `DeviceIdStore`, a random UUID kept in the preferences file until the app is uninstalled or its data is cleared. Android's identifier guide recommends this over `ANDROID_ID`, which the SDK would otherwise use and which survives reinstalls. Each installation, so also the debug and release builds, is its own device on the server. Repositories expose the data layer's own models, not SDK types. The SDK logs through SLF4J, which `slf4j-android` sends to Logcat. Some SDK models require fields that 10.9 servers don't send (`UserItemDataDto.ItemId`, `MediaStream.IsOriginal`), so their answers fail to parse there: list requests leave out user data (`enableUserData = false`), and PlaybackInfo is read with the app's own `PlaybackInfo` model through the SDK's `post<T>`. Test new requests against 10.9.11 too.
- **Network**: cleartext HTTP is allowed for every host (home servers often have no HTTPS). From API 37, LAN connections need the `ACCESS_LOCAL_NETWORK` runtime permission (`feature/login/.../LocalNetworkAccess.kt`); `localhost` through `adb reverse` works without it.
- **Playback**: Media3 (ExoPlayer and session). Screens call `MusicPlayer.play(queue, startIndex)` with `QueueItem`s that carry only a track id and what the notification shows. `MediaControllerMusicPlayer` connects a `MediaController` on the first play and releases it in `MainActivity`'s `onStop`. `PlaybackService`'s `TrackStreamCallback` turns ids into streams from `StreamRepository` (`:core:data`) asynchronously (`scope.future`, kotlinx-coroutines-guava). For each track it sends a PlaybackInfo request whose device profile lists the formats the phone plays: `PlayableFormats.kt` pairs the containers Media3 reads with the codecs `MediaCodecList` has decoders for. The server answers with the original file (`static=true`) or an HLS conversion, AAC 256 kbps in fMP4 segments (`media3-exoplayer-hls`; HLS because Jellyfin's progressive conversions can't be seeked). The access token never goes in a URL: `conversionUrlOf` strips the `ApiKey`/`api_key` the server puts in conversion URLs (and adds the `AudioCodec` that 10.9 leaves out), and a `ResolvingDataSource` adds the `Authorization` header to each request, HLS playlists and segments included. ExoPlayer handles audio focus. Platform diagnostics are off (no telemetry). The session activity is the app's launch intent, so `:core:playback` doesn't depend on `:app`, and the notification shows `playback_ic_notification` through `DefaultMediaNotificationProvider`. Media3's defaults are relied on for the rest: a wake lock while playing, music keeps playing when the app is swiped away (the service stops if paused), the service leaves the foreground after 10 minutes paused, and media notifications need no `POST_NOTIFICATIONS`. Unstable Media3 APIs are opted in per function with `androidx.annotation.OptIn(UnstableApi::class)` and a comment.
- **Images**: Coil 3. `AlbumCover` in `:core:designsystem` loads covers with `coil-compose`; `:app` adds the OkHttp network fetcher (`coil-network-okhttp`). The default singleton `ImageLoader` is used. The data layer builds cover URLs with the image tag, so changed artwork gets a new URL.
- **Resources**: strings and drawables in library modules are prefixed with the module name (`feature_albums_`, `designsystem_`). Icons are vector drawables; the `material-icons` artifacts are deprecated and not used.
- **Strings (UI-05)**: every user-facing text, including content descriptions, is a string resource read with `stringResource`; the `HardcodedComposeText` Lint check enforces it in Compose code (`@Preview` functions are exempt; other text users never see needs `@Suppress` with a comment). Also:
  - Add a `<!-- comment -->` for translators when the context isn't obvious (where it appears, length limits).
  - Wrap placeholders in `<xliff:g id="…" example="…">` and use positional formats (`%1$s`). Build whole sentences in one string; never concatenate pieces.
  - Counts use `<plurals>` and `pluralStringResource`. Text that must not be translated gets `translatable="false"`.
  - Debug builds include the en-XA and ar-XB pseudolocales. To check text expansion and right-to-left layout on a device: `adb shell cmd locale set-app-locales io.github.lorber13.jellyfinmusic.debug --locales en-XA` (`ar-XB`; `--locales ""` resets).
- **Release build**: R8 code and resource optimization through the AGP 9 `optimization { enable = true }` DSL, which includes the default keep rules. Extra keep rules go in `app/src/main/keepRules/*.keep` (none needed so far). Signed with the local debug key until TECH-05 adds the release key.
- **Backups are disabled** (`data_extraction_rules.xml`, `full_backup_content.xml`) because app data will hold server credentials.

## Working agreement

**Ask the user before making decisions.** Whenever a choice isn't already settled by the user or by the docs (scope, priorities, sizes, naming, file locations, dependencies, architecture, conventions), ask first and offer options with a recommendation. Don't pick one and report it afterwards.

**Fetch the latest Android Developers docs before implementing.** Android APIs, Jetpack libraries and recommended patterns change often. Before writing or changing Android code, Gradle config or dependency versions, read the relevant pages on developer.android.com (including AndroidX release notes and the Media3 and Android Auto guides) and follow what they currently say. Don't rely on memory. Say which pages you checked.

**Test on the emulator; put only the release build on the Pixel phone.** Run instrumented tests and try features with the debug build (`io.github.lorber13.jellyfinmusic.debug`) on an emulator, never on the phone: `Pixel_7_API_37` (Android 17, the targetSdk) by default, `Pixel_6_API_29` (Android 10, the minSdk) for oldest-version checks. Create Android 17 emulators with command-line tools 22 or newer: older `avdmanager` versions write `target=android-0` for `android-37.0` images, and those emulators crash whenever the screen is read (`hasReadColorBufferDma` assertion), so screenshots and UI dumps fail. Start one if none is running (`emulator -avd Pixel_7_API_37`), and point it at the local test server (`adb reverse tcp:8096 tcp:8096`, then `localhost`). Once a feature is implemented and the build passes, leave the debug build launched on the emulator so the user can check the feature, and install the release build on the user's Pixel phone (`./gradlew assembleRelease`, then `adb -s <phone serial> install -r app/build/outputs/apk/release/app-release.apk`), which is the user's everyday app. Update it in place; never uninstall it or clear its data, since it holds the user's own sign-in. Before reporting, open the feature's screens in the release build to catch R8 problems (crashes, missing data). The phone and an emulator are often connected together, so always pick the device by its serial from `adb devices -l` (`ANDROID_SERIAL` for Gradle and `up.sh`). If the phone isn't connected or isn't authorized, say so and ask. Report what was installed where.

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
