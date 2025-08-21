package com.tairitsu.compose.arcaea

import com.benasher44.uuid.uuid4
import com.tairitsu.compose.arcaea.Difficulty.Companion.timingGroupStack
import io.ktor.utils.io.core.*
import kotlinx.io.Sink
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

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

@OptIn(InternalSerializationApi::class)
fun MapSet.writeToOutput(outputFactory: (String) -> Sink) {
    val songDataOutput = outputFactory("song_data.json")
    val songData = json.encodeToString(MapSet::class.serializer(), this)
    songDataOutput.writeFully(songData.toByteArray())
    songDataOutput.close()

    val difficulties = this.difficulties
    for (difficulty in difficulties) {
        val difficultyOutput = outputFactory("${difficulty.ratingClass.rating}.aff")
        val difficultyData = difficulty.chart.serializeForArcaea()
        difficultyOutput.writeFully(difficultyData.toByteArray())
        difficultyOutput.close()
    }

    val songConfig = outputFactory("songconfig.txt")
    songConfig.writeFully("id=${this.id}\r\n".toByteArray())
    songConfig.writeFully("title=${this.titleLocalized.en}\r\n".toByteArray())
    songConfig.writeFully("artist=${this.artist}\r\n".toByteArray())
    songConfig.writeFully(
        "designer=${
            this.difficulties.map { it.chartDesigner }.toSet().joinToString(separator = ",")
        }\r\n".toByteArray()
    )
    songConfig.writeFully("bpm_disp=${this.bpm}\r\n".toByteArray())
    songConfig.writeFully("bpm_base=${this.bpmBase}\r\n".toByteArray())
    songConfig.writeFully("side=${this.side.id}\r\n".toByteArray())
    songConfig.writeFully(
        "diff=${
            this.difficulties.past?.rating ?: 0
        }-${
            this.difficulties.present?.rating ?: 0
        }-${
            this.difficulties.future?.rating ?: 0
        }-${
            this.difficulties.beyond?.rating ?: 0
        }-${
            this.difficulties.eternal?.rating ?: 0
        }\r\n".toByteArray()
    )
    songConfig.close()
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
 * Get the main [TimingGroup] of the [Difficulty]
 */
fun Difficulty.mainTimingGroup(closure: TimingGroup.() -> Unit) {
    closure.invoke(chart.mainTiming)
}

/**
 * Get the existing or creating a new [TimingGroup] of the [Difficulty]
 */
fun Difficulty.timingGroup(name: String = uuid4().toString(), closure: TimingGroup.() -> Unit): TimingGroup {
    if (name == "main") {
        mainTimingGroup(closure)
        return chart.mainTiming
    }

    val newTimingGroup = chart.subTiming.getOrPut(name) { TimingGroup(name) }
    context.timingGroupStack.addLast(newTimingGroup)
    closure(newTimingGroup)
    context.timingGroupStack.removeLast()
    return newTimingGroup
}

fun Difficulty.timingGroup(vararg specialEffects: TimingGroupSpecialEffect, closure: TimingGroup.() -> Unit): TimingGroup {
    val tg = timingGroup(uuid4().toString(), closure)
    specialEffects.forEach {
        tg.addRawSpecialEffect(it)
    }
    return tg
}

fun Difficulty.timingGroup(name: String, vararg specialEffects: TimingGroupSpecialEffect, closure: TimingGroup.() -> Unit): TimingGroup {
    val tg = timingGroup(name, closure)
    specialEffects.forEach {
        tg.addRawSpecialEffect(it)
    }
    return tg
}

val Difficulty.noinput: TimingGroupSpecialEffect
    get() = TimingGroupSpecialEffect(TimingGroupSpecialEffectType.NO_INPUT)
val Difficulty.fadingholds: TimingGroupSpecialEffect
    get() = TimingGroupSpecialEffect(TimingGroupSpecialEffectType.FADING_HOLDS)

fun Difficulty.anglex(extraParam: Int): TimingGroupSpecialEffect = TimingGroupSpecialEffect(TimingGroupSpecialEffectType.ANGLEX, extraParam)
fun Difficulty.angley(extraParam: Int): TimingGroupSpecialEffect = TimingGroupSpecialEffect(TimingGroupSpecialEffectType.ANGLEX, extraParam)


// Scenecontrol

internal fun <TTime : Number> Difficulty.rawScenecontrol(
    time: TTime,
    type: ScenecontrolType,
    param1: Double?,
    param2: Int?,
) {
    val ctx = this.currentTimingGroup
    val sc = Scenecontrol(time.toLong(), type, param1, param2)
    return ctx.addScenecontrol(sc)
}

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

fun <TTime : Number> Difficulty.trackhide(time: TTime) = scenecontrol(time.toLong(), ScenecontrolType.TRACK_HIDE)
fun <TTime : Number> Difficulty.trackshow(time: TTime) = scenecontrol(time.toLong(), ScenecontrolType.TRACK_SHOW)

/**
 * @param timeInSec 轨道从当前 alpha 变换到目标 alpha(param2) 所要花费的时间，数字为小数，单位为秒，填 0.00 等价于填 1.00
 * @param alpha 轨道需要变换到的目标 alpha 值，可以填非负整数；<255 时有黑色背景特效，否则没有；=0 为轨道完全透明，=255 为轨道不透明，>=256 时透明度溢出（可看作透明度对 256 取余数计算）。
 * @sample trackdisplay(20480,6.00,0);
 */
fun <TTime : Number> Difficulty.trackdisplay(time: TTime, timeInSec: Double, alpha: Int) =
    scenecontrol(time.toLong(), ScenecontrolType.TRACK_DISPLAY, timeInSec, alpha)

/**
 * @param timeInSec 红线存在的时间，数字为小数，单位为秒
 * @sample redline(40960,1.88);
 */
fun <TTime : Number> Difficulty.redline(time: TTime, timeInSec: Double) =
    scenecontrol(time.toLong(), ScenecontrolType.RED_LINE, timeInSec)

/**
 * @param timeInSec 从当前 alpha 变换为指定 alpha 的持续时间，数字为小数，单位为秒
 * @param alpha 目标 alpha 值
 * @sample arcahvdebris(1000,1.00,128);
 */
fun <TTime : Number> Difficulty.arcahvdistort(time: TTime, timeInSec: Double, alpha: Int) =
    scenecontrol(time.toLong(), ScenecontrolType.ARCAHV_DISTORT, timeInSec, alpha)

/**
 * @param timeInSec 从当前 alpha 变换为指定 alpha 的持续时间，数字为小数，单位为秒
 * @param alpha 目标 alpha 值（0~255）
 * @sample arcahvdebris(1000,1.00,128);
 */
fun <TTime : Number> Difficulty.arcahvdebris(time: TTime, timeInSec: Double, alpha: Int) =
    scenecontrol(time.toLong(), ScenecontrolType.ARCAHV_DEBRIS, timeInSec, alpha)

/**
 * @param isHidden 隐藏或显示该时间组的 note（1/0）
 * @sample hidegroup(81920,true);
 */
fun <TTime : Number> Difficulty.hidegroup(time: TTime, isHidden: Boolean) =
    scenecontrol(time.toLong(), ScenecontrolType.HIDE_GROUP, if (isHidden) 1 else 0)

/**
 * @param durationInMs 持续时长（ms）
 * @param isActive 淡入或淡出该事件展示的效果（1/0）
 * @sample enwidencamera(1000,1000.00,true);
 */
fun <TTime : Number> Difficulty.enwidencamera(time: TTime, durationInMs: Double, isActive: Boolean) =
    scenecontrol(time.toLong(), ScenecontrolType.ENWIDEN_CAMERA, durationInMs, if (isActive) 1 else 0)

/**
 * @param durationInMs 持续时长（ms）
 * @param isActive 淡入或淡出该事件展示的效果（1/0）
 * @sample enwidenlanes(1000,1000.00,1);
 */
fun <TTime : Number> Difficulty.enwidenlanes(time: TTime, durationInMs: Double, isActive: Boolean) =
    scenecontrol(time.toLong(), ScenecontrolType.ENWIDEN_LANES, durationInMs, if (isActive) 1 else 0)

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

val Difficulty.blue: ArcNote.Color
    get() = ArcNote.Color.BLUE
val Difficulty.red: ArcNote.Color
    get() = ArcNote.Color.RED
val Difficulty.green: ArcNote.Color
    get() = ArcNote.Color.GREEN

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
        isGuidingLine.toString(),
        arcTapClosure
    )
    return ctx.addArcNote(note)
}

fun <TTime : Number, TEndTime : Number> Difficulty.arcNoteLegacy(
    time: TTime,
    endTime: TEndTime,
    x1: Double, x2: Double,
    curveType: ArcNote.CurveType,
    y1: Double, y2: Double,
    color: ArcNote.Color? = null,
    isGuidingLine: Boolean = color == null,
    isDesignant: Boolean? = null,
    arcTapClosure: (ArcNote.ArcTapList.() -> Unit) = { },
): Note {
    val ctx = this.currentTimingGroup
    val note = ArcNote(
        time.toLong(),
        endTime.toLong(),
        x1 pos y1,
        curveType,
        x2 pos y2,
        color ?: ArcNote.Color.BLUE,
        if (isDesignant == true) "designant" else isGuidingLine.toString(),
        arcTapClosure
    )
    return ctx.addArcNote(note)
}

fun <TTime : Number, TEndTime : Number> Difficulty.arcNoteLegacy(
    time: TTime,
    endTime: TEndTime,
    x1: Double, x2: Double,
    curveType: ArcNote.CurveType,
    y1: Double, y2: Double,
    color: ArcNote.Color? = null,
    arcType: String,
    arcTapClosure: (ArcNote.ArcTapList.() -> Unit) = { },
): Note {
    val ctx = this.currentTimingGroup
    val note = ArcNote(
        time.toLong(),
        endTime.toLong(),
        x1 pos y1,
        curveType,
        x2 pos y2,
        color ?: ArcNote.Color.BLUE,
        arcType,
        arcTapClosure
    )
    return ctx.addArcNote(note)
}

// Var-len Arctap

fun <TTime : Number> Difficulty.vlArctap(
    time: TTime,
    startPosition: Position,
    endPosition: Position,
) {
    arcNote(
        time,
        time,
        startPosition,
        ArcNote.CurveType.S,
        endPosition,
        ArcNote.Color(3),
        false
    ) { }
}

fun <TTime : Number> Difficulty.vlArctapWithRadius(
    time: TTime,
    centerPosition: Position,
    radius: Double,
) {
    vlArctap(
        time,
        centerPosition.x - radius pos centerPosition.y,
        centerPosition.x + radius pos centerPosition.y
    )
}

fun <TTime : Number> Difficulty.vlArctapWithDistance(
    time: TTime,
    centerPosition: Position,
    distance: Double,
) {
    vlArctapWithRadius(
        time,
        centerPosition,
        distance / 2
    )
}

// Camera

val Difficulty.l: Camera.CameraEaseType
    get() = Camera.CameraEaseType.L
val Difficulty.qi: Camera.CameraEaseType
    get() = Camera.CameraEaseType.QI
val Difficulty.qo: Camera.CameraEaseType
    get() = Camera.CameraEaseType.QO
val Difficulty.reset: Camera.CameraEaseType
    get() = Camera.CameraEaseType.RESET

fun <TTime : Number> Difficulty.camera(
    time: TTime,
    xOff: Double,
    yOff: Double,
    zOff: Double,
    xozAng: Double,
    yozAng: Double,
    xoyAng: Double,
    ease: Camera.CameraEaseType,
    duration: TTime,
) {
    val ctx = this.currentTimingGroup
    val camera = Camera(
        time.toLong(),
        xOff, yOff, zOff,
        xozAng, yozAng, xoyAng, ease, duration.toLong()
    )
    return ctx.addCamera(camera)
}

fun <TTime : Number> Difficulty.camera(
    time: TTime,
    xOff: Double,
    yOff: Double,
    zOff: Double,
    xozAng: Double,
    yozAng: Double,
    xoyAng: Double,
    ease: ArcNote.CurveType,
    duration: TTime,
) {
    val ctx = this.currentTimingGroup
    val camera = Camera(
        time.toLong(),
        xOff, yOff, zOff,
        xozAng, yozAng, xoyAng, ease, duration.toLong()
    )
    return ctx.addCamera(camera)
}