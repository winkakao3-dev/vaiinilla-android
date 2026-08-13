import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.google.services)
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
}

val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }

fun readConfig(
    name: String,
    defaultValue: String,
): String =
    providers.gradleProperty(name).orNull
        ?: localProperties.getProperty(name)
        ?: defaultValue

fun escapeBuildConfig(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\$", "\\\$")

val selectedApiBaseUrl =
    readConfig(
        "vaiinillaApiBaseUrl",
        "https://localhost.invalid/api/v1/",
    )

// Seed passwords: local.properties / -P only. Never commit real values.
val seedPasswordCliente = readConfig("vaiinillaSeedPasswordCliente", "")
val seedPasswordCajero = readConfig("vaiinillaSeedPasswordCajero", "")
val seedPasswordCocina = readConfig("vaiinillaSeedPasswordCocina", "")
val seedPasswordMesero = readConfig("vaiinillaSeedPasswordMesero", "")

android {
    namespace = "com.vaiinilla.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vaiinilla.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 15
        versionName = "0.4.1-vai27-remote"

        buildConfigField("String", "API_BASE_URL", "\"$selectedApiBaseUrl\"")
        // Defaults: release-safe. Debug buildType overrides below.
        buildConfigField("boolean", "SEED_AUTH_ENABLED", "false")
        buildConfigField("String", "SEED_PASSWORD_CLIENTE", "\"\"")
        buildConfigField("String", "SEED_PASSWORD_CAJERO", "\"\"")
        buildConfigField("String", "SEED_PASSWORD_COCINA", "\"\"")
        buildConfigField("String", "SEED_PASSWORD_MESERO", "\"\"")
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("boolean", "SEED_AUTH_ENABLED", "true")
            buildConfigField(
                "String",
                "SEED_PASSWORD_CLIENTE",
                "\"${escapeBuildConfig(seedPasswordCliente)}\"",
            )
            buildConfigField(
                "String",
                "SEED_PASSWORD_CAJERO",
                "\"${escapeBuildConfig(seedPasswordCajero)}\"",
            )
            buildConfigField(
                "String",
                "SEED_PASSWORD_COCINA",
                "\"${escapeBuildConfig(seedPasswordCocina)}\"",
            )
            buildConfigField(
                "String",
                "SEED_PASSWORD_MESERO",
                "\"${escapeBuildConfig(seedPasswordMesero)}\"",
            )
        }
        getByName("release") {
            buildConfigField("boolean", "SEED_AUTH_ENABLED", "false")
            buildConfigField("String", "SEED_PASSWORD_CLIENTE", "\"\"")
            buildConfigField("String", "SEED_PASSWORD_CAJERO", "\"\"")
            buildConfigField("String", "SEED_PASSWORD_COCINA", "\"\"")
            buildConfigField("String", "SEED_PASSWORD_MESERO", "\"\"")
        }
        create("preview") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release", "debug")
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("boolean", "SEED_AUTH_ENABLED", "true")
            buildConfigField(
                "String",
                "SEED_PASSWORD_CLIENTE",
                "\"${escapeBuildConfig(seedPasswordCliente)}\"",
            )
            buildConfigField(
                "String",
                "SEED_PASSWORD_CAJERO",
                "\"${escapeBuildConfig(seedPasswordCajero)}\"",
            )
            buildConfigField(
                "String",
                "SEED_PASSWORD_COCINA",
                "\"${escapeBuildConfig(seedPasswordCocina)}\"",
            )
            buildConfigField(
                "String",
                "SEED_PASSWORD_MESERO",
                "\"${escapeBuildConfig(seedPasswordMesero)}\"",
            )
        }
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
                    file("src/test/fixtures").absolutePath,
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
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.zxing.core)
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
