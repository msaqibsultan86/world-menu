# World Menu — fresh build design

Date: 2026-09-05
Status: approved for planning

## Purpose

Add two buttons to Minecraft's Select World screen: one that opens the saves
folder, one that imports a world folder from anywhere on disk. The mod is
client-side and has nothing to configure.

This is a rewrite. The existing 146 lines are discarded rather than patched.

## Version target

Build for **Minecraft 1.21.1 / Fabric** first, and test against it.

The long-term goal is the full range Mod Menu supports (1.14.4 through the
current 1.21.x and beyond). That range spans three Java versions and several
incompatible rendering APIs, so it is a build-system problem, not a code
problem. The chosen approach is Stonecutter — one source tree with
version-conditional regions, generating one jar per Minecraft version.

This spec covers 1.21.1 only. Stonecutter is not introduced yet, but the
package layout and the isolation of version-sensitive code (screen hooks,
rendering calls) are chosen so that adding it later means adding versions to a
list rather than restructuring source.

## Behaviour

### Placement

Both buttons sit on `SelectWorldScreen`, above the world list and clear of the
screen title:

- Top-left corner: **Open World Folder**, folder icon
- Top-right corner: **Import World**, upload icon

Each button draws its 16x16 sprite to the left of its label. Positions are
computed from screen width so they stay in their corners at any resolution and
GUI scale.

### Open World Folder

Opens the current version's `saves` directory using
`Util.getOperatingSystem().open(path)` — the same call vanilla uses for "Open
Resource Pack Folder". This avoids `java.awt.Desktop`, which is unreliable
alongside GLFW.

If the directory does not exist it is created first, so the button never fails
silently on a fresh install.

### Import World

1. Opens a native folder picker through LWJGL's `TinyFileDialogs`.
   LWJGL ships with Minecraft, so this adds no dependency. `JFileChooser` is
   rejected: AWT deadlocks against GLFW on macOS.
2. The picker runs off the render thread. The chosen path is handed back to the
   client thread before any world state is touched.
3. The selected folder is validated: it must exist, be a directory, and contain
   a readable `level.dat`.
4. It is copied into `saves/`. If a world with that folder name already exists,
   a numeric suffix is appended rather than overwriting.
5. The world list reloads so the imported world appears without reopening the
   screen.

### Errors

Every failure path produces a message on screen, not only in the log:

- No folder chosen — nothing happens, no message
- Folder has no `level.dat` — "That folder isn't a Minecraft world"
- Copy fails (permissions, disk full, source vanished mid-copy) — a message
  naming what went wrong

A partially copied world is deleted on failure so the saves folder is never
left with a broken entry.

## Structure

Package `studio.spark.worldmenu`. Five files, one responsibility each.

| File | Responsibility |
|---|---|
| `WorldMenuClient` | Entry point. Subscribes to the screen-init event. |
| `SelectWorldScreenButtons` | Builds and positions the two buttons. The only file that knows about `SelectWorldScreen`. |
| `IconButton` | Button widget that draws a sprite beside its label. |
| `SavesDirectory` | Resolves the saves path and opens it. |
| `WorldImporter` | Pick, validate, copy, report. |

`SelectWorldScreenButtons` and `IconButton` are the version-sensitive files.
Isolating them there is what makes the later multi-version work tractable.

## Assets

- `assets/worldmenu/textures/gui/folder.png` — 16x16, folder
- `assets/worldmenu/textures/gui/import.png` — 16x16, upward arrow
- `assets/worldmenu/icon.png` — the mod icon shown in Mod Menu, taken from
  `domain_icon_bg.png` at 128x128

Sprites are drawn to sit against vanilla's palette rather than stand out.

## Build

Local builds are the primary path; CI is the fallback.

The machine has JDK 21 at `~/.jdks/jdk-21.0.12+8`, and every required
repository is reachable (`maven.fabricmc.net`, Maven Central, the Gradle plugin
portal, `services.gradle.org`). The system default JDK is 26, which Loom will
not build against, so the build pins its toolchain to 21 explicitly rather than
inheriting whatever is on PATH.

The project currently has no Gradle wrapper and there is no `gradle` on PATH —
this is why it has only ever built in CI. The rewrite adds the wrapper, so
`./gradlew build` works locally.

The existing GitHub Actions workflow is kept and simplified: it builds from
repo source only. The current zip-fallback branch is dead weight now that the
source lives in the repo.

## Code style

- No narration comments. A comment exists only where the code cannot explain a
  decision by itself — the TinyFileDialogs-over-AWT choice is the main one.
- Small methods with names that say what they do.
- Errors handled where they occur, not funnelled through a catch-all.
- No unused abstraction. There is one screen and two buttons; the code should
  read that way.

## Verification

There is no automated test suite for a client mod of this size. Verification is
running it:

1. `./gradlew build` produces a jar without warnings.
2. Launch 1.21.1 with Fabric API and the mod installed.
3. Mod Menu shows the mod with the correct name and icon.
4. Singleplayer screen: both buttons render in their corners. Check at GUI
   scale 1 and 4, and at a small window size, that they do not overlap the
   title or the world list.
5. Open World Folder opens the correct `saves` directory.
6. Import a real world folder — it appears in the list and is playable.
7. Import a folder with no `level.dat` — a clean on-screen message, no crash,
   nothing written to `saves`.
8. Import a world whose folder name already exists — both worlds survive.

## Out of scope

- Any Minecraft version other than 1.21.1
- Stonecutter setup
- Exporting or deleting worlds
- A configuration screen
- Server-side anything
