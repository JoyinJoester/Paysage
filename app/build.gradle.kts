import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import java.util.regex.Pattern

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
}

android {
    namespace = "joyin.takgi.paysage"
    compileSdk {
        version = release(36)
    }

    val appVersionCode = 1
    val baseVersionName = "1.0.0"
    val apkPrefix = "Paysage"
    val packagedAbiTags = listOf("arm64-v8a", "armeabi-v7a")

    fun normalizeTwoDigits(raw: Any?): String {
        val digits = raw?.toString()
            ?.trim()
            ?.filter { it.isDigit() }
            .orEmpty()
        val padded = digits.ifBlank { "01" }.padStart(2, '0')
        return padded.takeLast(2)
    }

    fun sanitizeTag(raw: Any?, fallback: String): String {
        val value = raw?.toString()?.trim().orEmpty()
        if (value.isBlank()) return fallback
        val cleaned = value.replace(Regex("[^0-9A-Za-z._+-]"), "_")
        return cleaned.ifBlank { fallback }
    }

    fun escapeForBuildConfig(raw: String): String =
        raw.replace("\\", "\\\\").replace("\"", "\\\"")

    fun gitSha(): String =
        runCatching {
            providers.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
            }.standardOutput.asText.get().trim().ifBlank { "unknown" }
        }.getOrDefault("unknown")

    fun findNextDailySeqForVersion(versionNameTag: String, dateTag: String, versionTag: String): Int {
        val versionPrefix = "$versionNameTag-$dateTag$versionTag"
        val apkNamePattern = Pattern.compile(
            "^$apkPrefix-Android-.+-${Pattern.quote(versionPrefix)}-(\\d{2})\\.[aA][pP][kK]$"
        )
        val outputsDir = layout.buildDirectory.dir("outputs/apk").get().asFile
        if (!outputsDir.exists()) return 1

        val maxSeq = outputsDir
            .walkTopDown()
            .filter { it.isFile }
            .mapNotNull { file ->
                val matcher = apkNamePattern.matcher(file.name)
                if (matcher.matches()) matcher.group(1).toIntOrNull() else null
            }
            .maxOrNull()
            ?: 0
        return maxSeq + 1
    }

    val buildTimeZone = TimeZone.getTimeZone("Asia/Shanghai")
    val buildDateTag = SimpleDateFormat("yyMMdd").apply { timeZone = buildTimeZone }.format(Date())
    val buildTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").apply { timeZone = buildTimeZone }.format(Date())
    val buildVersionTag = normalizeTwoDigits(project.findProperty("apkVersionTag") ?: appVersionCode)
    val versionNameTag = sanitizeTag(project.findProperty("apkVersionName") ?: baseVersionName, "0.0.0")
    val buildDailySeq = normalizeTwoDigits(
        project.findProperty("dailySeq")
            ?: findNextDailySeqForVersion(versionNameTag, buildDateTag, buildVersionTag)
    )
    val buildDetailTag = "$buildDateTag$buildVersionTag-$buildDailySeq"
    val fullVersionName = "$baseVersionName-$buildDetailTag"
    val declaredApkArch = sanitizeTag(
        project.findProperty("apkArch") ?: packagedAbiTags.sorted().joinToString("+"),
        packagedAbiTags.sorted().joinToString("+")
    )
    val currentGitSha = sanitizeTag(gitSha(), "unknown")

    defaultConfig {
        applicationId = "joyin.takgi.paysage"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = baseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += packagedAbiTags
        }

        buildConfigField("String", "BUILD_TIME", "\"${escapeForBuildConfig(buildTime)}\"")
        buildConfigField("String", "FULL_VERSION_NAME", "\"${escapeForBuildConfig(fullVersionName)}\"")
        buildConfigField("String", "BUILD_DETAIL_TAG", "\"${escapeForBuildConfig(buildDetailTag)}\"")
        buildConfigField("String", "APK_ARCH", "\"${escapeForBuildConfig(declaredApkArch)}\"")
        buildConfigField("String", "GIT_SHA", "\"${escapeForBuildConfig(currentGitSha)}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/LICENSE.md"
        }
    }

    applicationVariants.all {
        outputs.all {
            val abiTag = filters
                .firstOrNull { filter ->
                    val typeText = filter.filterType
                    typeText.equals("ABI", ignoreCase = true) || typeText.uppercase().endsWith(".ABI")
                }
                ?.identifier
                ?.let { sanitizeTag(it, declaredApkArch) }
                ?: declaredApkArch
            val outputVersionName = sanitizeTag(
                project.findProperty("apkVersionName") ?: versionName ?: baseVersionName,
                "0.0.0"
            )
            (this as BaseVariantOutputImpl).outputFileName =
                "$apkPrefix-Android-$abiTag-$outputVersionName-${buildDateTag}${buildVersionTag}-$buildDailySeq.APK"
        }
    }
}

dependencies {
    implementation(project(":lpac-jni"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // AppCompat for language switching
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // JavaMail
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Reliable background work
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // In-app QR scanning
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("com.google.zxing:core:3.5.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
