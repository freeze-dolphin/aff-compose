package com.tairitsu.compose.arcaea

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

fun MapSet.writeToFolder(outputPath: Path) {
    writeToFolder(outputPath.toFile())
}

fun MapSet.writeToFolder(outputPath: File) {
    Files.createDirectories(outputPath.toPath())
    writeToFile(outputPath.path)
}

fun Difficulty.printToConsole(containHeaders: Boolean = true, containBaseTiming: Boolean = true) {
    var headerOver = false
    var baseTimingOver = false
    this.chart.serialize().split("\r\n").forEach {
        if (it.startsWith("-")) {
            headerOver = true
            if (containHeaders) {
                println("-")
            }
            return@forEach
        }

        if (!headerOver && containHeaders) {
            println(it)
            return@forEach
        }

        if (it.startsWith("timing") && !baseTimingOver) {
            baseTimingOver = true
            if (containBaseTiming) {
                println(it)
            }
            return@forEach
        }

        if (headerOver) println(it)

    }
}
