plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.example.maquirentapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.maquirentapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "cliente"
    productFlavors {
        create("maquirent") {
            dimension = "cliente"
                applicationId = "com.example.maquirentapp"
            versionCode = 4
            versionName = "1.2-maquirent"

            buildConfigField("String", "COMPANY_NAME", "\"MAQUIRENT SERVICIOS GENERALES\"")
            buildConfigField("String", "COMPANY_RUC", "\"10266739414\"")
            buildConfigField("String", "COMPANY_OWNER", "\"Hugo Alberto Esquivel Pando\"")
            buildConfigField("String", "COMPANY_ADDRESS", "\"AV. ATAHUALPA 810 - CAJAMARCA\"")
            buildConfigField("String", "COMPANY_PHONE", "\"976959490 - 955425015 - 969775015\"")
            buildConfigField("String", "COMPANY_CITY", "\"Cajamarca\"")
            buildConfigField("String", "COMPANY_BUSINESS_LINE", "\"Alquiler de Grupos Electrógenos y Maquinaria | Cajamarca - Perú\"")
            buildConfigField("String", "PREFIX_COTIZACION", "\"COT-\"")
            buildConfigField("String", "PREFIX_VALORIZACION", "\"VAL-\"")
            buildConfigField("String", "PREFIX_CERTIFICADO", "\"CERT-\"")
            buildConfigField("double", "TAX_RATE", "0.18")
            buildConfigField("String", "TAX_LABEL", "\"I.G.V. (18%)\"")
            buildConfigField("int", "DEFAULT_MAINTENANCE_HOURS", "250")
        }

        create("volker") {
            dimension = "cliente"
            applicationId = "com.empresa.volker"
            versionCode = 1
            versionName = "1.0-volker"

            buildConfigField("String", "COMPANY_NAME", "\"VOLKER MAQUINARIAS S.A.C.\"")
            buildConfigField("String", "COMPANY_RUC", "\"20609876543\"")
            buildConfigField("String", "COMPANY_OWNER", "\"Gerencia Volker\"")
            buildConfigField("String", "COMPANY_ADDRESS", "\"Av. Industrial 456 - Lima\"")
            buildConfigField("String", "COMPANY_PHONE", "\"01-7654321\"")
            buildConfigField("String", "COMPANY_CITY", "\"Lima\"")
            buildConfigField("String", "COMPANY_BUSINESS_LINE", "\"Alquiler de Generadores y Maquinaria Pesada\"")
            buildConfigField("String", "PREFIX_COTIZACION", "\"COT-VK-\"")
            buildConfigField("String", "PREFIX_VALORIZACION", "\"VAL-VK-\"")
            buildConfigField("String", "PREFIX_CERTIFICADO", "\"CERT-VK-\"")
            buildConfigField("double", "TAX_RATE", "0.18")
            buildConfigField("String", "TAX_LABEL", "\"I.G.V. (18%)\"")
            buildConfigField("int", "DEFAULT_MAINTENANCE_HOURS", "250")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-functions:20.4.0")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-crashlytics")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.viewmodel)
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("it.xabaras.android:recyclerview-swipedecorator:1.4")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.github.yalantis:ucrop:2.2.8")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
