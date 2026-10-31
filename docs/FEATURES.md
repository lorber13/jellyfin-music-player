# Jellyfin Music Player — Feature Specification

An Android music player for Jellyfin with offline playback and Android Auto support.


This document lists **what** the app does. It does not say **when**: features are assigned to releases while building the backlog.

## 1. Account & server
- **ACC-01 Connect to server**: enter a Jellyfin server address. The app checks it and shows the server's name.
- **ACC-02 Log in with username and password.**
- **ACC-03 Log in with Quick Connect**: approve the login from another Jellyfin client.
- **ACC-04 Stay logged in** across app restarts.
- **ACC-05 Log out**: clears all local data. The user is asked whether to keep or delete downloads.
- **ACC-06 Session expired handling**: if the server rejects the login, the user is asked to log in again. Downloaded music keeps playing in the meantime.
- **ACC-07 Unsupported server message** for Jellyfin versions older than 10.9.
- **ACC-08 Account switching**: add several servers or users and switch between them, one active at a time. Each has its own library and downloads.

## 2. Library browsing
- **LIB-01 Home screen**: Recently played, Recently added, Favorites and Downloaded sections.
- **LIB-02 Artists list**, alphabetical with quick A–Z scrolling.
- **LIB-03 Albums list**, sortable by name, year or date added.
- **LIB-04 Playlists list.**
- **LIB-05 Genres list** and genre detail showing the albums in that genre.
- **LIB-06 Artist detail**: albums and top tracks.
- **LIB-07 Album detail**: track list, total duration, year.
- **LIB-08 Playlist detail**: track list and total duration.
- **LIB-09 Search** across artists, albums, tracks and playlists.
- **LIB-10 Favorites**: mark or unmark tracks, albums and artists, synced to the server. Changes made offline sync later.
- **LIB-11 Browse offline**: screens the user has already seen still open with no connection.
- **LIB-12 Large library support**: browsing stays smooth with 10,000+ albums.

## 3. Playlist editing
- **PL-01 Create a playlist.** It is saved on the server.
- **PL-02 Rename and delete playlists.**
- **PL-03 Add to playlist** from any track, album or the current queue.
- **PL-04 Remove and reorder tracks** in a playlist.
- **PL-05 Save queue as playlist.**
- **PL-06 Offline edits**: changes made offline are synced when back online, and downloaded playlists update to match.

## 4. Playback
- **PLY-01 Play** a track, album, playlist or artist.
- **PLY-02 Shuffle** an album, playlist, artist or the whole library.
- **PLY-03 Play next / Add to queue.**
- **PLY-04 Queue management**: view, reorder, remove, clear.
- **PLY-05 Repeat modes**: off, all, one.
- **PLY-06 Mini player** on every screen.
- **PLY-07 Now Playing screen**: artwork, title, artist, album, seek bar, controls, and colors taken from the album art.
- **PLY-08 Background playback** with a media notification and lock-screen controls.
- **PLY-09 Headset & Bluetooth controls**: play/pause/next/previous from buttons.
- **PLY-10 Pause when headphones disconnect**, and lower or pause the volume for calls and navigation prompts.
- **PLY-11 Resume where you left off**: the queue and position survive closing the app and rebooting the phone. The system "resume playback" card works.
- **PLY-12 Gapless playback** between tracks.
- **PLY-13 Original quality when possible**: the original file plays when the phone supports its format. Otherwise the server converts it.
- **PLY-14 Data saver on mobile data**: prefer downloaded or cached copies. Stream only what's missing, at a lower quality limit (default 192 kbps).
- **PLY-15 Play history sync**: plays are reported to Jellyfin so play counts and "Recently played" stay correct. Plays made offline are sent later.
- **PLY-16 Lyrics**: show synced or plain lyrics from Jellyfin on the Now Playing screen.
- **PLY-17 Volume normalization (ReplayGain)**: consistent loudness across tracks and albums.
- **PLY-18 Instant Mix / radio**: start an endless mix from a song, album or artist.

## 5. Downloads & offline
- **OFF-01 Download a track.**
- **OFF-02 Download an album, playlist or artist** (all of its tracks).
- **OFF-03 Keep downloaded playlists up to date**: new tracks are downloaded and removed tracks are deleted automatically.
- **OFF-04 Download quality setting**: original, or smaller converted files (e.g. 128 / 192 / 320 kbps).
- **OFF-05 Download over Wi-Fi only**: on by default.
- **OFF-06 Downloads screen**: progress, size, pause/resume/cancel/delete, and total storage used.
- **OFF-07 Download location**: internal storage or SD card, with existing downloads moved when it changes. If the SD card is removed, its downloads show as unavailable but aren't deleted.
- **OFF-08 Recently-played cache**: streamed songs are kept automatically up to a size limit (default 1 GB) so replays need no data and can work offline.
- **OFF-09 Offline mode**: no connection is detected automatically. An offline indicator appears, songs that can't be played are dimmed, and a playing queue skips them.
- **OFF-10 Downloads survive** app updates and logging back in to the same account.
- **OFF-11 Downloaded indicator** on tracks, albums and playlists throughout the app.

## 6. Android Auto
- **AUTO-01 Browse in the car**: Home, Albums, Playlists and Downloads tabs.
- **AUTO-02 Car Home**: starts with Recently played, then Recently added, Favorites, Artists and Genres.
- **AUTO-03 Album & playlist grids** with artwork. Tracks are shown as lists.
- **AUTO-04 Voice search**: "Play <song / album / artist> on <app>".
- **AUTO-05 Auto-resume on connect**: the last queue continues when the car connects. Can be turned off in settings.
- **AUTO-06 Extra car buttons**: shuffle, repeat, favorite.
- **AUTO-07 Driving-safe lists**: no list longer than about 100 items. Big collections are grouped A–Z and start with "Shuffle all".
- **AUTO-08 Offline in the car**: when there's no connection, only playable (downloaded or cached) music is shown, with Downloads first.
- **AUTO-09 Artwork in the car**, including offline.
- **AUTO-10 Clear car messages**: "Sign in on your phone" and "No downloaded music while offline".

## 7. Settings
- **SET-01 Streaming quality limits**: separate for Wi-Fi and mobile data.
- **SET-02 Data saver toggle** (PLY-14).
- **SET-03 Download settings**: quality, Wi-Fi only, location.
- **SET-04 Cache settings**: size limit and clear cache.
- **SET-05 Android Auto settings**: auto-resume on connect.
- **SET-06 Account**: server, user, log out.
- **SET-07 Export logs** for bug reports (there is no telemetry).
- **SET-08 About**: version, license (GPL-3.0), open-source notices.

## 8. Look & feel
- **UI-01 Material You**: colors follow the wallpaper on Android 12+, with a fixed app palette on older versions.
- **UI-02 Light and dark themes**, following the system.
- **UI-03 Portrait and landscape** on phones.
- **UI-04 Accessibility**: screen-reader labels, large touch targets, support for bigger system fonts.
- **UI-05 Translatable**: English by default, and every text in the app can be translated.
- **UI-06 Community translations**: volunteers can contribute translations through a translation platform.
- **UI-07 Tablet & foldable layouts**: two-pane layouts on large screens, e.g. a list beside its detail, or the library beside Now Playing.

## 9. Out of scope
**Other devices**
- Android TV
- Wear OS
- Android Automotive OS (cars with Android built in). Android Auto *is* in scope.

**Other media types**
- Video, music videos, movies
- Audiobooks
- Podcasts
- Live TV / radio streams

**Other capabilities**
- Casting: Chromecast, DLNA/UPnP, remote-controlling other Jellyfin clients
- Editing server metadata (tags, artwork, album info)
- A merged view combining several servers' libraries (account switching, ACC-08, is in scope)
- Analytics, crash reporting or any tracking

## 10. Product constraints
These are not features, but every feature has to respect them:
- Android 10 or newer (phones, tablets, foldables).
- Jellyfin server 10.9 or newer.
- Published on Google Play and F-Droid. Open source under GPL-3.0, with no Google-proprietary components.
