plugins {
    id("com.android.library") version "8.2.0"
    kotlin("android") version "1.8.22"
}

android {
    namespace = "ai.ailia.llm"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src/main/kotlin")
            java.srcDirs("src/main/java") // Keep for compatibility
        }
    }
}

kotlin {
    jvmToolchain(8)
}
