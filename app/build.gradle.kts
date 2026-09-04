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
    environmentName: String,
    defaultValue: String,
): String =
    providers.gradleProperty(name).orNull?.takeIf(String::isNotBlank)
        ?: providers.environmentVariable(environmentName).orNull?.takeIf(String::isNotBlank)
        ?: localProperties.getProperty(name)?.takeIf(String::isNotBlank)
        ?: defaultValue

fun escapeBuildConfig(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\$", "\\\$")

val selectedApiBaseUrl =
    readConfig(
        "vaiinillaApiBaseUrl",
        "VAIINILLA_API_BASE_URL",
        "https://localhost.invalid/api/v1/",
    )

// Debug/preview builds should be installable without requiring every developer
// to create local.properties first. Release still requires an explicit URL.
val selectedDebugApiBaseUrl =
    readConfig(
        "vaiinillaApiBaseUrl",
        "VAIINILLA_API_BASE_URL",
        "https://vaiinillaback-development-3f6c.up.railway.app/api/v1/",
    )

val releaseApiBaseUrl =
    readConfig(
        "vaiinillaApiBaseUrl",
        "VAIINILLA_API_BASE_URL",
        "",
    )

val productionApiBaseUrl = "https://vaiinillaback-development-3f6c.up.railway.app/api/v1/"

val releaseStoreFile =
    readConfig(
        "vaiinillaReleaseStoreFile",
        "VAIINILLA_RELEASE_STORE_FILE",
        "",
    )
val releaseStorePassword =
    readConfig(
        "vaiinillaReleaseStorePassword",
        "VAIINILLA_RELEASE_STORE_PASSWORD",
        "",
    )
val releaseKeyAlias =
    readConfig(
        "vaiinillaReleaseKeyAlias",
        "VAIINILLA_RELEASE_KEY_ALIAS",
        "",
    )
val releaseKeyPassword =
    readConfig(
        "vaiinillaReleaseKeyPassword",
        "VAIINILLA_RELEASE_KEY_PASSWORD",
        "",
    )
val releaseSigningInputs =
    listOf(
        releaseStoreFile,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    )
val hasCompleteReleaseSigning = releaseSigningInputs.all(String::isNotBlank)
val hasPartialReleaseSigning = releaseSigningInputs.any(String::isNotBlank) && !hasCompleteReleaseSigning
if (hasPartialReleaseSigning) {
    throw GradleException(
        "Release signing requires VAIINILLA_RELEASE_STORE_FILE, " +
            "VAIINILLA_RELEASE_STORE_PASSWORD, VAIINILLA_RELEASE_KEY_ALIAS, " +
            "and VAIINILLA_RELEASE_KEY_PASSWORD together.",
    )
}

val isProdReleaseTask =
    gradle.startParameter.taskNames.any {
        it.contains("Production", ignoreCase = true) && it.contains("Release", ignoreCase = true)
    }
if (isProdReleaseTask && releaseApiBaseUrl.isBlank()) {
    throw GradleException(
        "Release builds require -PvaiinillaApiBaseUrl or VAIINILLA_API_BASE_URL. " +
            "Do not ship the localhost.invalid fallback.",
    )
}
if (
    isProdReleaseTask &&
    releaseApiBaseUrl.trimEnd('/') != productionApiBaseUrl.trimEnd('/')
) {
    throw GradleException(
        "Release builds are pinned to the verified production API endpoint. " +
            "Refusing VAIINILLA_API_BASE_URL=$releaseApiBaseUrl",
    )
}

// Seed passwords: local.properties / -P only. Never commit real values.
val seedPasswordCliente =
    readConfig("vaiinillaSeedPasswordCliente", "VAIINILLA_SEED_PASSWORD_CLIENTE", "")
val seedPasswordCajero =
    readConfig("vaiinillaSeedPasswordCajero", "VAIINILLA_SEED_PASSWORD_CAJERO", "")
val seedPasswordCocina =
    readConfig("vaiinillaSeedPasswordCocina", "VAIINILLA_SEED_PASSWORD_COCINA", "")
val seedPasswordMesero =
    readConfig("vaiinillaSeedPasswordMesero", "VAIINILLA_SEED_PASSWORD_MESERO", "")

val selectedVersionCode =
    providers.gradleProperty("vaiinillaVersionCode").orNull
        ?: providers.environmentVariable("VAIINILLA_VERSION_CODE").orNull
        ?: "16"
val selectedVersionName =
    providers.gradleProperty("vaiinillaVersionName").orNull
        ?: providers.environmentVariable("VAIINILLA_VERSION_NAME").orNull
        ?: "0.5.0"

android {
    namespace = "com.vaiinilla.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vaiinilla.app"
        minSdk = 26
        targetSdk = 36
        versionCode = selectedVersionCode.toIntOrNull() ?: error("VAIINILLA_VERSION_CODE must be an integer")
        versionName = selectedVersionName

        // Defaults: release-safe. Debug buildType overrides below.
        buildConfigField("boolean", "SEED_AUTH_ENABLED", "false")
        buildConfigField("String", "SEED_PASSWORD_CLIENTE", "\"\"")
        buildConfigField("String", "SEED_PASSWORD_CAJERO", "\"\"")
        buildConfigField("String", "SEED_PASSWORD_COCINA", "\"\"")
        buildConfigField("String", "SEED_PASSWORD_MESERO", "\"\"")
    }

    flavorDimensions += "environment"
    productFlavors {
        create("development") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            buildConfigField("String", "ENVIRONMENT_NAME", "\"development\"")
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://vaiinillaback-development.up.railway.app/api/v1/\"",
            )
        }
        create("production") {
            dimension = "environment"
            buildConfigField("String", "ENVIRONMENT_NAME", "\"production\"")
            val prodUrl = if (releaseApiBaseUrl.isNotBlank()) releaseApiBaseUrl else productionApiBaseUrl
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"$prodUrl\"",
            )
        }
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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasCompleteReleaseSigning) {
                val productionSigningConfig =
                    signingConfigs.create("production") {
                        storeFile = rootProject.file(releaseStoreFile)
                        storePassword = releaseStorePassword
                        keyAlias = releaseKeyAlias
                        keyPassword = releaseKeyPassword
                    }
                signingConfig = productionSigningConfig
            }
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
    implementation(libs.haze.core)
    implementation(libs.haze.blur)
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
    implementation(libs.stripe.android)
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
