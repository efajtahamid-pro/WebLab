# WebLab

Turn your Android phone into a lightweight local web-development workstation.

WebLab is a native Android IDE for building websites and web apps directly on
your phone — files, a touch-friendly code editor, a live preview, a real
terminal, and npm package management, all running against your own project
sandbox on-device.

> **Built by [Dewan Efaj Tahamid Rifat](https://efajtahamid.monster)** — AI Architect & Full-Stack AI Engineer.
> Need something built? [Hire the developer →](https://efajtahamid.monster)

---

## Features

- **Project management** — create Static Website, Node.js, or Vite-style projects from built-in templates
- **File explorer** — full CRUD (new/rename/delete/duplicate/move), global filename + content search, all sandboxed to the project directory
- **Code editor** — line numbers, real syntax highlighting (HTML/CSS/JS/TS/JSON), undo/redo, a touch coding toolbar (`< > { } [ ] ( ) / = ; : " ' _`, TAB), debounced autosave
- **Live preview** — a real WebView rendering static projects directly, or a project's local dev server once it's running; mobile/tablet/desktop viewport modes
- **Terminal** — real filesystem commands (`cd`, `pwd`, `ls`, `mkdir`, `rm`, `cp`, `mv`, `cat`, `echo`, `clear`) plus `node`/`npm`/`npx`, all confined to the current project
- **Package management** — reads and writes the project's real `package.json`; install/uninstall packages and run npm scripts with real output, no simulated data
- **Import/export** — projects as ZIP archives, protected against zip-slip and archive bombs
- **Security-first** — every filesystem and archive operation is resolved through a central path validator; a project's JavaScript never gets access to Android APIs

## Architecture

```
UI (Jetpack Compose)
  ↓
ViewModel (StateFlow, Coroutines)
  ↓
Repository (ProjectRepository, FileRepository)
  ↓
Filesystem / Room (metadata only) / Runtime (Node process manager)
```

- **UI**: Jetpack Compose + Material 3, dark-first developer theme (charcoal / cyan / purple)
- **State**: `ViewModel` + `StateFlow`, no unnecessary DI framework — a small manual container in `WebLabApp`
- **Persistence**: Room stores only project *metadata* (name, path, type, timestamps); every source file lives as a real file on disk, never duplicated into the database
- **Security**: `PathValidator` is the single choke point for path resolution — every file operation and every ZIP entry is resolved through it before touching disk

## Node.js Runtime

WebLab runs Node.js **locally on the device** — there is no remote server and no
cloud execution. Because Android only allows executing native binaries that
were packaged as JNI libraries (so they land in a directory the OS marks
executable), the runtime is wired up like this:

1. Bundle an ARM64 Node.js build at `app/src/main/jniLibs/arm64-v8a/libnode.so`
   (a standard Node executable, renamed to satisfy Android's `jniLibs`
   packaging convention — the same technique terminal-emulator apps use).
2. `NodeRuntimeManager` (`runtime/NodeRuntimeManager.kt`) looks for that binary
   at `applicationInfo.nativeLibraryDir/libnode.so` at runtime.
3. If it's present, WebLab reports `READY` with the real detected version and
   can start/stop/restart real `node`/`npm`/`npx` processes, capturing real
   stdout/stderr.
4. If it's **not** present (the default in this repository, since no binary
   asset is checked in), WebLab honestly reports `NOT_INSTALLED` everywhere —
   in Settings, in the Packages screen, and in the Terminal — instead of
   faking a server or faking command output.

This is a deliberate build decision (see spec section 41: no simulated
execution, anywhere). Static-site projects work fully offline with zero
runtime asset required, since their preview loads straight from disk into a
WebView.

Git support (`runtime/GitManager.kt`) follows the identical pattern: it really
shells out to a `git` binary, and reports "Git is not available" honestly on a
stock device rather than faking `git status`/`git log` output.

## Security Model

- **`PathValidator`** resolves every relative path against the project root's
  *canonical* path and rejects anything that would resolve outside it —
  blocking `../../` traversal, and the same check backs ZIP-entry validation
  (zip-slip protection) on import.
- **ZIP import** additionally caps total entry count and total uncompressed
  size to defend against archive bombs.
- **WebView preview** never calls `addJavascriptInterface` — a project's own
  JS can only run inside the WebView sandbox, never reach Android APIs.
- **Process execution** uses `ProcessBuilder` with structured argument lists,
  never unsafe shell string concatenation.

## Project Structure

```
WebLab/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/efajtahamid/weblab/
│   │   │   │   ├── data/            # models, Room db, repositories
│   │   │   │   ├── runtime/         # Node runtime, terminal, npm, zip, git, ports
│   │   │   │   ├── security/        # PathValidator
│   │   │   │   ├── template/        # project scaffolding
│   │   │   │   └── ui/              # Compose screens, ViewModels, navigation, theme
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── test/                    # JVM unit tests
│   └── build.gradle.kts
├── .github/workflows/android.yml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Installation

Grab the latest debug APK from the **Actions** tab (each push builds one), or
a signed release APK from the **Releases** page for tagged versions. Sideload
it onto an Android 8.0+ (API 26+) device with "Install unknown apps" enabled
for your file manager or browser.

## Development Setup

1. Install **Android Studio** (Koala or newer) with SDK 34.
2. Clone the repo and open it in Android Studio.
3. Let Android Studio regenerate the Gradle wrapper jar if prompted (this repo
   ships `gradlew` / `gradlew.bat` / `gradle-wrapper.properties`, but not the
   binary `gradle-wrapper.jar` — Android Studio, or running `gradle wrapper`
   once with a local Gradle 8.7 install, will produce it).
4. Sync Gradle, then Run on a device or emulator (arm64 recommended, since
   that's the only Node runtime architecture this build targets).

### Node Runtime Setup (optional, for real Node/npm execution)

Static-site projects work with zero setup. To enable real `node`/`npm`/`npx`
execution for Node/Vite projects:

1. Obtain an ARM64 Node.js build.
2. Place the `node` executable at `app/src/main/jniLibs/arm64-v8a/libnode.so`.
3. (Optional, for `npm`) place npm's CLI entry point at
   `app/src/main/jniLibs/arm64-v8a/libnpm_cli.so`.
4. Rebuild — Settings > Runtime will show `Ready` with the detected version.

## Building

```bash
./gradlew assembleDebug     # debug APK -> app/build/outputs/apk/debug/
./gradlew assembleRelease   # release APK (needs keystore.properties, see below)
./gradlew testDebugUnitTest # unit tests
./gradlew lintDebug         # lint
```

## GitHub Actions

`.github/workflows/android.yml` runs on every push/PR: unit tests → lint →
debug APK build → artifact upload. Pushing a tag matching `v*.*.*` additionally
builds and signs a release APK and publishes it as a GitHub Release. Signing
uses these repository secrets (never hardcoded):

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## Roadmap

- Bundle the ARM64 Node.js + npm runtime asset described above
- Multi-process dashboard for concurrent frontend/backend dev servers
- Command palette (`⌘K`-style quick actions)
- Git UI on top of the existing `GitManager` abstraction
- Bundled Git binary for on-device version control

## Contributing

Issues and PRs are welcome. Please keep new subsystems consistent with the
existing architecture (UI → ViewModel → Repository → Filesystem/Room/Runtime)
and run `./gradlew testDebugUnitTest lintDebug` before submitting.

## License

MIT — see `LICENSE`.

## Contact

Built and maintained by **Dewan Efaj Tahamid Rifat**.
Portfolio: https://dewanefaj.netlify.app · Hire: https://efajtahamid.monster
