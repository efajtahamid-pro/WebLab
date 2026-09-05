# WebLab v1.0.0 Release

## 🎉 Initial Release - WebLab Android IDE

Turn your Android phone into a lightweight local web-development workstation.

WebLab is a native Android IDE for building websites and web apps directly on your phone — files, a touch-friendly code editor, a live preview, a real terminal, and npm package management, all running against your own project sandbox on-device.

## ✨ Key Features

- **Project Management** — Create Static Website, Node.js, or Vite-style projects from built-in templates
- **File Explorer** — Full CRUD operations (new/rename/delete/duplicate/move), global filename + content search, all sandboxed to the project directory
- **Code Editor** — Line numbers, real syntax highlighting (HTML/CSS/JS/TS/JSON), undo/redo, touch coding toolbar, debounced autosave
- **Live Preview** — Real WebView rendering static projects directly, or a project's local dev server; mobile/tablet/desktop viewport modes
- **Terminal** — Real filesystem commands (cd, pwd, ls, mkdir, rm, cp, mv, cat, echo, clear) plus node/npm/npx, confined to the current project
- **Package Management** — Reads and writes the project's real package.json; install/uninstall packages and run npm scripts
- **Import/Export** — Projects as ZIP archives with protection against zip-slip and archive bombs
- **Security-First** — Every filesystem and archive operation validated through PathValidator; project JS never accesses Android APIs

## 📱 Installation

1. Download `weblab-debug-apk.zip` from the release assets
2. Extract the APK file
3. On your Android device (8.0+ / API 26+), enable "Install unknown apps" for your file manager
4. Sideload the APK and launch WebLab

## 🏗️ Technical Specifications

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material 3
- **State Management**: ViewModel + StateFlow
- **Database**: Room (metadata only; all source files live as real files on disk)
- **Build System**: Gradle 8.7
- **Java Version**: Java 17
- **Target SDK**: Android 14 (SDK 34)
- **Minimum SDK**: Android 8.0 (API 26)
- **Architecture**: Clean separation between UI → ViewModel → Repository → Filesystem/Room/Runtime

## 🔒 Security Model

- **PathValidator** — Central choke point for all path resolution; blocks directory traversal and validates all ZIP entries (zip-slip protection)
- **Archive Safety** — Caps total entry count and uncompressed size to defend against archive bombs
- **WebView Sandbox** — Project JS runs only within WebView sandbox; never uses `addJavascriptInterface`
- **Safe Execution** — ProcessBuilder with structured argument lists; no shell string concatenation

## 🚀 Node.js Runtime (Optional)

Static-site projects work fully offline with zero setup. To enable real `node`/`npm`/`npx` execution for Node/Vite projects:

1. Obtain an ARM64 Node.js build
2. Place the `node` executable at `app/src/main/jniLibs/arm64-v8a/libnode.so`
3. (Optional) Place npm CLI at `app/src/main/jniLibs/arm64-v8a/libnpm_cli.so`
4. Rebuild — Settings > Runtime will show "Ready" with the detected version

## 📚 Documentation & Source

- **GitHub Repository**: https://github.com/efajtahamid-pro/WebLab
- **README**: https://github.com/efajtahamid-pro/WebLab#readme
- **License**: MIT

## 👨‍💻 Built By

**Dewan Efaj Tahamid Rifat** — AI Architect & Full-Stack AI Engineer

- Portfolio: https://dewanefaj.netlify.app
- Hire: https://efajtahamid.monster

---

## 📦 Build Information

- **Build Run**: https://github.com/efajtahamid-pro/WebLab/actions/runs/33973635088
- **Artifact ID**: 9971799709
- **APK Size**: ~18.2 MB
- **Build Status**: ✅ Successful

## 🔄 What's Next

- Bundle the ARM64 Node.js + npm runtime asset
- Multi-process dashboard for concurrent frontend/backend dev servers
- Command palette (⌘K-style quick actions)
- Git UI on top of existing GitManager abstraction
- Bundled Git binary for on-device version control

Enjoy building on your Android device! 🚀
