# Jellyfin Music Player — Backlog

Built from [FEATURES.md](FEATURES.md). Feature items keep their spec IDs. **TECH-xx** items are technical enablers that are not in the spec but are needed to deliver the features.

**Sizes**: **S** ≤ 1 day · **M** 2–3 days · **L** about 1 week · **XL** more than 1 week (split before starting)

**Milestones** are ordered by value. Each one ends with an app that works.

| Milestone | Goal | Items |
|---|---|---|
| [M1 Foundation](#m1--foundation-log-in-and-stream) | Log in and stream an album | 16 |
| [M2 Player complete](#m2--library--player-complete) | Daily-driver online player | 24 |
| [M3 Offline](#m3--offline) | Works without a connection | 16 |
| [M4 Android Auto](#m4--android-auto) | Full car experience | 12 |
| [M5 Public release](#m5--public-release) | Ready for Play + F-Droid | 14 |
| [M6 Extras](#m6--extras) | Nice-to-haves after the public release | 5 |

---

## M1 — Foundation: log in and stream
Done when: a user logs in to their server, opens an album and listens to it with the screen off.

| ID | Item | Size |
|---|---|---|
| TECH-01 | Project skeleton (Gradle setup, modules, app shell, navigation) | M |
| TECH-02 | CI: build, lint and unit tests on every push | S |
| TECH-03 | Local Jellyfin test server with a sample music library for development | S |
| UI-01 | Material You theme (dynamic color + fallback palette) | S |
| UI-02 | Light and dark theme following the system | S |
| UI-05 | All texts in translatable resources from day one | S |
| ACC-01 | Enter and validate a server address | S |
| ACC-02 | Log in with username and password | M |
| ACC-03 | Log in with Quick Connect | M |
| ACC-04 | Stay logged in across restarts | S |
| ACC-07 | "Unsupported server version" message (< 10.9) | S |
| LIB-03 | Albums list with sorting | M |
| LIB-07 | Album detail screen | S |
| PLY-01 | Play a track, album, playlist or artist | L |
| PLY-08 | Background playback with notification and lock-screen controls | M |
| PLY-13 | Original quality when supported, server conversion otherwise | M |

## M2 — Library & player complete
Done when: the whole library can be browsed and played online, with a full player and queue.

| ID | Item | Size |
|---|---|---|
| LIB-01 | Home screen (recently played, recently added, favorites) | M |
| LIB-02 | Artists list with A–Z scrolling | M |
| LIB-04 | Playlists list | S |
| LIB-05 | Genres list and genre detail | M |
| LIB-06 | Artist detail (albums, top tracks) | M |
| LIB-08 | Playlist detail | S |
| LIB-09 | Search | M |
| LIB-10 | Favorites (mark, unmark, sync) | M |
| LIB-12 | Smooth browsing for 10,000+ album libraries | L |
| PLY-02 | Shuffle album, playlist, artist or whole library | S |
| PLY-03 | Play next / Add to queue | S |
| PLY-04 | Queue screen: view, reorder, remove, clear | M |
| PLY-05 | Repeat modes | S |
| PLY-06 | Mini player on every screen | M |
| PLY-07 | Now Playing screen with album-art colors | M |
| PLY-09 | Headset and Bluetooth button controls | S |
| PLY-10 | Pause on headphone disconnect; handle calls and navigation prompts | S |
| PLY-11 | Resume queue and position after app close or reboot | M |
| PLY-12 | Gapless playback | S |
| PLY-15 | Play history sync to Jellyfin | M |
| ACC-05 | Log out | M |
| ACC-06 | Session-expired handling | M |
| SET-01 | Streaming quality limits (Wi-Fi / mobile data) | S |
| SET-06 | Account settings | S |

## M3 — Offline
Done when: a pinned album plays in airplane mode, and plays and favorites sync after reconnecting.

| ID | Item | Size |
|---|---|---|
| OFF-01 | Download a track | L |
| OFF-02 | Download an album, playlist or artist | M |
| OFF-03 | Keep downloaded playlists up to date | M |
| OFF-04 | Download quality setting | S |
| OFF-05 | Download over Wi-Fi only | S |
| OFF-06 | Downloads screen | M |
| OFF-07 | Download location: internal or SD card | L |
| OFF-08 | Recently-played cache with size limit | M |
| OFF-09 | Offline mode (detect, indicate, dim, skip) | L |
| OFF-10 | Downloads survive updates and re-login | M |
| OFF-11 | Downloaded indicator across the app | S |
| LIB-11 | Browse previously seen screens offline | L |
| PLY-14 | Data saver on mobile data | M |
| SET-02 | Data saver toggle | S |
| SET-03 | Download settings | S |
| SET-04 | Cache settings | S |

## M4 — Android Auto
Done when: everything below works in the Desktop Head Unit (DHU) and in a real car, both online and offline.

| ID | Item | Size |
|---|---|---|
| TECH-04 | Android Auto test setup (DHU) and car checklist | S |
| AUTO-01 | Browse tabs: Home, Albums, Playlists, Downloads | L |
| AUTO-02 | Car Home sections | S |
| AUTO-03 | Grids and lists with artwork | S |
| AUTO-04 | Voice search | M |
| AUTO-05 | Auto-resume on connect | S |
| AUTO-06 | Shuffle, repeat and favorite buttons | M |
| AUTO-07 | Driving-safe lists (≤ 100 items, A–Z groups, Shuffle all) | M |
| AUTO-08 | Offline car browsing | M |
| AUTO-09 | Artwork in the car, including offline | M |
| AUTO-10 | Clear car error messages | S |
| SET-05 | Android Auto settings | S |

## M5 — Public release
Done when: the app is published on Google Play and F-Droid.

| ID | Item | Size |
|---|---|---|
| PL-01 | Create a playlist | S |
| PL-02 | Rename and delete playlists | S |
| PL-03 | Add to playlist from track, album or queue | M |
| PL-04 | Remove and reorder playlist tracks | M |
| PL-05 | Save queue as playlist | S |
| PL-06 | Offline playlist edits | L |
| UI-03 | Portrait and landscape | M |
| UI-04 | Accessibility pass | M |
| UI-07 | Tablet and foldable two-pane layouts | L |
| SET-07 | Export logs | M |
| SET-08 | About screen (version, GPL-3.0, open-source notices) | S |
| TECH-05 | Release signing and versioning | S |
| TECH-06 | F-Droid metadata and reproducible build | M |
| TECH-07 | Google Play listing (store text, screenshots, privacy policy) | S |

## M6 — Extras
| ID | Item | Size |
|---|---|---|
| PLY-16 | Lyrics | M |
| PLY-17 | Volume normalization (ReplayGain) | M |
| PLY-18 | Instant Mix / radio | S |
| ACC-08 | Account switching | L |
| UI-06 | Community translations | S |
