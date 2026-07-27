import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun readConfig(name: String, defaultValue: String): String =
    providers.gradleProperty(name).orNull
        ?: localProperties.getProperty(name)
        ?: defaultValue

val selectedDataSource = readConfig("vaiinillaDataSource", "MOCK")
    .uppercase()
    .also { require(it == "MOCK" || it == "REMOTE") { "vaiinillaDataSource debe ser MOCK o REMOTE" } }

val selectedApiBaseUrl = readConfig(
    "vaiinillaApiBaseUrl",
    "https://localhost.invalid/api/v1/",
)

val bootstrapAccessToken = readConfig("vaiinillaAccessToken", "")
val tokenCliente = readConfig("vaiinillaAccessTokenCliente", bootstrapAccessToken)
val tokenCajero = readConfig("vaiinillaAccessTokenCajero", "")
val tokenCocina = readConfig("vaiinillaAccessTokenCocina", "")
val tokenMesero = readConfig("vaiinillaAccessTokenMesero", "")

android {
    namespace = "com.vaiinilla.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vaiinilla.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.3.1-vai11"

        buildConfigField("String", "DATA_SOURCE_MODE", "\"$selectedDataSource\"")
        buildConfigField("String", "API_BASE_URL", "\"$selectedApiBaseUrl\"")
        buildConfigField("String", "BOOTSTRAP_ACCESS_TOKEN", "\"$bootstrapAccessToken\"")
        buildConfigField("String", "ACCESS_TOKEN_CLIENTE", "\"$tokenCliente\"")
        buildConfigField("String", "ACCESS_TOKEN_CAJERO", "\"$tokenCajero\"")
        buildConfigField("String", "ACCESS_TOKEN_COCINA", "\"$tokenCocina\"")
        buildConfigField("String", "ACCESS_TOKEN_MESERO", "\"$tokenMesero\"")
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
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnit()
                it.systemProperty(
                    "vaiinilla.fixtureDir",
                    file("src/main/assets/fixtures").absolutePath,
                )
                it.systemProperties["robolectric.pixelCopyRenderMode"] = "hardware"
            }
        }
    }
}

roborazzi {
    outputDir.set(file("src/test/roborazzi"))
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
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
