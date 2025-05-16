plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.entregador"
    compileSdk = 34

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
                "META-INF/NOTICE",
                "/META-INF/AL2.0",
                "/META-INF/LGPL2.1",
                "/META-INF/*.kotlin_module",
                "/META-INF/*.version",
                "/module-info.class"
            )
        }
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    // GeoPackage (já inclui ORMLite)
    implementation("mil.nga.geopackage:geopackage-android:5.1") {
        exclude(group = "com.j256.ormlite")
    }

    // ORMLite Core
    implementation("com.j256.ormlite:ormlite-core:6.1")

    // GSon
    implementation("com.google.code.gson:gson:2.10.1")

    // Android Core
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // OSMdroid + Mapsforge
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("org.osmdroid:osmdroid-mapsforge:6.1.18")
    implementation("org.osmdroid:osmdroid-geopackage:6.1.18")
    implementation("org.mapsforge:mapsforge-map-reader:0.20.0")

    // GraphHopper all-in-one JAR
    add("implementation", files("libs/graphhopper-11.0-all.jar"))

    // Jackson (versão correta usada pelo GraphHopper)
    //implementation("com.fasterxml.jackson.core:jackson-core:2.13.4")
    //implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4")
    //implementation("com.fasterxml.jackson.core:jackson-annotations:2.13.4")

    // SLF4J
    //implementation("org.slf4j:slf4j-api:1.7.36")
    //implementation("org.slf4j:slf4j-simple:1.7.36")

    // Apache Commons
    //implementation("org.apache.commons:commons-lang3:3.13.0")

    // XML Parsing
    implementation("net.sf.kxml:kxml2:2.3.0")

    // Testes
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

// Exclusões globais para evitar duplicações
configurations.all {
    exclude(group = "com.j256.ormlite")
    exclude(group = "org.slf4j")
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "com.fasterxml.jackson.core")
    exclude(group = "com.fasterxml.jackson.annotation")
    exclude(group = "com.fasterxml.jackson.databind")
}