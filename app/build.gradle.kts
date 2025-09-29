plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.digitalsix.leitornfc" // Verifique se o seu namespace está correto
    compileSdk = 35

    defaultConfig {
        applicationId = "com.digitalsix.leitornfc" // Verifique se o seu applicationId está correto
        minSdk = 23
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    // O bloco buildFeatures foi removido daqui!
    // O bloco composeOptions foi removido daqui!
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Dependências existentes
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // NOVAS DEPENDÊNCIAS PARA O SISTEMA DE LOGIN:

    // Material Design Components (para TextInputLayout)
    implementation("com.google.android.material:material:1.12.0")

    // CardView (para os cartões da interface)
    implementation("androidx.cardview:cardview:1.0.0")

    // Lifecycle (para lifecycleScope)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // Coroutines (já deve estar incluído via lifecycle)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Gson (para serialização JSON - já incluído via retrofit)
    implementation("com.google.code.gson:gson:2.10.1")

    // Testes
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}