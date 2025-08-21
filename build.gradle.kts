@file:OptIn(ExperimentalWasmDsl::class)

import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.konan.properties.hasProperty
import java.util.*

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.vanniktech.mavenPublish)

    alias(libs.plugins.kotlin.antlr)
}

group = "com.tairitsu"

val generateKotlinGrammarSource = tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    dependsOn("cleanGenerateKotlinGrammarSource")

    // ANTLR .g4 files are under {example-project}/antlr
    // Only include *.g4 files. This allows tools (e.g., IDE plugins)
    // to generate temporary files inside the base path
    source = fileTree(layout.projectDirectory.dir("src").dir("antlr")) {
        include("**/*.g4")
    }

    // We want the generated source files to have this package name
    val pkgName = "com.tairitsu.compose.arcaea.antlr"
    packageName = pkgName

    // We want visitors alongside listeners.
    // The Kotlin target language is implicit, as is the file encoding (UTF-8)
    arguments = listOf("-visitor")

    // Generated files are outputted inside build/generatedAntlr/{package-name}
    val outDir = "generatedAntlr/${pkgName.replace(".", "/")}"
    outputDirectory = layout.buildDirectory.dir(outDir).get().asFile
}

fun getCheckedOutGitCommitHash(takeFromHash: Int = 7): String {
    val gitFolder = file(rootProject.projectDir.absolutePath, ".git")
    require(gitFolder.exists()) { "Not a Git repository (missing .git directory)" }

    val headFile = File(gitFolder, "HEAD")
    val headContent = headFile.readText().trim()

    return when {
        ':' !in headContent -> headContent.take(takeFromHash)
        else -> {
            val refPath = headContent.substringAfter(": ").trim()
            File(gitFolder, refPath).let { refFile ->
                require(refFile.exists()) { "Git reference file not found: $refFile" }
                refFile.readText().trim().take(takeFromHash)
            }
        }
    }
}

version = getCheckedOutGitCommitHash()

kotlin {
    jvm()

    val buildNativeFilename =
        arrayOf("build", "native", System.getenv().getOrElse("__AFF_COMPOSE_NATIVE_BUILD_TARGET") { null }, "properties").filterNotNull()
            .joinToString(separator = ".")

    val buildNativeFile = File(rootProject.projectDir, buildNativeFilename);
    if (buildNativeFile.exists()) {
        val buildNativeProp = Properties().apply { buildNativeFile.inputStream().use { load(it) } }
        val autoDetection = !buildNativeProp.hasProperty("os.name")
                || !buildNativeProp.hasProperty("os.arch")

        val hostOs = if (autoDetection) System.getProperty("os.name") else buildNativeProp.getProperty("os.name")
        val isArm64 = (if (autoDetection) System.getProperty("os.arch") else buildNativeProp.getProperty("os.arch")) == "aarch64"

        val isMingwX64 = hostOs.startsWith("Windows")

        val nativeTarget = when {
            hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
            hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
            hostOs == "Linux" && isArm64 -> linuxArm64("native")
            hostOs == "Linux" && !isArm64 -> linuxX64("native")
            isMingwX64 -> mingwX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }

        nativeTarget.apply {
            binaries {
                sharedLib {
                    baseName = "affcompose_" +
                            "native_" +
                            "${hostOs.lowercase().replace("\\s".toRegex(), "")}_" +
                            "${if (isArm64) "arm64" else "x64"}_" +
                            "${project.version}"
                }
            }
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateKotlinGrammarSource)
            dependencies {
                implementation(libs.kotlin.antlr)
                implementation(libs.ktor.io)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.uuid)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.antlr)
                implementation(libs.ktor.io)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.uuid)
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["kotlin"])
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