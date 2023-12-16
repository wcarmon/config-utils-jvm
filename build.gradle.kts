repositories {
    mavenCentral()
}

plugins {
    java
    id("com.diffplug.spotless") version "6.23.2"
}

group = "com.github.wcarmon"
version = "1.0.0"

configure<JavaPluginExtension> {
    // -- TODO: try to reduce
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("org.jetbrains:annotations:24.1.0")
}
