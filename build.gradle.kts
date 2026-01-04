import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import java.util.*

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.kotlin.antlr)

    alias(libs.plugins.kotlinx.microbenchmark)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "com.tairitsu"

val generateKotlinGrammarSource = tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    description = "Generate kotlin source from g4 files"
    group = "other"

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
    val gitFolder = File(rootProject.projectDir.absolutePath).resolve(".git")
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
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
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
            val main by compilations.getting
            val interop by main.cinterops.creating {
                definitionFile.set(project.file("src/nativeInterop/cinterop/interop.def"))
            }

            sharedLib {
                val prefix = if (isMingwX64) "lib" else ""
                baseName = prefix + "affcompose"
            }
        }
    }

    jvm()

    sourceSets {
        commonMain {
            kotlin.srcDir(generateKotlinGrammarSource)
            dependencies {
                implementation(libs.kotlin.antlr)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.cryptography)
                implementation(libs.cryptography.provider)
                implementation(libs.uuid)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.antlr)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.uuid)
            }
        }

        val commonBenchmark by creating {
            dependencies {
                implementation(libs.kotlinx.microbenchmark)
            }
        }
    }

    val cbm = sourceSets.getByName("commonBenchmark")

    targets.matching { it.name != "metadata" }.all {
        compilations.create("benchmark") {
            associateWith(this@all.compilations.getByName("main"))
            defaultSourceSet {
                dependencies {
                    implementation(libs.kotlinx.microbenchmark)
                }
                dependsOn(cbm)
            }
        }

        benchmark.targets.register("${name}Benchmark")
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
