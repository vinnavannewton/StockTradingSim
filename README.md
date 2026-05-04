<div align="center">

<br/>

# 📈 StockFlow

### A cross-platform stock trading simulator built with Kotlin Multiplatform & Compose

<br/>

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.7.3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Supabase](https://img.shields.io/badge/Supabase-3.0.0-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)](https://supabase.com/)
[![Android](https://img.shields.io/badge/Android-API_26+-34A853?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue?style=for-the-badge)](LICENSE)

<br/>

*Practice trading with $1,000,000 in virtual cash — real market feel, zero risk.*

<br/>

</div>

---

## ✨ Features

- **📊 Real-time Prices** — Live stock quotes via the Finnhub API, refreshing every 60 seconds (1.5s when focused on a stock)
- **📈 Price Charts** — Smooth line chart with fill gradient for any stock, across multiple time ranges (1M · 5M · 15M · 1H · ALL)
- **💼 Portfolio Tracking** — Track holdings, average buy price, and real-time P&L per position
- **⭐ Watchlist** — Star any stock to keep it in your personal watchlist tab
- **📜 Trade History** — Full log of every buy/sell order
- **☁️ Cloud Sync** — Portfolio, watchlist and history persisted to Supabase — survives app restarts and works across devices
- **🔐 Authentication** — Email/password signup + **Google OAuth** one-tap sign-in
- **🎨 Premium Dark UI** — Glassmorphism-inspired design with smooth micro-animations throughout
- **📱 Responsive Layout** — Adaptive layout: side-by-side desktop view collapses to a bottom-nav mobile view
- **🔍 Sector Filters** — Filter stocks by Technology, Automotive, Finance, Healthcare, Consumer, Fintech
- **⌨️ Desktop Zoom** — `Ctrl +` / `Ctrl -` / `Ctrl 0` to scale the entire UI

---

## 🖥️ Screenshots

> *Dark-mode UI with real-time price simulation, responsive on both desktop and Android*

| Desktop — Market View | Desktop — Stock Detail | Android — Portfolio |
|:-:|:-:|:-:|
| Side-by-side layout with live prices | Price chart with buy/sell panel | Bottom-nav mobile layout |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.1.0 |
| **UI Framework** | Compose Multiplatform 1.7.3 |
| **Platforms** | Android (API 26+) · Linux · Windows |
| **Auth & Database** | Supabase (auth-kt + postgrest-kt 3.0.0) |
| **Market Data** | Finnhub REST API |
| **Networking** | Ktor 3.0.3 |
| **Serialization** | kotlinx.serialization 1.7.3 |
| **Concurrency** | Kotlin Coroutines 1.8.1 |
| **Session Storage** | SharedPreferences (Android) · File (Desktop) |

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version |
|---|---|
| JDK | 17 or 21 |
| Android SDK | API 34 (for Android builds) |
| Gradle | Bundled via `./gradlew` |

Set your Android SDK path in `local.properties`:
```properties
sdk.dir=/home/youruser/Android/Sdk
```

---

## 🔨 Build Commands

### ▶️ Run on Desktop (no install needed)
```bash
./gradlew run
```

### 📱 Android APK
```bash
./gradlew assembleDebug
# → build/outputs/apk/debug/StockFlow-debug.apk
```
Install directly on a connected device or emulator:
```bash
./gradlew installDebug
```

### 🐧 Linux — RPM Package (Fedora / RHEL)
```bash
./gradlew packageRpm
# → build/compose/binaries/main/rpm/stockflow-1.0.0-1.x86_64.rpm
```
```bash
sudo dnf install build/compose/binaries/main/rpm/stockflow-1.0.0-1.x86_64.rpm
```

### 🐧 Linux — DEB Package (Ubuntu / Debian)
> If you're on Fedora and need a `.deb`, use the included Podman helper script:
```bash
./build_deb.sh
# → build/compose/binaries/main/deb/stockflow-1.0.0-1.amd64.deb
```
On Ubuntu/Debian natively:
```bash
./gradlew packageDeb
```

### 🪟 Windows — Installer
```batch
gradlew.bat packageMsi
# → build\compose\binaries\main\msi\StockFlow-1.0.0.msi
```
```batch
gradlew.bat packageExe
# → build\compose\binaries\main\exe\StockFlow-1.0.0.exe
```

> **Note:** Packaging must be run on the **target OS** (e.g., `.rpm`/`.deb` on Linux, `.msi`/`.exe` on Windows). The only exception is building `.deb` on Fedora via `./build_deb.sh` (uses Podman).

---

## 🗂️ Project Structure

```
StockFlowFixed/
├── src/
│   ├── commonMain/kotlin/com/stock/
│   │   ├── api/
│   │   │   ├── FinnhubClient.kt      # Real-time price fetching
│   │   │   └── SupabaseManager.kt    # Auth + cloud data layer
│   │   ├── auth/
│   │   │   └── SessionStorage.kt     # expect/actual session persistence
│   │   ├── model/
│   │   │   ├── Market.kt             # Simulation engine + Finnhub polling
│   │   │   ├── Stock.kt              # Stock data + price history
│   │   │   ├── User.kt               # Portfolio, watchlist, balance logic
│   │   │   ├── Transaction.kt
│   │   │   └── UiState.kt            # Single derived UI state snapshot
│   │   ├── storage/
│   │   │   └── DataStore.kt          # expect/actual local persistence
│   │   ├── ui/
│   │   │   ├── App.kt                # Root composable + auth state machine
│   │   │   ├── LoginScreen.kt        # Email + Google OAuth login UI
│   │   │   ├── StockDetailScreen.kt  # Chart + trading panel
│   │   │   └── Theme.kt              # Colors, gradients, shapes
│   │   └── util/
│   │       ├── Formatting.kt
│   │       └── TimeFormatting.kt
│   ├── androidMain/                  # Android-specific actuals
│   │   └── kotlin/com/stock/android/
│   │       └── MainActivity.kt       # Deep-link handler for Google OAuth
│   └── desktopMain/                  # Desktop-specific actuals
│       └── kotlin/com/stock/desktop/
│           └── Main.kt               # Window + Ctrl+/- zoom feature
├── build_deb.sh                      # Podman helper: build .deb on Fedora
└── build.gradle.kts
```

---

## 🔑 Environment / Configuration

The app connects to a hosted Supabase project. To use your own:

1. Create a project at [supabase.com](https://supabase.com)
2. Create these tables in the SQL editor:

```sql
-- User profiles
create table profiles (
  id uuid references auth.users primary key,
  balance double precision default 1000000,
  initial_balance double precision default 1000000
);

-- Portfolio holdings
create table portfolio_items (
  id bigserial primary key,
  user_id uuid references auth.users,
  symbol text, quantity int, avg_price double precision
);

-- Watchlist
create table watchlist_items (
  id bigserial primary key,
  user_id uuid references auth.users,
  symbol text
);

-- Trade history
create table transactions_log (
  id bigserial primary key,
  user_id uuid references auth.users,
  type text, symbol text, quantity int,
  price_per_share double precision, timestamp bigint
);
```

3. Enable Row Level Security (RLS) on all tables with `auth.uid() = user_id` policies
4. Update `SUPABASE_URL` and `SUPABASE_KEY` in `SupabaseManager.kt`
5. For Google OAuth, configure the provider in Supabase → Authentication → Providers

---

## 🎮 How to Play

1. **Sign up** with email/password or **Continue with Google**
2. You start with **$1,000,000** in virtual cash
3. Browse stocks across 6 sectors — tap/click any to open the detail view
4. Enter a quantity and hit **BUY** or **SELL**
5. Track your portfolio performance in the **Portfolio** tab
6. Your data syncs to the cloud automatically on every trade

---

## 📦 Stocks Available

| Sector | Symbols |
|---|---|
| Technology | AAPL · GOOG · MSFT · NVDA · META · AMZN |
| Automotive | TSLA · F · RIVN |
| Finance | JPM · V · GS |
| Healthcare | JNJ · PFE · UNH |
| Consumer | KO · NKE · SBUX |
| Fintech | COIN · SQ |

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE) for details.

---

<div align="center">

Made with ❤️ using Kotlin Multiplatform

</div>
