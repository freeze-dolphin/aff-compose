@file:Suppress("unused", "UnusedReceiverParameter")

package com.tairitsu.compose

import com.benasher44.uuid.uuid4
import com.tairitsu.compose.Difficulty.Companion.timingGroupStack
import com.tairitsu.compose.Position.Companion.pos
import com.tairitsu.compose.filter.MirrorNoteFilter

// Composing
fun chartConfig(
    audioOffset: Long = 0,
    vararg extra: Pair<String, String>,
): Chart.Configuration {
    return Chart.Configuration(audioOffset, extra.map { Chart.Configuration.Item(it.first, it.second) }.toMutableSet())
}

fun chartConfig(
    audioOffset: Long = 0,
    vararg extra: Chart.Configuration.Item,
): Chart.Configuration {
    return Chart.Configuration(audioOffset, extra.toMutableSet())
}

fun chartConfig(
    audioOffset: Long = 0,
    timingPointDensityFactor: Double = 1.0,
    vararg extra: Chart.Configuration.Item,
): Chart.Configuration {
    return Chart.Configuration(audioOffset, extra.toMutableSet()).apply {
        if (timingPointDensityFactor != 1.0) addItem("TimingPointDensityFactor", timingPointDensityFactor.toAffFormat())
    }
}

fun compose(
    fxFilter: TimingGroupSpecialEffectFilter = TimingGroupSpecialEffectFilter.DEFAULT,
    closure: Difficulty.() -> Unit,
): Difficulty {
    val ctx = Difficulty(fxFilter = fxFilter)
    closure(ctx)
    return ctx
}

fun compose(
    chartConfig: Chart.Configuration,
    fxFilter: TimingGroupSpecialEffectFilter = TimingGroupSpecialEffectFilter.DEFAULT,
    closure: Difficulty.() -> Unit,
): Difficulty {
    val ctx = Difficulty(chartConfig, fxFilter)
    closure(ctx)
    return ctx
}

fun compose(
    diff: Difficulty,
    closure: Difficulty.() -> Unit,
): Difficulty {
    closure(diff)
    return diff
}

// Timing
fun <TOffset : Number, TBpm : Number, TBeat : Number> Difficulty.timing(
    offset: TOffset,
    bpm: TBpm,
    beats: TBeat,
): Timing {
    val ctx = this.currentTimingGroup
    val ret = Timing(offset.toLong(), bpm.toDouble(), beats.toDouble())
    ctx.addTiming(ret)
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
 * Create a [TimingGroup] by using Arcaea grammar
 */
fun Difficulty.timinggroup(vararg specialEffects: TimingGroup.SpecialEffect, closure: TimingGroup.() -> Unit): TimingGroup =
    timingGroup {
        specialEffects.forEach { this.addSpecialEffect(it) }
        closure.invoke(this)
    }

/**
 * Get the existing or creating a new [TimingGroup] of the [Difficulty]
 */
fun Difficulty.timingGroup(
    name: String = uuid4().toString(),
    fxFilter: TimingGroupSpecialEffectFilter = TimingGroupSpecialEffectFilter.DEFAULT,
    closure: TimingGroup.() -> Unit,
): TimingGroup {
    if (name == "main") {
        mainTimingGroup(closure)
        return chart.mainTiming
    }

    val newTimingGroup = chart.subTiming.getOrPut(name) { TimingGroup(name, fxFilter) }
    context.timingGroupStack.addLast(newTimingGroup)
    closure(newTimingGroup)
    context.timingGroupStack.removeLast()
    return newTimingGroup
}

fun Difficulty.timingGroup(
    vararg specialEffects: TimingGroup.SpecialEffect,
    fxFilter: TimingGroupSpecialEffectFilter = TimingGroupSpecialEffectFilter.DEFAULT,
    closure: TimingGroup.() -> Unit,
): TimingGroup {
    val tg = timingGroup(uuid4().toString(), fxFilter, closure)
    specialEffects.forEach {
        tg.addSpecialEffect(it)
    }
    return tg
}

fun Difficulty.timingGroup(
    name: String,
    vararg specialEffects: TimingGroup.SpecialEffect,
    fxFilter: TimingGroupSpecialEffectFilter = TimingGroupSpecialEffectFilter.DEFAULT,
    closure: TimingGroup.() -> Unit,
): TimingGroup {
    val tg = timingGroup(name, fxFilter, closure)
    specialEffects.forEach {
        tg.addSpecialEffect(it)
    }
    return tg
}

val Difficulty.noinput: TimingGroup.SpecialEffect get() = TimingGroup.SpecialEffect(TimingGroup.SpecialEffectType.NO_INPUT)
val Difficulty.fadingholds: TimingGroup.SpecialEffect get() = TimingGroup.SpecialEffect(TimingGroup.SpecialEffectType.FADING_HOLDS)

fun <TTimingGroupEffectParam : Number> Difficulty.anglex(extraParam: TTimingGroupEffectParam): TimingGroup.SpecialEffect =
    TimingGroup.SpecialEffect(TimingGroup.SpecialEffectType.ANGLEX, extraParam.toString())

fun <TTimingGroupEffectParam : Number> Difficulty.angley(extraParam: TTimingGroupEffectParam): TimingGroup.SpecialEffect =
    TimingGroup.SpecialEffect(TimingGroup.SpecialEffectType.ANGLEX, extraParam.toString())


// Scenecontrol

fun <TTime : Number> Difficulty.scenecontrol(
    time: TTime,
    type: ScenecontrolType,
    vararg params: Any,
): Scenecontrol {
    val ctx = this.currentTimingGroup
    val sc = Scenecontrol(time.toLong(), type) { params.map { if (it is Double) it.toAffFormat() else it.toString() } }
    return ctx.addScenecontrol(sc)
}

fun <TTime : Number> Difficulty.scenecontrol(
    time: TTime,
    type: ScenecontrolType,
    vararg params: Number,
): Scenecontrol {
    val ctx = this.currentTimingGroup
    val sc = Scenecontrol(time.toLong(), type) { params.map { if (it is Double) it.toAffFormat() else it.toString() } }
    return ctx.addScenecontrol(sc)
}

fun <TTime : Number> Difficulty.scenecontrol(
    time: TTime,
    type: ScenecontrolType,
    vararg params: String,
): Scenecontrol {
    val ctx = this.currentTimingGroup
    val sc = Scenecontrol(time.toLong(), type)
    return ctx.addScenecontrol(sc)
}

fun <TTime : Number> Difficulty.scenecontrol(
    time: TTime,
    type: ScenecontrolType,
): Scenecontrol {
    val ctx = this.currentTimingGroup
    val sc = Scenecontrol(time.toLong(), type)
    return ctx.addScenecontrol(sc)
}

// Built-in scenecontrols

val Difficulty.trackhide: ScenecontrolType get() = ScenecontrolType.TRACK_HIDE
val Difficulty.trackshow: ScenecontrolType get() = ScenecontrolType.TRACK_SHOW
val Difficulty.trackdisplay: ScenecontrolType get() = ScenecontrolType.TRACK_DISPLAY
val Difficulty.redline: ScenecontrolType get() = ScenecontrolType.RED_LINE
val Difficulty.arcahvdistort: ScenecontrolType get() = ScenecontrolType.ARCAHV_DISTORT
val Difficulty.arcahvdebris: ScenecontrolType get() = ScenecontrolType.ARCAHV_DEBRIS
val Difficulty.hidegroup: ScenecontrolType get() = ScenecontrolType.HIDE_GROUP
val Difficulty.enwidencamera: ScenecontrolType get() = ScenecontrolType.ENWIDEN_CAMERA
val Difficulty.enwidenlanes: ScenecontrolType get() = ScenecontrolType.ENWIDEN_LANES

// Normal Note

fun <TTime : Number> Difficulty.normalNote(time: TTime, column: Int): Note {
    val ctx = this.currentTimingGroup
    val note = NormalNote(time.toLong(), column)
    return ctx.addNormalNote(note)
}

fun <TTime : Number> Difficulty.normalNoteFloat(time: TTime, columnFloat: Double): Note {
    val ctx = this.currentTimingGroup
    val note = NormalNote(time.toLong(), Int.MIN_VALUE, columnFloat)
    return ctx.addNormalNote(note)
}

// Hold Note

/**
 * Create a [HoldNote] by using Arcaea grammar
 */
fun <TTime : Number, TEndTime : Number> Difficulty.hold(time: TTime, endTime: TEndTime, column: Int): Note =
    holdNote(time, endTime, column)

/**
 * Create a [HoldNote] by using Arcaea grammar
 */
fun <TTime : Number, TEndTime : Number> Difficulty.hold(time: TTime, endTime: TEndTime, column: Double): Note =
    holdNoteFloat(time, endTime, column)

fun <TTime : Number, TEndTime : Number> Difficulty.holdNote(time: TTime, endTime: TEndTime, column: Int): Note {
    val ctx = this.currentTimingGroup
    val note = HoldNote(time.toLong(), endTime.toLong(), column)
    return ctx.addHoldNote(note)
}

fun <TTime : Number, TEndTime : Number> Difficulty.holdNoteFloat(time: TTime, endTime: TEndTime, columnFloat: Double): Note {
    val ctx = this.currentTimingGroup
    val note = HoldNote(time.toLong(), endTime.toLong(), Int.MIN_VALUE, columnFloat)
    return ctx.addHoldNote(note)
}

// ArcNote

val Difficulty.s: ArcNote.EaseType get() = ArcNote.EaseType.S
val Difficulty.b: ArcNote.EaseType get() = ArcNote.EaseType.B
val Difficulty.si: ArcNote.EaseType get() = ArcNote.EaseType.SI
val Difficulty.so: ArcNote.EaseType get() = ArcNote.EaseType.SO
val Difficulty.siso: ArcNote.EaseType get() = ArcNote.EaseType.SISO
val Difficulty.sosi: ArcNote.EaseType get() = ArcNote.EaseType.SOSI
val Difficulty.sisi: ArcNote.EaseType get() = ArcNote.EaseType.SISI
val Difficulty.soso: ArcNote.EaseType get() = ArcNote.EaseType.SOSO

val Difficulty.blue: ArcNote.Color get() = ArcNote.Color.BLUE
val Difficulty.red: ArcNote.Color get() = ArcNote.Color.RED
val Difficulty.green: ArcNote.Color get() = ArcNote.Color.GREEN
val Difficulty.gray: ArcNote.Color get() = ArcNote.Color.GRAY
val Difficulty.grey: ArcNote.Color get() = ArcNote.Color.GREY

val Difficulty.none: String get() = "none"

val Difficulty.designant: ArcNote.NoteType get() = ArcNote.NoteType.DESIGNANT

fun <TTime : Number> MutableList<ArcTapNote>.arctap(time: TTime) {
    this.add(ArcTapNote(time.toLong()))
}

fun <TTime : Number, TLength : Number> MutableList<ArcTapNote>.arctap(time: TTime, length: TLength) {
    this.add(ArcTapNote(time.toLong(), length.toDouble()))
}

/**
 * Create an [ArcNote] by using Arcaea grammar
 */
fun <TTime : Number, TEndTime : Number, TCoordinate : Number> Difficulty.arc(
    time: TTime,
    endTime: TEndTime,
    x1: TCoordinate, x2: TCoordinate,
    easeType: ArcNote.EaseType,
    y1: TCoordinate, y2: TCoordinate,
    color: Int,
    hitSound: String,
    isTrace: Boolean,
    arcResolution: Double = 1.0,
    arcTapList: MutableList<ArcTapNote>.() -> Unit = { },
): Note {
    val ctx = this.currentTimingGroup
    val note = ArcNote(
        time.toLong(),
        endTime.toLong(),
        x1 pos y1,
        easeType,
        x2 pos y2,
        ArcNote.Color.fromColorId(color),
        if (isTrace) ArcNote.NoteType.TRACE else ArcNote.NoteType.ARC,
        arcResolution,
        arcTapList = mutableListOf<ArcTapNote>().apply { arcTapList(this) }
    )
    return ctx.addArcNote(note)
}

/**
 * Create an [ArcNote] by using Arcaea grammar
 */
fun <TTime : Number, TEndTime : Number, TCoordinate : Number> Difficulty.arc(
    time: TTime,
    endTime: TEndTime,
    x1: TCoordinate, x2: TCoordinate,
    easeType: ArcNote.EaseType,
    y1: TCoordinate, y2: TCoordinate,
    color: Int,
    hitSound: String,
    noteType: ArcNote.NoteType,
    arcResolution: Double = 1.0,
    arcTapList: MutableList<ArcTapNote>.() -> Unit = { },
): Note {
    val ctx = this.currentTimingGroup
    val note = ArcNote(
        time.toLong(),
        endTime.toLong(),
        x1 pos y1,
        easeType,
        x2 pos y2,
        ArcNote.Color.fromColorId(color),
        noteType,
        arcResolution,
        arcTapList = mutableListOf<ArcTapNote>().apply { arcTapList(this) }
    )
    return ctx.addArcNote(note)
}

/**
 * Create an [ArcNote]
 */
fun <TTime : Number, TEndTime : Number> Difficulty.arcNote(
    time: TTime,
    endTime: TEndTime,
    startPosition: Position,
    easeType: ArcNote.EaseType,
    endPosition: Position,
    color: ArcNote.Color,
    arcResolution: Double = 1.0,
): Note {
    val ctx = this.currentTimingGroup
    val note = ArcNote(
        time.toLong(),
        endTime.toLong(),
        startPosition,
        easeType,
        endPosition,
        color,
        ArcNote.NoteType.ARC,
        arcResolution
    )
    return ctx.addArcNote(note)
}

/**
 * Create [ArcNote] as a trace
 */
fun <TTime : Number, TEndTime : Number> Difficulty.trace(
    time: TTime,
    endTime: TEndTime,
    startPosition: Position,
    easeType: ArcNote.EaseType,
    endPosition: Position,
    color: ArcNote.Color = ArcNote.Color.BLUE,
    hitSound: String = "none",
    arcResolution: Double = 1.0,
    arcTapList: MutableList<ArcTapNote>.() -> Unit = { },
): Note {
    val ctx = this.currentTimingGroup
    val note = ArcNote(
        time.toLong(),
        endTime.toLong(),
        startPosition,
        easeType,
        endPosition,
        color,
        ArcNote.NoteType.TRACE,
        arcResolution,
        hitSound,
        arcTapList = mutableListOf<ArcTapNote>().apply { arcTapList(this) }
    )
    return ctx.addArcNote(note)
}

/**
 * Create [ArcNote] as designant
 */
fun <TTime : Number, TEndTime : Number> Difficulty.traceDesignant(
    time: TTime,
    endTime: TEndTime,
    startPosition: Position,
    easeType: ArcNote.EaseType,
    endPosition: Position,
    hitSound: String = "none",
    arcResolution: Double = 1.0,
    arcTapList: MutableList<ArcTapNote>.() -> Unit = { },
): Note {
    val ctx = this.currentTimingGroup
    val note = ArcNote(
        time.toLong(),
        endTime.toLong(),
        startPosition,
        easeType,
        endPosition,
        ArcNote.Color.BLUE,
        ArcNote.NoteType.DESIGNANT,
        arcResolution,
        hitSound,
        arcTapList = mutableListOf<ArcTapNote>().apply { arcTapList(this) }
    )
    return ctx.addArcNote(note)
}

// Camera

val Difficulty.l: Camera.EaseType
    get() = Camera.EaseType.L
val Difficulty.qi: Camera.EaseType
    get() = Camera.EaseType.QI
val Difficulty.qo: Camera.EaseType
    get() = Camera.EaseType.QO
val Difficulty.reset: Camera.EaseType
    get() = Camera.EaseType.RESET

fun <TTime : Number, TPixel : Number, TDegree : Number> Difficulty.camera(
    time: TTime,
    xOff: TPixel,
    yOff: TPixel,
    zOff: TPixel,
    xozAng: TDegree,
    yozAng: TDegree,
    xoyAng: TDegree,
    ease: Camera.EaseType,
    duration: TTime,
): Camera {
    val ctx = this.currentTimingGroup
    val camera = Camera(
        time.toLong(),
        xOff.toDouble(), yOff.toDouble(), zOff.toDouble(),
        xozAng.toDouble(), yozAng.toDouble(), xoyAng.toDouble(), ease, duration.toLong()
    )
    return ctx.addCamera(camera)
}

fun <TTime : Number, TPixel : Number, TDegree : Number> Difficulty.camera(
    time: TTime,
    xOff: TPixel,
    yOff: TPixel,
    zOff: TPixel,
    xozAng: TDegree,
    yozAng: TDegree,
    xoyAng: TDegree,
    ease: ArcNote.EaseType,
    duration: TTime,
): Camera {
    require(ease == ArcNote.EaseType.S)

    val ctx = this.currentTimingGroup
    val camera = Camera(
        time.toLong(),
        xOff.toDouble(), yOff.toDouble(), zOff.toDouble(),
        xozAng.toDouble(), yozAng.toDouble(), xoyAng.toDouble(), Camera.EaseType.S, duration.toLong()
    )
    return ctx.addCamera(camera)
}

// filter

fun Difficulty.noteFilter(noteFilter: NoteFilter, closure: (Difficulty.() -> Unit)) {
    addNoteFilter(noteFilter)
    closure()
    popNoteFilter()
}

fun Difficulty.mirror(closure: (Difficulty.() -> Unit)) = noteFilter(MirrorNoteFilter, closure)
