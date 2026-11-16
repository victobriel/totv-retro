import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// ---- Optional cores (SNES / Mega Drive) ---------------------------------------------------------
// Off by default. Enable with  -PretroExtraCores=snes,md   (or "all"), or with a decision file
// (-PretroBenchDecision=/path/file, lines `snes=include` / `md=include`).
// The cores themselves must have been fetched first:  tools/fetch-cores.sh --extra
val extraCores: Set<String> = run {
    val prop = (findProperty("retroExtraCores") as String?)?.split(',')?.map { it.trim().lowercase() }?.toMutableSet()
        ?: mutableSetOf()
    val decision = file(
        (findProperty("retroBenchDecision") as String?)
            ?: "/tmp/claude-1000/-home-victo-Desktop-personal-totv/720db899-8641-431d-bb7b-9ff354344d51/scratchpad/bench-decision.txt"
    )
    if (decision.isFile) {
        decision.readLines().forEach { line ->
            val (k, v) = (line.substringBefore('=').trim().lowercase()) to (line.substringAfter('=', "").trim().lowercase())
            if (k in setOf("snes", "md") && v in setOf("include", "yes", "true", "1", "on")) prop += k
        }
    }
    if ("all" in prop) setOf("snes", "md") else prop
}

android {
    namespace = "app.totv.retro"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.totv.retro"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }
        buildConfigField("String", "EXTRA_CORES", "\"${extraCores.sorted().joinToString(",")}\"")
    }

    // Release key: keystore.properties (gitignored; a copy lives in ~/.config/totv-retro/).
    val keystoreProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) create("release") {
            storeFile = file(keystoreProps.getProperty("storeFile"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    // Cores are loaded with dlopen() from nativeLibraryDir, so they must be extracted on install.
    packaging { jniLibs { useLegacyPackaging = true } }

    sourceSets {
        getByName("main") {
            jniLibs.srcDir(rootProject.file("cores/base"))
            if (extraCores.isNotEmpty()) jniLibs.srcDir(rootProject.file("cores/extra"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }
    lint { abortOnError = false }
}

dependencies {
    // Emulation front-end library (GPL-3.0). Pinned: see THIRD_PARTY.md.
    implementation("com.github.Swordfish90:LibretroDroid:0.14.0")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
