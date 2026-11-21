# Jellyfin Music Player

An Android music player for [Jellyfin](https://jellyfin.org) with offline playback and Android Auto support. Open source under [GPL-3.0](LICENSE), with no proprietary dependencies and no telemetry.

The app is in early development. Today it is an empty shell with navigation; see the backlog for what comes next.

- [docs/FEATURES.md](docs/FEATURES.md): what the app will do
- [docs/BACKLOG.md](docs/BACKLOG.md): the order in which it gets built

## Requirements

- JDK 17 or newer
- Android SDK with platform 37 (`ANDROID_HOME` set, or `sdk.dir` in `local.properties`)
- A device or emulator running Android 10 or newer

## Build and run

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew installDebug           # install it on a connected device
```

## Checks

```bash
./gradlew spotlessCheck          # formatting (fix with ./gradlew spotlessApply)
./gradlew lint                   # Android Lint, warnings fail the build
./gradlew testDebugUnitTest      # unit tests
./gradlew connectedDebugAndroidTest   # UI tests, needs a connected device
```

CI ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) runs formatting, lint, unit tests and the debug build on every push to `main` and on every pull request.

## Local test server

`dev/test-server/up.sh` starts a Jellyfin server in Docker with a generated sample music library and the user `test` / `test`. A phone connected over USB reaches it at `http://localhost:8096`, the emulator at `http://10.0.2.2:8096`. See [dev/test-server/README.md](dev/test-server/README.md).

## Project layout

| Module              | Contents                                               |
| ------------------- | ------------------------------------------------------ |
| `app`               | Application, single activity, app shell and navigation |
| `core/designsystem` | Theme and shared UI components                         |
| `core/data`         | Data layer (repositories)                              |
| `core/playback`     | Playback service and the API screens use to play music |
| `feature/albums`    | Albums list and album detail screens                   |
| `feature/login`     | Server address and sign-in screens                     |
| `build-logic`       | Gradle convention plugins shared by the modules        |
