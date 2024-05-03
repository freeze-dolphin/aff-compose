package com.tairitsu.compose.arcaea

import com.tairitsu.compose.arcaea.serializer.ArcNoteColorSerializer
import com.tairitsu.compose.arcaea.serializer.ArcNoteCurveTypeSerializer
import com.tairitsu.compose.arcaea.serializer.ArcTapListSerializer
import com.tairitsu.compose.arcaea.serializer.PositionSerializer
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

    fun serialize(): String {
        val sb = StringBuilder()

        sb.append("AudioOffset:${configuration.audioOffset}\r\n")
        configuration.extra.forEach {
            sb.append("${it.name}:${it.value}\r\n")
        }
        sb.append("-\r\n")

        sb.append(mainTiming.serialize(0))
        for (timing in subTiming.values) {
            sb.append("timinggroup(${
                timing.specialEffects.joinToString("_") {
                    it.serialize()
                }
            }){\r\n")
            sb.append(timing.serialize(padding = 4))
            sb.append("};\r\n")
        }

        return sb.toString().trim { it <= ' ' }
    }

    companion object {

        fun fromAff(aff: String): Chart = ANTLRChartParser.fromAff(aff)

        @Deprecated("use ANTLRChartParser#fromAff instead", ReplaceWith("com.tairitsu.compose.arcaea.ANTLRChartParser.fromAff(aff)"))
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

    object Comparator : kotlin.Comparator<TimedObject> {
        override fun compare(a: TimedObject, b: TimedObject): Int {
            // sort by time
            val timeCmp = a.time.compareTo(b.time)
            if (timeCmp != 0) return timeCmp

            if ((a is Timing && b is Timing) || (a is Scenecontrol && b is Scenecontrol)) {
                return 0
            }
            if (a is Note && b is Note) {
                return Note.Comparator.compare(a, b)
            }
            if (a is Timing || a is Scenecontrol) {
                return -1
            }
            if (b is Timing || b is Scenecontrol) {
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

    fun serialize(): String {
        return "${type.codename}${extraParam ?: ""}"
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

    companion object {
        val noinput = TimingGroupSpecialEffect(TimingGroupSpecialEffectType.NO_INPUT)
        val fadingholds = TimingGroupSpecialEffect(TimingGroupSpecialEffectType.FADING_HOLDS)
        fun anglex(extraParam: Int): TimingGroupSpecialEffect = TimingGroupSpecialEffect(TimingGroupSpecialEffectType.ANGLEX, extraParam)
        fun angley(extraParam: Int): TimingGroupSpecialEffect = TimingGroupSpecialEffect(TimingGroupSpecialEffectType.ANGLEX, extraParam)
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

    internal fun addRawSpecialEffect(effect: TimingGroupSpecialEffect) {
        specialEffects.add(effect)
    }

    fun serialize(padding: Int): String {
        val `object` = mutableListOf<TimedObject>()
        `object`.addAll(notes)
        `object`.addAll(timing)
        `object`.addAll(scenecontrols)
        `object`.sortWith(TimedObject.Comparator)

        val sb = StringBuilder()
        for (n in `object`) {
            if (padding > 0) {
                sb.append(" ".repeat(padding))
            }
            sb.append(n.serialize()).append("\r\n")
        }
        return sb.toString()
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
        sb.append("arc(${time},${endTime},${startPosition.x.affFormat},${endPosition.x.affFormat},${curveType.value},${startPosition.y.affFormat},${endPosition.y.affFormat},${color.value},$hitSound,$isGuidingLine)")
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
    if (this.isGuidingLine) return this
    this.hitSound = rawHitsound
    return this
}