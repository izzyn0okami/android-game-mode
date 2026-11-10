plugins {
    id("com.android.application")
}

android {
    namespace = "com.izzyn0okami.gamemode.diagnostic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.izzyn0okami.gamemode.diagnostic"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
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
