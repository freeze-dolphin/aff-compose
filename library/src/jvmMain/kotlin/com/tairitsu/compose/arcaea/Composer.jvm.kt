package com.tairitsu.compose.arcaea

import kotlinx.io.asSink
import kotlinx.io.buffered
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path

fun MapSet.writeToFolder(outputPath: Path) {
    writeToFolder(outputPath.toFile())
}

fun MapSet.writeToFolder(outputPath: File) {
    Files.createDirectories(outputPath.toPath())
    writeToOutput { fileName ->
        FileOutputStream(File(outputPath, fileName)).asSink().buffered()
    }
}

fun Difficulty.generateString(): String {
    return this.chart.serializeForArcaea()
}