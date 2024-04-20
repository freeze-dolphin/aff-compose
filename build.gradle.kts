plugins {
    alias(libs.plugins.jvm)
    kotlin("plugin.serialization") version "1.9.22"
    id("com.autonomousapps.dependency-analysis") version "1.29.0"
    `java-library`
    java
    `maven-publish`
}

group = "com.tairitsu"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    testImplementation(libs.junit.jupiter.engine)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${property("kotlinxSerializationJsonVersion")}")

    // https://mvnrepository.com/artifact/com.benasher44/uuid
    implementation("com.benasher44:uuid:0.8.2")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "aff-compose"
            version = project.version.toString()
        }
    }
}