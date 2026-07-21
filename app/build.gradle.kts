import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
}

val selectedDataSource =
    providers
        .gradleProperty("vaiinillaDataSource")
        .orElse("MOCK")
        .get()
        .uppercase()
        .also { require(it == "MOCK" || it == "REMOTE") { "vaiinillaDataSource debe ser MOCK o REMOTE" } }

val selectedApiBaseUrl =
    providers
        .gradleProperty("vaiinillaApiBaseUrl")
        .orElse("https://localhost.invalid/api/v1/")
        .get()

android {
    namespace = "com.vaiinilla.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vaiinilla.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-vai5"

        buildConfigField("String", "DATA_SOURCE_MODE", "\"$selectedDataSource\"")
        buildConfigField("String", "API_BASE_URL", "\"$selectedApiBaseUrl\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests.all {
            it.useJUnit()
            it.systemProperty(
                "vaiinilla.fixtureDir",
                file("src/main/assets/fixtures").absolutePath,
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
