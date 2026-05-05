<div align="center">
  <img src="https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Compose_Multiplatform-000000?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white" />
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Linux-FCC624?style=for-the-badge&logo=linux&logoColor=black" />
  <img src="https://img.shields.io/badge/Windows-0078D6?style=for-the-badge&logo=windows&logoColor=white" />

  <h1>📈 StockFlow</h1>
  <p><strong>A Modern, Multiplatform Stock Trading Simulator Built with Kotlin & Compose Multiplatform</strong></p>
</div>

---

## 🌟 Overview

**StockFlow** is a comprehensive, cross-platform stock trading simulator application. Built from the ground up using **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**, it delivers a seamless, native-feeling user experience across Android, Windows, and Linux.

Whether you're looking to practice trading strategies without financial risk, or explore the capabilities of modern KMP architecture, StockFlow provides real-time market data, secure user authentication, and persistent portfolio management.

## ✨ Key Features

- **📱 True Multiplatform**: A single codebase powering an Android App (APK), Windows installers (.exe, .msi), and Linux packages (.deb, .rpm).
- **🔒 Secure Authentication**: Integrated with [Supabase Auth](https://supabase.com/auth) for robust, cross-platform user login and session management, including custom URI scheme routing for desktop OAuth.
- **📊 Real-Time Market Data**: Powered by the [Finnhub API](https://finnhub.io/), fetching live stock prices, company details, and market trends.
- **💼 Portfolio Management**: Simulated trading environment backed by a Supabase Postgres database to track your transactions, balances, and stock ownership.
- **🎨 Modern UI/UX**: Built entirely with Jetpack/JetBrains Compose Multiplatform, utilizing Material Design components for a beautiful, responsive interface.
- **⚡ Reactive Architecture**: Leverages Kotlin Coroutines, StateFlow, and Ktor for asynchronous, non-blocking network calls and state management.

## 🛠️ Technology Stack

| Domain | Technology |
|---|---|
| **Core Language** | [Kotlin 2.1.0](https://kotlinlang.org/) |
| **UI Framework** | [Compose Multiplatform 1.7.3](https://www.jetbrains.com/lp/compose-multiplatform/) |
| **Backend / BaaS** | [Supabase](https://supabase.com/) (`postgrest-kt`, `auth-kt`) |
| **Networking** | [Ktor Client & Server 3.0.3](https://ktor.io/) |
| **Concurrency** | Kotlinx Coroutines |
| **Serialization** | Kotlinx Serialization JSON |
| **Build Tool** | Gradle (Kotlin DSL) |

## 🏗️ Project Architecture

The project follows standard Kotlin Multiplatform structure to maximize code sharing while allowing platform-specific implementations where necessary:

- **`commonMain`**: Contains 90%+ of the application logic. This includes the Compose UI (`App.kt`, Screens), Supabase integration, Finnhub API client (`FinnhubClient.kt`), state management, and generic interface definitions for Auth and Storage.
- **`androidMain`**: Android-specific implementations for DataStore, Intent-based Auth routing, and `MainActivity`.
- **`desktopMain`**: JVM-specific implementations for desktop environments, including an embedded Ktor Netty server for handling OAuth redirect callbacks, Windows URI scheme registration, and file-based session storage.

## 🚀 Getting Started

### Prerequisites

To build and run this project locally, ensure you have the following installed:
- **JDK 17** (Required for compiling and desktop packaging)
- **Android SDK** (API 34) (For Android builds)
- **Android Studio** (Recommended IDE) or IntelliJ IDEA.

*Note: Ensure your `local.properties` file in the project root points to your Android SDK:*
```properties
sdk.dir=/path/to/your/android/sdk
```

### 🔑 Environment Setup

1. **Supabase Setup**: You will need a Supabase project. Set up your database tables for users and transactions.
2. **Finnhub API Key**: Obtain a free API key from [Finnhub](https://finnhub.io/).
3. *(Add your configuration variables/keys into the appropriate configuration file or environment variables as defined in the source code).*

### 📦 Build Commands

StockFlow uses Gradle wrappers for building targets across platforms.

#### 🤖 Android (APK)
Build a debug APK.
```bash
./gradlew assembleDebug
# Output: build/outputs/apk/debug/StockFlow-debug.apk
```

#### 🐧 Desktop — Linux (Requires Linux host)
Build Debian (`.deb`) or RPM (`.rpm`) packages.
```bash
./gradlew packageDeb
# Output: build/compose/binaries/main/deb/

./gradlew packageRpm # Requires rpm-build tool
# Output: build/compose/binaries/main/rpm/
```

#### 🪟 Desktop — Windows (Requires Windows host)
Build Executable (`.exe`) or MSI (`.msi`) installers.
```powershell
.\gradlew.bat packageExe
# Output: build\compose\binaries\main\exe\

.\gradlew.bat packageMsi
# Output: build\compose\binaries\main\msi\
```

#### 🏃‍♂️ Run Locally
To run the desktop application directly from source without packaging:
```bash
./gradlew run
```

## 📝 Important Notes

- **Platform Dependencies**: Desktop installers (`.exe`, `.msi`, `.deb`, `.rpm`) use `jpackage` and **must** be built on their respective host operating systems (e.g., build Windows packages on a Windows machine).
- **Standalone Java**: The desktop app bundles its own Java runtime environment via `jpackage`, meaning end-users do not need Java installed on their machines to run the installed `.exe` or `.deb`.

## 📜 License

This project is open-source and available under the terms of the project's [LICENSE](LICENSE) file.

---
<div align="center">
  <i>Built with ❤️ using Kotlin Multiplatform</i>
</div>
