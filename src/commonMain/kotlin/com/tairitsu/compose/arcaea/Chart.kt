package com.tairitsu.compose.arcaea

import com.tairitsu.compose.arcaea.parser.*
import com.tairitsu.compose.arcaea.serializer.*
import io.sn.aetherium.utils.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.abs
import kotlin.math.roundToInt

const val TIMING_GROUP_SERIALIZATION_PADDING = 4

@Serializable
class Chart {

    val configuration: ChartConfiguration = ChartConfiguration(0, mutableSetOf())
    val mainTiming: TimingGroup = TimingGroup("main")
    val subTiming: MutableMap<String, TimingGroup> = mutableMapOf()

    @Deprecated("please specify the format", ReplaceWith("com.tairitsu.compose.arcaea.Chart.serializeForArcaea()"))
    fun serialize() = serializeForArcaea()

    private fun StringBuilder.appendTimingGroup(tgs: List<TimingGroup>) {
        for (timing in tgs) {
            append(
                "timinggroup(${
                    timing.specialEffects.joinToString(",") {
                        it.serializeForArcCreate()
                    }
                }){\r\n")
            append(timing.serializeForArcCreate(padding = TIMING_GROUP_SERIALIZATION_PADDING, this@Chart))
            append("};\r\n")
        }
    }

    fun serializeForArcaea(): String {
        val sb = StringBuilder()

        sb.append("AudioOffset:${configuration.audioOffset}\r\n")
        configuration.extra.forEach {
            sb.append("${it.name}:${it.value}\r\n")
        }
        sb.append("-\r\n")

        sb.append(mainTiming.serializeForArcaea(0, this))
        for (timing in subTiming.values) {
            sb.append(
                "timinggroup(${
                    timing.specialEffects.joinToString("_") {
                        it.serializeForArcaea()
                    }
                }){\r\n")
            sb.append(timing.serializeForArcaea(padding = TIMING_GROUP_SERIALIZATION_PADDING, this))
            sb.append("};\r\n")
        }

        return sb.toString().trim { it <= ' ' }
    }

    fun serializeForArcCreate(): String {
        val sb = StringBuilder()

        sb.append("AudioOffset:${configuration.audioOffset}\r\n")
        configuration.extra.forEach {
            sb.append("${it.name}:${it.value}\r\n")
        }
        sb.append("-\r\n")

        sb.append(mainTiming.serializeForArcCreate(0, this))

        val nonArcResolutionTimings = mutableListOf<TimingGroup>()
        val arcResolutionTimings = mutableListOf<TimingGroup>()

        for (timingEntry in subTiming) {
            if (timingEntry.key.contains("_arcResolution")) {
                arcResolutionTimings.add(timingEntry.value)
            } else {
                nonArcResolutionTimings.add(timingEntry.value)
            }
        }

        sb.appendTimingGroup(nonArcResolutionTimings)

        // arcResolution serialization is seperated to here
        sb.appendTimingGroup(arcResolutionTimings)

        return sb.toString().trim { it <= ' ' }
    }

    companion object {
        @Deprecated("use ANTLRChartParser#fromAff instead", ReplaceWith("com.tairitsu.compose.arcaea.Chart.fromAff(aff)"))
        fun fromAffRaw(aff: String): Chart = RawChartParser.fromAff(aff)

        fun fromAff(aff: String): Chart = ANTLRArcaeaChartParser(aff).parse()
        fun fromAcf(acf: String): Pair<Chart, ArcCreateChartParseReport> = ANTLRArcCreateChartParser(acf).parse()
    }

}

@Serializable
data class ChartConfiguration(var audioOffset: Long, val extra: MutableSet<ConfigurationItem>) {
    @Serializable
    data class ConfigurationItem(val name: String, val value: String)

    fun tuneOffset(newOffset: Long) {
        audioOffset = newOffset
    }

    fun addItem(name: String, value: String) {
        extra.add(ConfigurationItem(name, value))
    }

    fun addItem(name: String, value: Number) {
        extra.add(ConfigurationItem(name, value.toString()))
    }

    fun syncWith(other: ChartConfiguration) {
        audioOffset = other.audioOffset
        extra.addAll(other.extra)
    }

}

interface ChartObject

data class SerializationContext(
    val chart: Chart,
    val timingGroup: TimingGroup,
)

interface TimedObject : ChartObject {
    val time: Long

    fun serializeTimedObjDefault(): String?
    fun serializeTimedObjForArcaea(ctx: SerializationContext) = serializeTimedObjDefault()
    fun serializeTimedObjForArcCreate(ctx: SerializationContext) = serializeTimedObjDefault()

    object Comparator : kotlin.Comparator<TimedObject> {
        override fun compare(a: TimedObject, b: TimedObject): Int {
            // sort by time
            val timeCmp = a.time.compareTo(b.time)
            if (timeCmp != 0) return timeCmp

            if (a is Camera && b !is Camera) {
                return 1
            } else if (a !is Camera && b is Camera) {
                return -1
            }

            if ((a is Timing && b is Timing) || (a is Scenecontrol && b is Scenecontrol)) {
                return 0
            }
            if (a is Note && b is Note) {
                return Note.Comparator.compare(a, b)
            }
            if (a is Timing) {
                return -1
            }
            if (b is Timing) {
                return 1
            }
            if (a is Scenecontrol) {
                return -1
            }
            if (b is Scenecontrol) {
                return 1
            }
            return 0
        }
    }
}

internal val Double.affFormat: String
    get() {
        val rounded = (this * 100.0).roundToInt() / 100.0
        val str = rounded.toString()
        return if (str.contains('.')) {
            val (whole, decimal) = str.split('.')
            "$whole.${decimal.take(2).padEnd(2, '0')}"
        } else {
            "$str.00"
        }
    }

@Serializable
class Timing(val offset: Long, val bpm: Double, val beats: Double) : TimedObject {
    override val time: Long
        get() = offset

    override fun serializeTimedObjDefault(): String {
        return "timing($offset,${bpm.affFormat},${beats.affFormat});"
    }
}

@Suppress("unused")
enum class ScenecontrolType(val id: String, val paramReq1: Boolean, val paramReq2: Boolean) {
    // @formatter:off
    TRACK_HIDE("trackhide", false, false),
    TRACK_SHOW("trackshow", false, false),
    TRACK_DISPLAY("trackdisplay", true, true),
    RED_LINE("redline", true, false),
    ARCAHV_DISTORT("arcahvdistort", true, true),
    ARCAHV_DEBRIS("arcahvdebris", true, true),
    HIDE_GROUP("hidegroup", false, true),
    ENWIDEN_CAMERA("enwidencamera", true, true),
    ENWIDEN_LANES("enwidenlanes", true, true);
    // @formatter:on

    companion object {
        fun fromId(id: String): ScenecontrolType {
            entries.forEach {
                if (it.id == id) return it
            }
            throw IllegalArgumentException("Unknown scenecontrol type: $id")
        }
    }

    fun needTimeConversion(): Boolean {
        return this.id in listOf("trackdisplay", "arcahvdistort", "arcahvdebris")
    }

}

@Serializable
class Scenecontrol(
    override val time: Long,
    val type: ScenecontrolType,
    val param1: Double?,
    val param2: Int?,
) : TimedObject {
    override fun serializeTimedObjDefault(): String {
        val params = when {
            !type.paramReq1 && !type.paramReq2 -> {
                ""
            }

            !type.paramReq1 && type.paramReq2 -> {
                ",0.00,${param2!!}"
            }

            type.paramReq1 && !type.paramReq2 -> {
                ",${param1!!.affFormat},0"
            }

            else -> {
                ",${param1!!.affFormat},${param2!!}"
            }

        }
        return "scenecontrol(${time},${type.id}${params});"
    }

    override fun serializeTimedObjForArcCreate(ctx: SerializationContext): String? {
        return when {
            type == ScenecontrolType.TRACK_HIDE -> {
                Scenecontrol(time, ScenecontrolType.TRACK_DISPLAY, 1000.0, 0).serializeTimedObjDefault()
            }

            type == ScenecontrolType.TRACK_SHOW -> {
                Scenecontrol(time, ScenecontrolType.TRACK_DISPLAY, 1000.0, 255).serializeTimedObjDefault()
            }

            type.needTimeConversion() -> {
                Scenecontrol(time, type, param1!!.times(1000), param2).serializeTimedObjDefault()
            }

            else -> {
                super.serializeTimedObjForArcCreate(ctx)
            }
        }
    }
}

@Serializable
data class TimingGroupSpecialEffectType(
    /**
     * @param lowercase id of the special effect, while the `name` field is the uppercase enum name
     */
    val codename: String,
) {
    companion object {
        val NO_INPUT = TimingGroupSpecialEffectType("noinput")
        val FADING_HOLDS = TimingGroupSpecialEffectType("fadingholds")
        val ANGLEX = TimingGroupSpecialEffectType("anglex")
        val ANGLEY = TimingGroupSpecialEffectType("angley")

        val entries = listOf(
            NO_INPUT,
            FADING_HOLDS,
            ANGLEX,
            ANGLEY,
        )

        fun fromCodename(codename: String): TimingGroupSpecialEffectType {
            entries.forEach {
                if (it.codename == codename) return it
            }
            throw IllegalArgumentException("Unknown timing group special effect type: $codename")
        }
    }
}

@Serializable
data class TimingGroupSpecialEffect(val type: TimingGroupSpecialEffectType, var extraParam: String?) {

    fun serializeForArcaea(): String {
        return "${type.codename}${extraParam ?: ""}"
    }

    fun serializeForArcCreate(): String {
        if (type == TimingGroupSpecialEffectType.ANGLEX || type == TimingGroupSpecialEffectType.ANGLEY) {
            extraParam = (extraParam!!.toDouble() / 10).toString()
        }
        return "${type.codename}${if (extraParam != null) "=${extraParam!!.toDouble().affFormat}" else ""}"
    }

    private fun validate() {
        if (type == TimingGroupSpecialEffectType.ANGLEX || type == TimingGroupSpecialEffectType.ANGLEY) {
            if (extraParam == null) throw IllegalArgumentException("Effect `${type.codename}` needs a parameter")
            else {
                if ((extraParam!!.toDouble() % 3600).toInt() == 0) return
                extraParam = (if (extraParam!!.toDouble() <= 0) {
                    (3600 + (extraParam!!.toDouble() % 3600)).toInt()
                } else {
                    extraParam!!.toDouble() % 3600
                }).toString()
            }
        }
    }

    constructor(effect: TimingGroupSpecialEffectType) : this(effect, null)

    init {
        validate()
    }

}

@Serializable
class TimingGroup : ChartObject {

    @Transient
    var name: String = ""

    constructor(name: String) {
        this.name = name
    }

    val specialEffects: MutableList<TimingGroupSpecialEffect> = mutableListOf()

    internal val timing: MutableList<Timing> = mutableListOf()

    @Transient
    private val noteFilters: ArrayDeque<NoteFilter> = ArrayDeque()

    private val notes = mutableListOf<Note>()
    private val scenecontrols = mutableListOf<Scenecontrol>()
    private val cameras = mutableListOf<Camera>()

    /**
     * get a copy of all [Note]
     */
    fun getNotes(): List<Note> {
        return notes.toList()
    }

    /**
     * get a copy of all [Timing]
     */
    fun getTimings(): List<Timing> {
        return timing.toList()
    }

    /**
     * get a copy of all [Scenecontrol]
     */
    fun getScenecontrols(): List<Scenecontrol> {
        return scenecontrols.toList()
    }

    /**
     * get a copy of all [Camera]
     */
    fun getCameras(): List<Camera> {
        return cameras.toList()
    }


    private fun applyFilterImpl(note: Note): Note {
        var ret = note

        val filterSize = noteFilters.size
        for (idx in (0 until filterSize).reversed()) {
            ret = noteFilters[idx](ret)
        }

        return ret
    }

    private fun applyFilterImpl(note: Collection<Note>): Collection<Note> {
        return note.map { applyFilterImpl(it) }
    }

    private fun Note.applyFilter(): Note {
        return applyFilterImpl(this)
    }

    private fun Collection<Note>.applyFilter(): Collection<Note> {
        return this.map { applyFilterImpl(it) }
    }

    /**
     * Add a [NoteFilter]
     */
    fun addNoteFilter(filter: NoteFilter) {
        noteFilters.addLast(filter)
    }

    /**
     * Remove the last [NoteFilter]
     */
    fun popNoteFilter() {
        noteFilters.removeLast()
    }

    /**
     * Add a [Scenecontrol]
     */
    fun addScenecontrol(sc: Scenecontrol) {
        scenecontrols.add(sc)
    }

    /**
     * Add a [NormalNote]
     */
    fun addNormalNote(note: NormalNote): Note {
        val commitNote = note.applyFilter()
        notes.add(commitNote)
        return commitNote
    }

    /**
     * Add a [HoldNote]
     */
    fun addHoldNote(note: HoldNote): Note {
        val commitNote = note.applyFilter()
        notes.add(commitNote)
        return commitNote
    }

    /**
     * Add a [ArcNote]
     */
    fun addArcNote(note: ArcNote): Note {
        val commitNote = note.applyFilter()
        notes.add(commitNote)
        return commitNote
    }

    fun addSpecialEffect(type: TimingGroupSpecialEffectType, extraParam: Int) {
        specialEffects.add(TimingGroupSpecialEffect(type, extraParam.toString()))
    }

    fun addSpecialEffect(type: TimingGroupSpecialEffectType) {
        specialEffects.add(TimingGroupSpecialEffect(type, null))
    }

    fun addCamera(camera: Camera) {
        cameras.add(camera)
    }

    internal fun addRawSpecialEffect(effect: TimingGroupSpecialEffect) {
        specialEffects.add(effect)
    }

    private fun serializeGeneric(padding: Int, serializeClosure: TimedObject.() -> String?): String {
        val `object` = mutableListOf<TimedObject>()
        `object`.addAll(notes)
        `object`.addAll(timing)
        `object`.addAll(scenecontrols)
        `object`.addAll(cameras)
        `object`.sortWith(TimedObject.Comparator)

        val sb = StringBuilder()
        for (n in `object`) {
            if (padding > 0) {
                sb.append(" ".repeat(padding))
            }
            val serializedObject = serializeClosure(n)
            if (!serializedObject.isNullOrEmpty()) {
                sb.append(serializedObject).append("\r\n")
            }
        }
        return sb.toString()
    }

    @Deprecated("please specify target", ReplaceWith("serializeForArcaea() or serializeForArcCreate()"))
    fun serializeDefault(padding: Int, chart: Chart): String {
        return serializeForArcaea(padding, chart)
    }

    fun serializeForArcaea(padding: Int, chart: Chart): String {
        return serializeGeneric(padding) {
            this.serializeTimedObjForArcaea(SerializationContext(chart, this@TimingGroup))
        }
    }

    fun serializeForArcCreate(padding: Int, chart: Chart): String {
        return serializeGeneric(padding) {
            this.serializeTimedObjForArcCreate(SerializationContext(chart, this@TimingGroup))
        }
    }

    override fun toString(): String {
        return name
    }
}

@Serializable
sealed class Note : TimedObject {
    object Comparator : kotlin.Comparator<Note> {
        override fun compare(a: Note, b: Note): Int {
            // sort by time
            val timeCmp = a.time.compareTo(b.time)
            if (timeCmp != 0) return timeCmp

            // sort by column
            if (a is KeyboardNote && b is KeyboardNote) {
                val keyCmp = a.column.compareTo(b.column)
                if (keyCmp != 0) return keyCmp
            }

            // sort by type
            if (a is KeyboardNote && b !is KeyboardNote) return -1
            if (a !is KeyboardNote && b is KeyboardNote) return 1

            if (a is ArcNote && b is ArcNote) {
                if (a.isGuidingLine() && !b.isGuidingLine()) return -1
                if (!a.isGuidingLine() && b.isGuidingLine()) return 1

                if (a.startPosition.x < b.startPosition.x) return -1
                if (a.startPosition.x > b.startPosition.x) return 1

                if (a.startPosition.y < b.startPosition.y) return -1
                if (a.startPosition.y > b.startPosition.y) return 1
            }
            return 0
        }
    }
}

@Serializable
@SerialName("keyboard")
sealed class KeyboardNote : Note() {
    abstract val column: Int
}

@Serializable
@SerialName("normal")
data class NormalNote(
    override val time: Long,
    override val column: Int,
) : KeyboardNote() {
    override fun serializeTimedObjDefault(): String = "($time,$column);"
}

@Serializable
@SerialName("hold")
data class HoldNote(
    override val time: Long,
    val endTime: Long,
    override val column: Int,
) : KeyboardNote() {
    override fun serializeTimedObjDefault(): String = "hold($time,$endTime,$column);"
}

@Serializable
@SerialName("arc")
data class ArcNote(
    override val time: Long,
    val endTime: Long,
    val startPosition: Position,
    val curveType: CurveType,
    val endPosition: Position,
    val color: Color,
    var hitSound: String,
    var arcType: String,
    var arcResolution: Double,
    val arcTapList: MutableList<Long>,
) : Note() {

    fun isGuidingLine(): Boolean =
        arcType == "true" // || (arcTapList.isNotEmpty() && arcType != "designant") /* this convertion logic is in the ctor during initialization */

    fun isDesignant(): Boolean = arcType == "designant"

    companion object {
        fun getEasingFunction3D(startPosition: Position, endPosition: Position, curveType: CurveType) =
            when (curveType) {
                CurveType.S -> buildEasingFunction3D(linear)
                CurveType.B -> buildBezierEasingFunction3D(
                    startPosition.x, startPosition.x, endPosition.x, endPosition.x
                )

                CurveType.SO -> buildEasingFunction3D(easeInSine, linear) // invert sIn and sOut
                CurveType.SI -> buildEasingFunction3D(easeOutSine, linear)
                CurveType.SISO -> buildEasingFunction3D(easeOutSine, easeInSine)
                CurveType.SOSI -> buildEasingFunction3D(easeInSine, easeOutSine)
                CurveType.SISI -> buildEasingFunction3D(easeOutSine)
                CurveType.SOSO -> buildEasingFunction3D(easeInSine)

                else -> {
                    throw IllegalArgumentException("Invalid curve type: $curveType")
                }
            }
    }

    constructor(
        time: Long,
        endTime: Long,
        startPosition: Position,
        curveType: CurveType,
        endPosition: Position,
        color: Color,
        arcType: String,
        arcResolution: Double = 1.0,
        arcTapList: MutableList<Long> = mutableListOf<Long>(),
        postProcessor: (ArcNote.() -> Unit) = { },
    ) : this(
        time,
        endTime,
        startPosition,
        curveType,
        endPosition,
        color,
        "none",
        arcType,
        arcResolution,
        arcTapList
    ) {
        if (arcTapList.isNotEmpty() && arcType != "designant") {
            this@ArcNote.arcType = "true"
        }
        postProcessor.invoke(this)
    }

    override fun serializeTimedObjDefault(): String {
        val sb = StringBuilder()
        sb.append(
            "arc(${time},${endTime},${startPosition.x.affFormat},${endPosition.x.affFormat},${curveType.value},${startPosition.y.affFormat},${endPosition.y.affFormat},${color.value},${
                hitSound.let {
                    if (it == "none") "none" else {
                        if (!it.endsWith("_wav") && !it.endsWith(".wav")) "${it}_wav" else it.replace(".wav", "_wav")
                    }
                }
            },${arcType}${if (arcResolution != 1.0) ",$arcResolution" else ""})"
        )
        if (arcTapList.isNotEmpty()) {
            arcTapList.sort()
            sb.append("[")
            for (idx in arcTapList.indices) {
                val tap = arcTapList[idx]
                sb.append("arctap(${tap})")
                if (idx < arcTapList.size - 1) {
                    sb.append(",")
                }
            }
            sb.append("]")
        }
        sb.append(";")
        return sb.toString()
    }

    override fun serializeTimedObjForArcCreate(ctx: SerializationContext): String? {
        fun buildArcSentence(): String {
            val sb = StringBuilder()
            sb.append("arc(${time},${endTime},${startPosition.x.affFormat},${endPosition.x.affFormat},${curveType.value},${startPosition.y.affFormat},${endPosition.y.affFormat},${color.value},${hitSound},$arcType)")
            if (arcTapList.isNotEmpty()) {
                arcTapList.sort()
                sb.append("[")
                for (idx in arcTapList.indices) {
                    val tap = arcTapList[idx]
                    sb.append("arctap(${tap})")
                    if (idx < arcTapList.size - 1) {
                        sb.append(",")
                    }
                }
                sb.append("]")
            }
            sb.append(";")
            return sb.toString()
        }

        // "designant" exception
        if (isDesignant()) return null

        // var-len arc-taps exception
        if (color == Color.GRAY && !isGuidingLine() && time == endTime) {
            val center = (startPosition.x + endPosition.x) / 2 pos startPosition.y
            return "arc(${time},${endTime},${center.x.affFormat},${center.x.affFormat},s,${center.y.affFormat},${center.y.affFormat},0,${hitSound},true)[arctap(${time},${
                abs(
                    startPosition.x - endPosition.x
                ) * 2
            })];"
        }

        // handle arcResolution
        if (arcResolution != 1.0) {
            // duplicate all `timing` commands
            val parentGroupId = if (ctx.timingGroup.name != "main") {
                ctx.chart.subTiming.values.indexOf(ctx.timingGroup)
            } else "main"

            val tgName = "${parentGroupId}_arcResolution${arcResolution}"
            val timingGroup = ctx.chart.subTiming.getOrPut(tgName) { TimingGroup(tgName) }

            // newly generated
            if (timingGroup.specialEffects.isEmpty()) {
                timingGroup.timing.addAll(ctx.timingGroup.getTimings())

                // duplicate all fx
                timingGroup.specialEffects.addAll(ctx.timingGroup.specialEffects)

                timingGroup.addRawSpecialEffect(
                    TimingGroupSpecialEffect(
                        TimingGroupSpecialEffectType("arcresolution"),
                        arcResolution.toString()
                    )
                )
            }

            timingGroup.addArcNote(this.apply {
                arcResolution = 1.0 // break java.lang.StackOverflowError
            })

            //
            return ""
        }

        return buildArcSentence()
    }

    @Serializable(ArcNoteCurveTypeSerializer::class)
    data class CurveType(val value: String) {
        companion object {
            val S = CurveType("s")
            val B = CurveType("b")
            val SI = CurveType("si")
            val SO = CurveType("so")
            val SISI = CurveType("sisi")
            val SOSO = CurveType("soso")
            val SISO = CurveType("siso")
            val SOSI = CurveType("sosi")
        }
    }

    @Serializable(ArcNoteColorSerializer::class)
    data class Color(val value: Int) {
        companion object {
            val BLUE = Color(0)
            val RED = Color(1)
            val GREEN = Color(2)
            val GRAY = Color(3)
        }
    }
}

@Serializable
data class Camera(
    override val time: Long,
    val xOff: Double,
    val yOff: Double,
    val zOff: Double,
    val xozAng: Double,
    val yozAng: Double,
    val xoyAng: Double,
    val ease: CameraEaseType,
    val duration: Long,
) : TimedObject {

    internal constructor(
        time: Long,
        xOff: Double,
        yOff: Double,
        zOff: Double,
        xozAng: Double,
        yozAng: Double,
        xoyAng: Double,
        ease: ArcNote.CurveType,
        duration: Long,
    ) : this(time, xOff, yOff, zOff, xozAng, yozAng, xoyAng, CameraEaseType(ease.value), duration) {
        if (ease.value != "s") throw IllegalArgumentException("CurveType of ArcNote cannot be applied to Camera")
    }

    @Serializable(CameraEaseTypeSerializer::class)
    data class CameraEaseType(val value: String) {
        companion object {
            val S = CameraEaseType("s")
            val L = CameraEaseType("l")
            val QI = CameraEaseType("qi")
            val QO = CameraEaseType("qo")
            val RESET = CameraEaseType("reset")
        }
    }

    override fun serializeTimedObjDefault(): String =
        "camera($time,${xOff.affFormat},${yOff.affFormat},${zOff.affFormat},${xozAng.affFormat},${yozAng.affFormat},${xoyAng.affFormat},${ease.value},$duration);"
}

@Serializable(PositionSerializer::class)
data class Position(
    var x: Double,
    var y: Double,
) {

    /**
     * Returns string representation of the [Position] including its [x] and [y] values.
     */
    override fun toString(): String = "($x, $y)"

    fun toList(): List<Double> = listOf(x, y)

    fun toPair(): Pair<Double, Double> = x to y

    operator fun times(n: Double): Position {
        return Position(x * n, y * n)
    }

    operator fun plus(n: Double): Position {
        return Position(x + n, y + n)
    }

    operator fun dec(n: Double): Position {
        return Position(x - n, y - n)
    }

    operator fun div(n: Double): Position {
        return Position(x / n, y / n)
    }

}

infix fun <A : Number, B : Number> A.pos(that: B): Position = Position(this.toDouble(), that.toDouble())

fun Pair<Double, Double>.toPosition(): Position {
    return this.first pos this.second
}
