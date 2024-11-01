package com.tairitsu.compose.arcaea

import com.tairitsu.compose.arcaea.serializer.*
import io.sn.aetherium.utils.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.text.DecimalFormat
import kotlin.math.roundToInt


@Serializable
class Chart {

    val configuration: ChartConfiguration = ChartConfiguration(0, mutableListOf())
    val mainTiming: TimingGroup = TimingGroup("main")
    val subTiming: MutableMap<String, TimingGroup> = mutableMapOf()

    @Deprecated("please specify the format", ReplaceWith("com.tairitsu.compose.arcaea.Chart.serializeForArcaea()"))
    fun serialize() = serializeForArcaea()


    fun serializeForArcaea(): String {
        val sb = StringBuilder()

        sb.append("AudioOffset:${configuration.audioOffset}\r\n")
        configuration.extra.forEach {
            sb.append("${it.name}:${it.value}\r\n")
        }
        sb.append("-\r\n")

        sb.append(mainTiming.serializeForArcaea(0))
        for (timing in subTiming.values) {
            sb.append("timinggroup(${
                timing.specialEffects.joinToString("_") {
                    it.serializeForArcaea()
                }
            }){\r\n")
            sb.append(timing.serializeForArcaea(padding = 4))
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

        sb.append(mainTiming.serializeForArcCreate(0))
        for (timing in subTiming.values) {
            sb.append("timinggroup(${
                timing.specialEffects.joinToString(",") {
                    it.serializeForArcCreate()
                }
            }){\r\n")
            sb.append(timing.serializeForArcCreate(padding = 4))
            sb.append("};\r\n")
        }

        return sb.toString().trim { it <= ' ' }
    }

    companion object {

        fun fromAff(aff: String): Chart = ANTLRChartParser.fromAff(aff)

        fun fromAcf(acf: String): Pair<Chart, ANTLRChartParser.ArcCreateChartParseReport> = ANTLRChartParser.fromAcf(acf)

        @Deprecated("use ANTLRChartParser#fromAff instead", ReplaceWith("com.tairitsu.compose.arcaea.Chart.fromAff(aff)"))
        fun fromAffRaw(aff: String): Chart = RawChartParser.fromAff(aff)
    }

}


@Serializable
data class ChartConfiguration(var audioOffset: Long, val extra: MutableList<ConfigurationItem>) {
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

    fun sync(other: ChartConfiguration) {
        audioOffset = other.audioOffset
        extra.addAll(other.extra)
    }

}

interface ChartObject

interface TimedObject : ChartObject {
    val time: Long

    fun serialize(): String
    fun serializeForArcaea() = serialize()
    fun serializeForArcCreate() = serialize()

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
        return DecimalFormat("#0.00").format((this * 100.00).roundToInt() / 100.00)
    }

@Serializable
class Timing(val offset: Long, val bpm: Double, val beats: Double) : TimedObject {
    override val time: Long
        get() = offset

    override fun serialize(): String {
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
    override fun serialize(): String {
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
        return "scenecontrol(${time.toBigDecimal()},${type.id}${params});"
    }
}

@Serializable
enum class TimingGroupSpecialEffectType(val codename: String) {
    // @formatter:off
    NO_INPUT("noinput"),
    FADING_HOLDS("fadingholds"),
    ANGLEX("anglex"),
    ANGLEY("angley");
    // @formatter:on

    companion object {
        fun fromCodename(codename: String): TimingGroupSpecialEffectType {
            TimingGroupSpecialEffectType.entries.forEach {
                if (it.codename == codename) return it
            }
            throw IllegalArgumentException("Unknown timing group special effect type: $codename")
        }
    }
}

@Serializable
data class TimingGroupSpecialEffect(val type: TimingGroupSpecialEffectType, var extraParam: Int?) {

    fun serializeForArcaea(): String {
        return "${type.codename}${extraParam ?: ""}"
    }

    fun serializeForArcCreate(): String {
        if (type == TimingGroupSpecialEffectType.ANGLEX || type == TimingGroupSpecialEffectType.ANGLEY) {
            extraParam = extraParam!! / 10
        }
        return "${type.codename}${if (extraParam != null) "=${extraParam!!.toDouble().affFormat}" else ""}"
    }

    private fun validate() {
        if (type == TimingGroupSpecialEffectType.ANGLEX || type == TimingGroupSpecialEffectType.ANGLEY) {
            if (extraParam == null) throw IllegalArgumentException("Effect `${type.codename}` needs a parameter")
            else {
                if (extraParam!! % 3600 == 0) return
                extraParam = if (extraParam!! <= 0) {
                    (3600 + (extraParam!! % 3600))
                } else {
                    extraParam!! % 3600
                }
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
        specialEffects.add(TimingGroupSpecialEffect(type, extraParam))
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

    private fun serializeGeneric(padding: Int, serializeClosure: TimedObject.() -> String): String {
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
            sb.append(serializeClosure(n)).append("\r\n")
        }
        return sb.toString()
    }

    @Deprecated("please specify target", ReplaceWith("serializeGeneric(padding) { this.serializeForArcaea() }"))
    fun serialize(padding: Int): String {
        return serializeForArcaea(padding)
    }

    fun serializeForArcaea(padding: Int): String {
        return serializeGeneric(padding) {
            this.serializeForArcaea()
        }
    }

    fun serializeForArcCreate(padding: Int): String {
        return serializeGeneric(padding) {
            this.serializeForArcCreate()
        }
    }

    override fun toString(): String {
        return name
    }
}

@Serializable
sealed class Note : TimedObject {
    override fun toString(): String = serialize()

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
                if (a.isGuidingLine && !b.isGuidingLine) return -1
                if (!a.isGuidingLine && b.isGuidingLine) return 1

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
    override fun serialize(): String = "($time,$column);"
}

@Serializable
@SerialName("hold")
data class HoldNote(
    override val time: Long,
    val endTime: Long,
    override val column: Int,
) : KeyboardNote() {
    override fun serialize(): String = "hold($time,$endTime,$column);"
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
    var isGuidingLine: Boolean,

    @Serializable(ArcTapListSerializer::class) @SerialName("tapList") val arcTapList: ArcTapList,
) : Note() {

    companion object {
        fun getEasingFunction3D(startPosition: Position, endPosition: Position, curveType: CurveType) =
            when (curveType) {
                CurveType.S -> buildEasingFunction3D(linear)
                CurveType.B -> buildBezierEasingFunction3D(
                    startPosition.x, startPosition.x, endPosition.x, endPosition.x
                )

                CurveType.SO -> buildEasingFunction3D(easeInSine, linear) // revert sIn and sOut
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
        startPosition: Pair<Double, Double>,
        curveType: CurveType,
        endPosition: Pair<Double, Double>,
        color: Color,
        isGuidingLine: Boolean,
        arcTapClosure: (ArcTapList.() -> Unit) = {},
    ) : this(
        time,
        endTime,
        Position(startPosition.first, startPosition.second),
        curveType,
        Position(endPosition.first, endPosition.second),
        color,
        "none",
        isGuidingLine,
        ArcTapList(mutableListOf())
    ) {
        arcTapList.arcNote = this
        arcTapClosure.invoke(arcTapList)
        if (arcTapTimestamps.isNotEmpty()) {
            this@ArcNote.isGuidingLine = true
        }
    }

    constructor(
        time: Long,
        endTime: Long,
        startPosition: Position,
        curveType: CurveType,
        endPosition: Position,
        color: Color,
        isGuidingLine: Boolean,
        arcTapClosure: (ArcTapList.() -> Unit) = {},
    ) : this(
        time, endTime, startPosition, curveType, endPosition, color, "none", isGuidingLine, ArcTapList(mutableListOf())
    ) {
        arcTapList.arcNote = this
        arcTapClosure.invoke(arcTapList)
        if (arcTapTimestamps.isNotEmpty()) {
            this@ArcNote.isGuidingLine = true
        }
    }

    private val arcTapTimestamps: MutableList<Long>
        get() = arcTapList.data

    override fun serialize(): String {
        val sb = StringBuilder()
        sb.append(
            "arc(${time},${endTime},${startPosition.x.affFormat},${endPosition.x.affFormat},${curveType.value},${startPosition.y.affFormat},${endPosition.y.affFormat},${color.value},${
                hitSound.let {
                    if (it == "none") "none" else {
                        if (!it.endsWith("_wav") && !it.endsWith(".wav")) "${it}_wav" else it.replace(".wav", "_wav")
                    }
                }
            },$isGuidingLine)"
        )
        if (arcTapTimestamps.isNotEmpty()) {
            arcTapTimestamps.sort()
            sb.append("[")
            for (idx in arcTapTimestamps.indices) {
                val tap = arcTapTimestamps[idx]
                sb.append("arctap(${tap})")
                if (idx < arcTapTimestamps.size - 1) {
                    sb.append(",")
                }
            }
            sb.append("]")
        }
        sb.append(";")
        return sb.toString()
    }

    override fun serializeForArcCreate(): String {
        val sb = StringBuilder()
        sb.append("arc(${time},${endTime},${startPosition.x.affFormat},${endPosition.x.affFormat},${curveType.value},${startPosition.y.affFormat},${endPosition.y.affFormat},${color.value},${hitSound},$isGuidingLine)")
        if (arcTapTimestamps.isNotEmpty()) {
            arcTapTimestamps.sort()
            sb.append("[")
            for (idx in arcTapTimestamps.indices) {
                val tap = arcTapTimestamps[idx]
                sb.append("arctap(${tap})")
                if (idx < arcTapTimestamps.size - 1) {
                    sb.append(",")
                }
            }
            sb.append("]")
        }
        sb.append(";")
        return sb.toString()
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
        }
    }

    @Serializable
    data class ArcTapList(
        val data: MutableList<Long>,
    ) {

        internal var arcNote: ArcNote? = null

        fun tap(vararg tap: Long) {
            tap.forEach { data.add(it) }
        }

        /**
         * The same as [ArcTapList.tap]
         */
        fun arctap(vararg tap: Long) {
            tap(*tap)
        }

        val parent: ArcNote
            get() {
                if (arcNote == null) throw IllegalStateException("The parent arcNote is null")
                return arcNote!!
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
    val duration: Long
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
        duration: Long
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

    override fun serialize(): String =
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

fun Note.withHitsound(hitsound: String): ArcNote {
    return withRawHitsound("${hitsound}_wav")
}

internal fun Note.withRawHitsound(rawHitsound: String): ArcNote {
    if (this !is ArcNote) throw IllegalStateException("Hitsound is only available for ArcNotes")
    // if (!this.isGuidingLine) return this
    this.hitSound = rawHitsound
    return this
}