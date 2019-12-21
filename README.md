# LiveSync: Acoustic-based Live Video Synchronization

KAIST CS442 (19 Fall) Project by Taeckyung LEE and Junhyeok Choi.

## Test Environment

Android (SDK version 26 and later)

Requires Speaker, Microphone, and Bluetooth available.

## Installation

Binary file: https://drive.google.com/open?id=1EVbe35_cbNHkg8zd64kiqCLBkUvNw9x2
Download apk file above or build as a typical Android application via Android Studio.

## build.gradle

```groovy
apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'
apply plugin: 'kotlin-android-extensions'

android {
    compileSdkVersion 29
    buildToolsVersion "29.0.2"
    defaultConfig {
        applicationId "com.terry00123.livesync"
        minSdkVersion 26
        targetSdkVersion 29
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation"org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"
    implementation 'androidx.appcompat:appcompat:1.0.2'
    implementation 'androidx.core:core-ktx:1.0.2'
    implementation 'com.google.android.material:material:1.0.0'
    implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
    testImplementation 'junit:junit:4.12'
    androidTestImplementation 'androidx.test.ext:junit:1.1.0'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.1.1'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:0.30.1-eap13'
}

kotlin {
    experimental {
        coroutines "enable"
    }
}
```
