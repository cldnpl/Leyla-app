import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Secrets do not belong in git, so they come from android/local.properties
// (already gitignored) rather than from this file.
private val localProps: Properties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Google Maps needs an API key. Put
//   MAPS_API_KEY=AIza...
// in local.properties. Without it the map card falls back to a static panel
// instead of rendering a grey tile grid.
val mapsApiKey: String = localProps.getProperty("MAPS_API_KEY") ?: ""

// Google sign-in sends an ID token minted for the *web* OAuth client, not the
// Android one — that is what Credential Manager's serverClientId means and what
// the backend checks against GOOGLE_CLIENT_IDS. Put
//   GOOGLE_WEB_CLIENT_ID=....apps.googleusercontent.com
// in android/local.properties. Empty hides the button rather than offering a
// sign-in that cannot complete.
val googleWebClientId: String = localProps.getProperty("GOOGLE_WEB_CLIENT_ID") ?: ""

android {
    namespace = "com.claudianapolitano.leyla"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.claudianapolitano.leyla"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.androidx.security.crypto)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    implementation(libs.androidx.health.connect)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
