package com.tairitsu.compose.arcaea

import com.benasher44.uuid.uuid4
import com.tairitsu.compose.arcaea.Difficulty.Companion.timingGroupStack
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Creating a new instance of [MapSet]
 */
fun mapSet(closure: MapSet.() -> Unit): MapSet {
    val mapSet = MapSet()
    mapSet.closure()
    return mapSet
}

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    encodeDefaults = true
    explicitNulls = false
}

fun MapSet.writeToFile(outputDirectory: String) {
    val json = Json { prettyPrint = true }

    val songDataFile = File(outputDirectory, "song_data.json")
    songDataFile.writeText(json.encodeToString(MapSet.serializer(), this))

    this.difficulties.forEach { difficulty ->
        val difficultyFile = File(outputDirectory, "${difficulty.ratingClass.rating}.aff")
        difficultyFile.writeText(difficulty.chart.serialize())
    }

    val lineSeparator = System.lineSeparator()

    val songConfigFile = File(outputDirectory, "songconfig.txt").bufferedWriter()
    songConfigFile.use { writer ->
        writer.write("id=${this.id}$lineSeparator")
        writer.write("title=${this.titleLocalized.en}$lineSeparator")
        writer.write("artist=${this.artist}$lineSeparator")
        writer.write(
            "designer=${
                this.difficulties.map { it.chartDesigner }.toSet().joinToString(separator = ",")
            }$lineSeparator"
        )
        writer.write("bpm_disp=${this.bpm}$lineSeparator")
        writer.write("bpm_base=${this.bpmBase}$lineSeparator")
        writer.write("side=${this.side.id}$lineSeparator")
        writer.write(
            "diff=${
                this.difficulties.past?.rating ?: 0
            }-${
                this.difficulties.present?.rating ?: 0
            }-${
                this.difficulties.future?.rating ?: 0
            }-${
                this.difficulties.beyond?.rating ?: 0
            }$lineSeparator"
        )
    }
}


/**
 * Get the existing or creating a new past [Difficulty] of the [MapSet]
 */
fun Difficulties.past(offset: Long = 0, closure: Difficulty.() -> Unit) {
    val difficulty = (this.past ?: Difficulty()).apply {
        chart.configuration.tuneOffset(offset)
    }
    difficulty.closure()
    this.past = difficulty
}

/**
 * Get the existing or creating a new present [Difficulty] of the [MapSet]
 */
fun Difficulties.present(offset: Long = 0, closure: Difficulty.() -> Unit) {
    val difficulty = (this.present ?: Difficulty()).apply {
        chart.configuration.tuneOffset(offset)
    }
    difficulty.closure()
    this.present = difficulty
}

/**
 * Get the existing or creating a new future [Difficulty] of the [MapSet]
 */
fun Difficulties.future(offset: Long = 0, closure: Difficulty.() -> Unit) {
    val difficulty = (this.future ?: Difficulty()).apply {
        chart.configuration.tuneOffset(offset)
    }
    difficulty.closure()
    this.future = difficulty
}

/**
 * Get the existing or creating a new past [Difficulty] of the [MapSet]
 */
fun Difficulties.beyond(offset: Long = 0, closure: Difficulty.() -> Unit) {
    val difficulty = (this.beyond ?: Difficulty()).apply {
        chart.configuration.tuneOffset(offset)
    }
    difficulty.closure()
    this.beyond = difficulty
}

/**
 * Get the existing or creating a new past [Difficulty] of the [MapSet]
 */
fun Difficulties.eternal(offset: Long = 0, closure: Difficulty.() -> Unit) {
    val difficulty = (this.eternal ?: Difficulty()).apply {
        chart.configuration.tuneOffset(offset)
    }
    difficulty.closure()
    this.eternal = difficulty
}

// Timing
fun <TOffset : Number, TBpm : Number, TBeat : Number> Difficulty.timing(
    offset: TOffset,
    bpm: TBpm,
    beats: TBeat,
): Timing {
    val ctx = this.currentTimingGroup
    val ret = Timing(offset.toLong(), bpm.toDouble(), beats.toDouble())
    ctx.timing.add(ret)
    return ret
}

// Timing group

/**
 * Get the existing or creating a new [TimingGroup] of the [Difficulty]
 */
fun Difficulty.timingGroup(name: String = uuid4().toString(), closure: TimingGroup.() -> Unit): TimingGroup {
    val newTimingGroup = chart.subTiming.getOrPut(name) { TimingGroup(name) }
    context.timingGroupStack.addLast(newTimingGroup)
    closure(newTimingGroup)
    context.timingGroupStack.removeLast()
    return newTimingGroup
}

// Scenecontrol

fun <TTime : Number> Difficulty.scenecontrol(
    time: TTime,
    type: ScenecontrolType,
    param1: Double,
    param2: Int,
) {
    val ctx = this.currentTimingGroup
    val sc = Scenecontrol(time.toLong(), type, param1, param2)
    return ctx.addScenecontrol(sc)
}

fun <TTime : Number> Difficulty.scenecontrol(
    time: TTime,
    type: ScenecontrolType,
    param2: Int,
) {
    val ctx = this.currentTimingGroup
    val sc = Scenecontrol(time.toLong(), type, null, param2)
    return ctx.addScenecontrol(sc)
}

fun <TTime : Number> Difficulty.scenecontrol(
    time: TTime,
    type: ScenecontrolType,
    param1: Double,
) {
    val ctx = this.currentTimingGroup
    val sc = Scenecontrol(time.toLong(), type, param1, null)
    return ctx.addScenecontrol(sc)
}

fun <TTime : Number> Difficulty.scenecontrol(
    time: TTime,
    type: ScenecontrolType,
) {
    val ctx = this.currentTimingGroup
    val sc = Scenecontrol(time.toLong(), type, null, null)
    return ctx.addScenecontrol(sc)
}

// Normal Note

fun <TTime : Number> Difficulty.normalNote(time: TTime, column: Int): Note {
    val ctx = this.currentTimingGroup
    val note = NormalNote(time.toLong(), column)
    return ctx.addNormalNote(note)
}

// Hold Note

fun <TTime : Number, TEndTime : Number> Difficulty.holdNote(time: TTime, endTime: TEndTime, column: Int): Note {
    val ctx = this.currentTimingGroup
    val note = HoldNote(time.toLong(), endTime.toLong(), column)
    return ctx.addHoldNote(note)
}

// ArcNote

val Difficulty.s: ArcNote.CurveType
    get() = ArcNote.CurveType.S
val Difficulty.b: ArcNote.CurveType
    get() = ArcNote.CurveType.B
val Difficulty.si: ArcNote.CurveType
    get() = ArcNote.CurveType.SI
val Difficulty.so: ArcNote.CurveType
    get() = ArcNote.CurveType.SO
val Difficulty.siso: ArcNote.CurveType
    get() = ArcNote.CurveType.SISO
val Difficulty.sosi: ArcNote.CurveType
    get() = ArcNote.CurveType.SOSI
val Difficulty.sisi: ArcNote.CurveType
    get() = ArcNote.CurveType.SISI
val Difficulty.soso: ArcNote.CurveType
    get() = ArcNote.CurveType.SOSO

fun <TTime : Number, TEndTime : Number> Difficulty.arcNote(
    time: TTime,
    endTime: TEndTime,
    startPosition: Position,
    curveType: ArcNote.CurveType,
    endPosition: Position,
    color: ArcNote.Color? = null,
    isGuidingLine: Boolean = color == null,
    arcTapClosure: (ArcNote.ArcTapList.() -> Unit) = { },
): Note {
    val ctx = this.currentTimingGroup
    val note = ArcNote(
        time.toLong(),
        endTime.toLong(),
        startPosition,
        curveType,
        endPosition,
        color ?: ArcNote.Color.BLUE,
        isGuidingLine,
        arcTapClosure
    )
    return ctx.addArcNote(note)
}

fun <TTime : Number, TEndTime : Number, TStartPositionX : Number, TStartPositionY : Number, TEndPositionX : Number, TEndPositionY : Number> Difficulty.arcNote(
    time: TTime,
    endTime: TEndTime,
    startPosition: Pair<TStartPositionX, TStartPositionY>,
    curveType: ArcNote.CurveType,
    endPosition: Pair<TEndPositionX, TEndPositionY>,
    color: ArcNote.Color? = null,
    isGuidingLine: Boolean = color == null,
    arcTapClosure: (ArcNote.ArcTapList.() -> Unit) = { },
): Note {
    val ctx = this.currentTimingGroup
    val note = ArcNote(
        time.toLong(),
        endTime.toLong(),
        startPosition.first.toDouble() to startPosition.second.toDouble(),
        curveType,
        endPosition.first.toDouble() to endPosition.second.toDouble(),
        color ?: ArcNote.Color.BLUE,
        isGuidingLine,
        arcTapClosure
    )
    return ctx.addArcNote(note)
}

// Var-len Arctap

fun <TTime : Number, TStartPositionX : Number, TStartPositionY : Number, TEndPositionX : Number, TEndPositionY : Number> Difficulty.vlArctap(
    time: TTime, startPosition: Pair<TStartPositionX, TStartPositionY>, endPosition: Pair<TEndPositionX, TEndPositionY>,
) {
    arcNote(time, time, startPosition, ArcNote.CurveType.S, endPosition, ArcNote.Color(3), false) { }
}

fun <TTime : Number> Difficulty.vlArctapWithRadius(
    time: TTime, centerPosition: Pair<Double, Double>, radius: Double,
) {
    vlArctap(
        time,
        centerPosition.first - radius to centerPosition.second,
        centerPosition.first + radius to centerPosition.second
    )
}

fun <TTime : Number> Difficulty.vlArctapWithDistance(
    time: TTime, centerPosition: Pair<Double, Double>, distance: Double,
) {
    vlArctapWithRadius(
        time,
        centerPosition,
        distance / 2
    )
}