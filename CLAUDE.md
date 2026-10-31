# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

An Android music player for Jellyfin with offline playback and Android Auto support. The repo is at **specification stage**: there is no source code or Gradle build yet. Implementation starts with backlog item TECH-01 (project skeleton). When that lands, add build, lint and test commands (including how to run a single test) and a short architecture overview to this file.

## Working agreement

**Ask the user before making decisions.** Whenever a choice isn't already settled by the user or by the docs (scope, priorities, sizes, naming, file locations, dependencies, architecture, conventions), ask first and offer options with a recommendation. Don't pick one and report it afterwards.

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
