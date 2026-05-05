import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.android.application") version "8.13.1"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

group = "com.stock"
version = "1.0.0"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    jvm("desktop")

    jvmToolchain(17)

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation("org.jetbrains.compose.components:components-ui-tooling-preview:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("io.github.jan-tennert.supabase:postgrest-kt:3.0.0")
                implementation("io.github.jan-tennert.supabase:auth-kt:3.0.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
                // Ktor HTTP client (core + JSON support for FinnhubClient)
                implementation("io.ktor:ktor-client-core:3.0.3")
                implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("io.ktor:ktor-client-okhttp:3.0.3")
                implementation("io.ktor:ktor-server-core:3.0.3")
                implementation("io.ktor:ktor-server-netty:3.0.3") // Switching to Netty as well for stability
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-compose:1.9.3")
                implementation("io.ktor:ktor-client-android:3.0.3")
            }
        }
    }
}

android {
    namespace = "com.stock.stockflow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.stock.stockflow"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            kotlin.srcDirs("src/androidMain/kotlin")
            res.srcDirs("src/androidMain/res")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

compose.desktop {
    application {
        mainClass = "com.stock.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi, TargetFormat.Exe)
            packageName = "StockFlow"
            packageVersion = "1.0.0"

            // Include the java.net.http module so ktor-client-java works in the bundled JDK
            modules(
                "java.net.http",
                "jdk.crypto.ec",       // TLS/HTTPS support
                "jdk.crypto.cryptoki", // PKCS11 crypto
                "java.naming",         // JNDI (needed by some JVM internals)
                "jdk.security.auth"
            )

            windows {
                // Register the stockflow:// URI scheme so Windows routes OAuth callbacks to this app
                // jpackage will embed this in the installer
                upgradeUuid = "4A2B3C4D-5E6F-7A8B-9C0D-1E2F3A4B5C6D"
                menuGroup = "StockFlow"
                perUserInstall = true
            }
        }
    }
}

dependencies {
    debugImplementation("org.jetbrains.compose.ui:ui-tooling:1.7.3")
}
