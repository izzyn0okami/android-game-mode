plugins {
    id("com.android.application")
}

// Package opened when a controller connects. Override with -PfrontendPackage=<id>
// (for example com.nendo.argosy.debug for a debug build of the frontend).
val frontendPackage = providers.gradleProperty("frontendPackage").getOrElse("com.nendo.argosy")

android {
    namespace = "com.izzyn0okami.gamemode.service"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.izzyn0okami.gamemode.service"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "FRONTEND_PACKAGE", "\"$frontendPackage\"")
        manifestPlaceholders["frontendPackage"] = frontendPackage
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":runtime"))
}
