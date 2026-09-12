import org.gradle.api.initialization.resolve.RepositoriesMode
import org.gradle.caching.http.HttpBuildCache

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VaiinillaAndroid"
include(":app")

// Shared remote build cache (build-cache-node on the VPS behind Caddy TLS).
// Activate by providing GRADLE_REMOTE_CACHE_URL/USER/PASSWORD env vars or the
// remoteBuildCache* gradle properties (e.g. in ~/.gradle/gradle.properties).
val remoteCacheUrl = providers.gradleProperty("remoteBuildCacheUrl")
    .orElse(providers.environmentVariable("GRADLE_REMOTE_CACHE_URL")).orNull
if (remoteCacheUrl != null) {
    buildCache {
        remote<HttpBuildCache> {
            url = uri(remoteCacheUrl)
            isAllowUntrustedServer = false
            isAllowInsecureProtocol = false
            isPush = providers.gradleProperty("remoteBuildCachePush")
                .orElse(providers.environmentVariable("GRADLE_REMOTE_CACHE_PUSH"))
                .orElse("false").get() == "true"
            credentials {
                username = providers.gradleProperty("remoteBuildCacheUser")
                    .orElse(providers.environmentVariable("GRADLE_REMOTE_CACHE_USER")).orNull
                password = providers.gradleProperty("remoteBuildCachePassword")
                    .orElse(providers.environmentVariable("GRADLE_REMOTE_CACHE_PASSWORD")).orNull
            }
        }
    }
}
