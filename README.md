# StockFlow — Kotlin Multiplatform Stock Simulator

## Prerequisites

- **JDK 17** (required for all platforms)
- **Android SDK** with API 34 (for Android APK)
- **Android Studio** or manually set `sdk.dir` in `local.properties`

## Build Commands

### Android APK (debug)
```
./gradlew assembleDebug
# Output: build/outputs/apk/debug/StockFlow-debug.apk
```

### Desktop — Linux .deb (run on Linux)
```
./gradlew packageDeb
# Output: build/compose/binaries/main/deb/
```

### Desktop — Linux .rpm (run on Linux, requires rpm-build)
```
./gradlew packageRpm
# Output: build/compose/binaries/main/rpm/
```

### Desktop — Windows .exe installer (run on Windows)
```
.\gradlew.bat packageExe
# Output: build\compose\binaries\main\exe\
```

### Desktop — Windows .msi installer (run on Windows)
```
.\gradlew.bat packageMsi
# Output: build\compose\binaries\main\msi\
```

### Build everything (on current platform)
```
./gradlew build
```

## Important Notes

1. **Edit `local.properties`** and set `sdk.dir` to your Android SDK path before building the APK.
2. Linux packages (deb/rpm) must be built on Linux.
3. Windows packages (exe/msi) must be built on Windows.
4. The desktop app requires Java 17+ to run standalone (before packaging).
