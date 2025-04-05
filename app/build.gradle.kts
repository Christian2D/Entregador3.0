plugins {

    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.entregador"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.entregador"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE",
                "META-INF/NOTICE"
            )
            excludes += "/META-INF/AL2.0"
            excludes += "/META-INF/LGPL2.1"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "/META-INF/*.version"
            excludes += "**/module-info.class"
        }
    }
}

configurations.all {
    // Estratégia para resolver conflitos
    resolutionStrategy {
        force("com.j256.ormlite:ormlite-core:6.1")
        force("com.j256.ormlite:ormlite-android:6.1")
        preferProjectModules()
    }
}

dependencies {
    // ORMLite (usar apenas o Android)
    implementation("com.j256.ormlite:ormlite-android:5.1") {
        exclude(group = "com.j256.ormlite", module = "ormlite-core")
    }

    // GeoPackage (adicionar exclusão)
    implementation("mil.nga.geopackage:geopackage-android:5.1") {
        exclude(group = "com.j256.ormlite")
    }

    // Android Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // OSMdroid
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("org.osmdroid:osmdroid-mapsforge:6.1.18")
    implementation("org.osmdroid:osmdroid-geopackage:6.1.18")

    // MapsForge
    implementation("org.mapsforge:mapsforge-map-reader:0.20.0")

    // Apache Commons
    implementation("org.apache.commons:commons-lang3:3.13.0")

    // XML Parsing
    implementation("net.sf.kxml:kxml2:2.3.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    configurations.all {
        resolutionStrategy {
            force ("com.j256.ormlite:ormlite-core:5.1")
            force ("com.j256.ormlite:ormlite-android:5.1")
        }
    }
}