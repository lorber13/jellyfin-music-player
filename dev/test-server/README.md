# Local Jellyfin test server

A Jellyfin server in Docker with a generated sample music library, so every developer tests against the same data. One command starts it and sets it up (TECH-03).

## Requirements

`docker` (with Compose), `ffmpeg`, `magick` (ImageMagick 7), `jq` and `curl`. `adb` is optional; it is used to make the server reachable from a phone.

## Commands

```bash
dev/test-server/up.sh          # generate the music if needed, start the server, set it up on the first run
dev/test-server/down.sh        # stop the server, keep its data
dev/test-server/reset.sh       # stop and delete the server data, so the next up.sh starts fresh
dev/test-server/reset.sh --all # also delete the generated music and every version's data
```

`up.sh` is safe to run again at any time. On the first run it creates the test user, adds the Music library, waits for the scan and creates a playlist. On later runs it skips what already exists and only starts the server and the `adb reverse` forward.

## Connecting

| From             | Server address          |
| ---------------- | ----------------------- |
| This machine     | `http://localhost:8096` |
| Phone over USB   | `http://localhost:8096` |
| Android emulator | `http://10.0.2.2:8096`  |

User **`test`**, password **`test`** (administrator).

The server listens on 127.0.0.1 only, so it can't be reached from your network. For a phone, `up.sh` runs `adb reverse tcp:8096 tcp:8096`, which forwards the phone's `localhost:8096` to this machine. If more than one device is connected, set `ANDROID_SERIAL` to pick one. The forward is lost when the phone is unplugged; run `up.sh` again to restore it.

The server uses plain HTTP, which the app allows for every server (`app/src/main/res/xml/network_security_config.xml`). On the server screen, enter `localhost` (the app adds port 8096 itself).

## Server version

The default is Jellyfin **12.4**. Set `JELLYFIN_VERSION` to any [`jellyfin/jellyfin` image tag](https://hub.docker.com/r/jellyfin/jellyfin/tags) to test another one:

```bash
JELLYFIN_VERSION=10.9.11 dev/test-server/up.sh   # oldest supported version
JELLYFIN_VERSION=10.8.13 dev/test-server/up.sh   # unsupported version, for ACC-07
JELLYFIN_VERSION=10.8.13 dev/test-server/down.sh # use the same variable with down.sh and reset.sh
```

Each version keeps its own server data in `data/<version>/`, so switching versions never upgrades or downgrades a database. Only one version runs at a time, because they all use port 8096.

## Sample library

`generate-library.sh` (called by `up.sh`) writes 11 albums and 84 tracks into `data/music/` in about 40 seconds. The tracks are sine tones, 30 seconds long unless noted. All artists and albums are fictional, and the library has internet metadata turned off, so the server never matches them to real ones.

| Album                              | Artist             | Format       | What it covers                                                                          |
| ---------------------------------- | ------------------ | ------------ | --------------------------------------------------------------------------------------- |
| Morning Light (2015)               | Aurora Fields      | FLAC 16/44.1 | Plain album with `folder.jpg` artwork                                                   |
| Evening Tide (2018)                | Aurora Fields      | FLAC 24/96   | Hi-res file the server may have to convert (PLY-13)                                     |
| Gapless Horizons (2021)            | Aurora Fields      | FLAC 16/44.1 | One continuous sweep across 5 tracks: a gap or click between tracks is audible (PLY-12) |
| Steam & Rust (2009)                | The Copper Kettles | MP3 320      | Artwork embedded in the files, no `folder.jpg`                                          |
| Pressure Valve (2012)              | The Copper Kettles | MP3 320      | Embedded artwork                                                                        |
| Canciones del Río (2016)           | Café Ñandú         | AAC (`.m4a`) | Embedded artwork, accented names                                                        |
| Été à Montréal (2019)              | Café Ñandú         | Opus         | Accented names                                                                          |
| ネオン Signals (2020)              | 東京 Echo Unit     | Opus         | Japanese names                                                                          |
| Grid Lines (Deluxe Edition) (2022) | 東京 Echo Unit     | FLAC 16/44.1 | 2 discs of 6 tracks (LIB-07)                                                            |
| Long Form (2023)                   | 東京 Echo Unit     | Ogg Vorbis   | No artwork at all; a 10-minute track for seeking                                        |
| Summer Sampler Vol. 1 (2024)       | Various Artists    | MP3 320      | Compilation: each track has its own artist, including ones with no album                |

Together they give 5 album artists with 1–3 albums each, 5 genres and release years from 2009 to 2024. `up.sh` also creates **Sample Playlist** with 10 tracks from different albums and formats.

To change the library, edit `generate-library.sh`, then run `reset.sh --all` and `up.sh`.
