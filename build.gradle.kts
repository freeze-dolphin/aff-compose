val kotlinxSerializationVersion: String by project

plugins {
    alias(libs.plugins.jvm)
    kotlin("plugin.serialization") version "1.9.23"
    java
    antlr

    `java-library`
    `maven-publish`

    id("com.autonomousapps.dependency-analysis") version "1.31.0"
}

group = "com.tairitsu"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    testImplementation(libs.junit.jupiter.engine)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
    implementation("com.benasher44:uuid:0.8.2")

    antlr("org.antlr:antlr4:4.13.1")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.generateGrammarSource {
    val antlrPath = sourceSets.main.get().antlr.srcDirs.first()
    arguments = listOf(
        "-package", "com.tairitsu.compose.arcaea.antlr",
        "-lib", file(antlrPath.path, "com", "tairitsu", "compose", "arcaea", "antlr").path
    )
}

tasks.generateTestGrammarSource {
    arguments = listOf(
        "-package", "com.tairitsu.compose.arcaea.antlr"
    )
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}

tasks.compileTestKotlin {
    dependsOn(tasks.generateTestGrammarSource)
}

sourceSets {
    main {
        java {
            srcDir(tasks.generateGrammarSource)
        }
    }
    test {
        java {
            srcDir(tasks.generateGrammarSource)
        }
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

fun file(vararg dirs: String): File = dirs.reduce { acc, next ->
    File(acc, next).path
}.let {
    File(it)
}