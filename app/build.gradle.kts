plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.gradecalculator_kotlin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gradecalculator_kotlin"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Enables ActivityMainBinding, StudentAdapter binding, etc.
    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    // Apache POI ships with duplicate META-INF files — these exclusions fix build errors
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
                "META-INF/versions/9/module-info.class"
            )
        }
    }
}

dependencies {
    // ── AndroidX & Material ──────────────────────────────────────
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ── Apache POI — Excel read/write (.xlsx) ────────────────────
    implementation("org.apache.poi:poi:5.2.3") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation("org.apache.poi:poi-ooxml:5.2.3") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation("org.apache.commons:commons-compress:1.24.0")
    implementation("com.github.virtuald:curvesapi:1.08")
    implementation("org.apache.xmlbeans:xmlbeans:5.1.1")

    // ── Coroutines — prevents UI freeze (ANR) on heavy tasks ────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // ── Testing ──────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}