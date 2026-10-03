import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.soyvinko.app"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.soyvinko.app"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.property("soyvinko.versionCode") as String).toInt()
        versionName = project.property("soyvinko.versionName") as String
        // URL pública de la API v1. Se puede cambiar al compilar: -PapiBaseUrl=https://...
        val apiBaseUrl = (project.findProperty("apiBaseUrl") as String?) ?: "https://soyvinko.com/"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "WEB_BASE_URL", "\"https://soyvinko.com\"")
        // Manifiesto de versiones para las actualizaciones obligatorias dentro de la app.
        val updateManifest = (project.findProperty("updateManifestUrl") as String?) ?: "https://soyvinko.com/downloads/android/latest.json"
        buildConfigField("String", "UPDATE_MANIFEST_URL", "\"$updateManifest\"")
    }

    // Firma de release: la clave vive fuera del repositorio (por defecto ~/.soyvinko/android/signing.properties).
    // Todas las versiones deben firmarse con ESTA clave o Android rechazará la actualización.
    val signingFile = ((project.findProperty("soyvinkoSigning") as String?)
        ?: "${System.getProperty("user.home")}/.soyvinko/android/signing.properties").let(::file)
    signingConfigs {
        if (signingFile.exists()) {
            create("release") {
                val props = Properties().apply { signingFile.inputStream().use(::load) }
                storeFile = File(signingFile.parentFile, props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Sin ofuscación: la app no se puede probar en un dispositivo desde este servidor, y R8
            // puede romper en tiempo de ejecución código con reflexión. Se puede activar más adelante.
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        // Capturas de pantalla en la JVM (Roborazzi + Robolectric) para revisar el diseño sin emulador.
        unitTests.isIncludeAndroidResources = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Toda la app (pantallas, datos, plataforma) vive en :shared, común con iOS.
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.location)
    implementation(libs.osmdroid)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
