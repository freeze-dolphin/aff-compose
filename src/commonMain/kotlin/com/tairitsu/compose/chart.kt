@file:Suppress("unused")

package com.tairitsu.compose

import com.tairitsu.compose.Chart.DifficultyContext.Companion.wrap
import com.tairitsu.compose.json_serializer.PositionSerializer
import io.sn.aetherium.utils.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Chart(
    val configuration: Configuration = Configuration.DEFAULT,
    val fxFilter: TimingGroupSpecialEffectFilter = TimingGroupSpecialEffectFilter.DEFAULT,
) {
    val mainTiming: TimingGroup = TimingGroup("main", this.fxFilter)
    val subTiming: MutableMap<String, TimingGroup> = mutableMapOf()
    val postTiming: MutableMap<String, TimingGroup> = mutableMapOf()

    /**
     * The context while mapping in code
     */
    @Transient
    val context: DifficultyContext = DifficultyContext()

    init {
        context["AffComposeTimingGroupStack"] = ArrayDeque<TimingGroup>()
    }

    /**
     * Get the current context
     */
    internal val currentTimingGroup: TimingGroup
        get() {
            return if (context.timingGroupStack.isEmpty()) {
                mainTiming
            } else {
                context.timingGroupStack.last()
            }
        }

    /**
     * Add a [NoteFilter]
     */
    fun addNoteFilter(filter: NoteFilter) {
        currentTimingGroup.addNoteFilter(filter)
    }

    /**
     * Remove the last [NoteFilter]
     */
    fun popNoteFilter() {
        currentTimingGroup.popNoteFilter()
    }

    /**
     * Add a [EventFilter]
     */
    fun addEventFilter(filter: EventFilter) {
        currentTimingGroup.addEventFilter(filter)
    }

    /**e
     * Remove the last [EventFilter]
     */
    fun popEventFilter() {
        currentTimingGroup.popEventFilter()
    }

    companion object {
        val DifficultyContext.timingGroupStack: ArrayDeque<TimingGroup>
            get() = wrap("AffComposeTimingGroupStack")
    }


    @Serializable
    data class Configuration(
        val audioOffset: Long,
        val extra: MutableSet<Item> = mutableSetOf(),
    ) {
        @Serializable
        data class Item(val key: String, val value: String)

        fun addItem(key: String, value: String) {
            extra.add(Item(key, value))
        }

        fun addItem(item: Item) {
            extra.add(item)
        }

        companion object {
            val DEFAULT = Configuration(0, mutableSetOf())
        }

    }

    class DifficultyContext : MutableMap<Any, Any?> {
        private val data = mutableMapOf<Any, Any?>()

        override val entries: MutableSet<MutableMap.MutableEntry<Any, Any?>>
            get() = data.entries
        override val keys: MutableSet<Any>
            get() = data.keys
        override val size: Int
            get() = data.size
        override val values: MutableCollection<Any?>
            get() = data.values

        override fun clear() {
            data.clear()
        }

        override fun isEmpty(): Boolean {
            return data.isEmpty()
        }

        override fun remove(key: Any): Any? {
            return data.remove(key)
        }

        override fun putAll(from: Map<out Any, Any?>) {
            return data.putAll(from)
        }

        override fun put(key: Any, value: Any?): Any? {
            return data.put(key, value)
        }

        override fun get(key: Any): Any? {
            return data[key]
        }

        override fun containsValue(value: Any?): Boolean {
            return data.containsValue(value)
        }

        override fun containsKey(key: Any): Boolean {
            return data.containsKey(key)
        }

        companion object {
            inline fun <reified T> DifficultyContext.wrap(key: Any): T {
                return this[key] as T
            }
        }
    }
}

interface ChartObject

interface TimedObject : ChartObject {
    val time: Long

    object Sorter : Comparator<TimedObject> {
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
                return Note.Sorter.compare(a, b)
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

expect fun Double.toAffFormat(precision: Int = 2): String

@Serializable
class Timing(val offset: Long, val bpm: Double, val beats: Double) : TimedObject {
    override val time: Long
        get() = offset
}

@Suppress("unused")
@Serializable
data class ScenecontrolType(val value: String) {
    companion object {
        val TRACK_HIDE = ScenecontrolType("trackhide")
        val TRACK_SHOW = ScenecontrolType("trackshow")
        val TRACK_DISPLAY = ScenecontrolType("trackdisplay")
        val RED_LINE = ScenecontrolType("redline")
        val ARCAHV_DISTORT = ScenecontrolType("arcahvdistort")
        val ARCAHV_DEBRIS = ScenecontrolType("arcahvdebris")
        val HIDE_GROUP = ScenecontrolType("hidegroup")
        val ENWIDEN_CAMERA = ScenecontrolType("enwidencamera")
        val ENWIDEN_LANES = ScenecontrolType("enwidenlanes")

        fun fromValue(value: String): ScenecontrolType {
            return when (value) {
                "trackhide" -> TRACK_HIDE
                "trackshow" -> TRACK_SHOW
                "trackdisplay" -> TRACK_DISPLAY
                "redline" -> RED_LINE
                "arcahvdistort" -> ARCAHV_DISTORT
                "arcahvdebris" -> ARCAHV_DEBRIS
                "hidegroup" -> HIDE_GROUP
                "enwidencamera" -> ENWIDEN_CAMERA
                "enwidenlanes" -> ENWIDEN_LANES
                else -> ScenecontrolType(value)
            }
        }
    }
}

@Serializable
class Scenecontrol(
    override val time: Long,
    val type: ScenecontrolType,
    val params: MutableList<String>,
) : TimedObject {
    constructor(
        time: Long,
        type: ScenecontrolType,
        params: Unit.() -> List<String> = { emptyList() },
    ) : this(time, type, params(Unit).toMutableList())
}

@Serializable
class TimingGroup : ChartObject {

    constructor(name: String) {
        this.name = name
        this.fxFilter = TimingGroupSpecialEffectFilter.DEFAULT
    }

    constructor(name: String, fxFilter: TimingGroupSpecialEffectFilter) {
        this.name = name
        this.fxFilter = fxFilter
    }

    @Transient
    var name: String = ""

    @Transient
    private val noteFilters: ArrayDeque<NoteFilter> = ArrayDeque()

    @Transient
    private val eventFilters: ArrayDeque<EventFilter> = ArrayDeque()

    @Transient
    private lateinit var fxFilter: TimingGroupSpecialEffectFilter

    private val notes = mutableListOf<Note>()
    private val timings = mutableListOf<Timing>()
    private val specialEffects = mutableListOf<SpecialEffect>()
    private val scenecontrols = mutableListOf<Scenecontrol>()
    private val cameras = mutableListOf<Camera>()

    @Serializable
    class SpecialEffectType(
        val value: String,
    ) {
        companion object {
            val NO_INPUT = SpecialEffectType("noinput")
            val FADING_HOLDS = SpecialEffectType("fadingholds")
            val ANGLEX = SpecialEffectType("anglex")
            val ANGLEY = SpecialEffectType("angley")

            fun fromValue(value: String): SpecialEffectType {
                return when (value) {
                    "noinput" -> NO_INPUT
                    "fadingholds" -> FADING_HOLDS
                    "anglex" -> ANGLEX
                    "angley" -> ANGLEY
                    else -> SpecialEffectType(value)
                }
            }
        }
    }

    @Serializable
    data class SpecialEffect(
        val type: SpecialEffectType,
        val param: String? = null,
    ) {
        constructor(
            type: SpecialEffectType,
            param: Number,
        ) : this(
            type,
            param.toString(),
        )
    }

    /**
     * get a copy of all [Note]
     */
    fun getNotes(): List<Note> = notes.toList()

    /**
     * get a copy of all [Timing]
     */
    fun getTimings(): List<Timing> = timings.toList()

    /**
     * get a copy of all [SpecialEffect]
     */
    fun getSpecialEffects(): List<SpecialEffect> = specialEffects.toList()

    /**
     * get a copy of all [Scenecontrol]
     */
    fun getScenecontrols(): List<Scenecontrol> = scenecontrols.toList()

    /**
     * get a copy of all [Camera]
     */
    fun getCameras(): List<Camera> = cameras.toList()

    private fun applyNoteFilterImpl(note: Note): Note {
        var ret = note

        val filterSize = noteFilters.size
        for (idx in (0 until filterSize).reversed()) {
            ret = noteFilters[idx](ret)
        }

        return ret
    }

    private fun applyEventFilterImpl(timedObject: TimedObject): TimedObject {
        var ret = timedObject

        val filterSize = eventFilters.size
        for (idx in (0 until filterSize).reversed()) {
            ret = eventFilters[idx](ret)
        }

        return ret
    }

    private fun applyNoteFilterImpl(note: Collection<Note>): Collection<Note> = note.map { applyNoteFilterImpl(it) }
    private fun applyEventFilterImpl(note: Collection<TimedObject>): Collection<TimedObject> = note.map { applyEventFilterImpl(it) }
    private fun Note.applyFilter(): Note = applyNoteFilterImpl(this)
    private fun TimedObject.applyFilter(): TimedObject = applyEventFilterImpl(this)
    private fun Collection<TimedObject>.applyFilter(): Collection<TimedObject> = this.map {
        when (this) {
            is Note -> applyNoteFilterImpl(it as Note)
            else -> applyEventFilterImpl(it)
        }
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
     * Add a [EventFilter]
     */
    fun addEventFilter(filter: EventFilter) {
        eventFilters.addLast(filter)
    }

    /**
     * Remove the last [EventFilter]
     */
    fun popEventFilter() {
        eventFilters.removeLast()
    }

    /**
     * Add a [Scenecontrol]
     */
    fun addScenecontrol(sc: Scenecontrol): Scenecontrol {
        val commitSc = sc.applyFilter() as Scenecontrol
        scenecontrols.add(commitSc)
        return commitSc
    }

    /**
     * Add a [Timing]
     */
    fun addTiming(timing: Timing): TimedObject {
        val commitTiming = timing.applyFilter() as Timing
        timings.add(commitTiming)
        return commitTiming
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

    /**
     * Add a [SpecialEffect]
     */
    fun addSpecialEffect(fx: SpecialEffect): SpecialEffect {
        check(specialEffects.all { it.type != fx.type }) { "duplicated effects" }
        check(::fxFilter.isInitialized) { "fxFilter not initialized" }

        fxFilter.filter(fx).apply {
            specialEffects.add(this)
            return this
        }
    }

    /**
     * Add a [Camera]
     */
    fun addCamera(camera: Camera): Camera {
        val commitCamera = camera.applyFilter() as Camera
        cameras.add(commitCamera)
        return commitCamera
    }

    /**
     * Add a [TimedObject]
     */
    fun addObject(obj: TimedObject) {
        when (obj) {
            is NormalNote -> addNormalNote(obj)
            is HoldNote -> addHoldNote(obj)
            is ArcNote -> addArcNote(obj)
            is Timing -> addTiming(obj)
            is Scenecontrol -> addScenecontrol(obj)
            is Camera -> addCamera(obj)
        }
    }

    /**
     * Return a new [TimingGroup] with the same timing table, but contains no chart objects
     */
    fun duplicate(name: String): TimingGroup {
        val newTg = TimingGroup(name)

        getSpecialEffects().forEach { newTg.addSpecialEffect(it) }
        noteFilters.forEach { newTg.addNoteFilter(it) }
        eventFilters.forEach { newTg.addEventFilter(it) }
        getTimings().forEach { newTg.addTiming(it) }

        return newTg
    }

    override fun toString(): String {
        return name
    }
}

@Serializable
sealed class Note : TimedObject {
    object Sorter : Comparator<TimedObject> {
        override fun compare(a: TimedObject, b: TimedObject): Int {
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
                if (a.isTrace() && !b.isTrace()) return -1
                if (!a.isTrace() && b.isTrace()) return 1

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
    abstract val columnFloat: Double

    fun isFloated(): Boolean {
        return !columnFloat.isNaN()
    }

    fun serializeColumn(): String =
        if (columnFloat.isNaN()) column.toString() else columnFloat.toAffFormat()
}

@Serializable
@SerialName("normal")
data class NormalNote(
    override val time: Long,
    override val column: Int,
    override val columnFloat: Double = Double.NaN,
) : KeyboardNote()

@Serializable
@SerialName("hold")
data class HoldNote(
    override val time: Long,
    val endTime: Long,
    override val column: Int,
    override val columnFloat: Double = Double.NaN,
) : KeyboardNote()

@Serializable
@SerialName("arcTap")
data class ArcTapNote(
    override val time: Long,
    val length: Double? = null,
) : Note()

@Serializable
@SerialName("arc")
data class ArcNote(
    override val time: Long,
    val endTime: Long,
    val startPosition: Position,
    val easeType: EaseType,
    val endPosition: Position,
    val color: Color,
    val arcType: NoteType,
    val arcResolution: Double = 1.0,
    val hitSound: String = "none",
    val arcTapList: MutableList<ArcTapNote> = mutableListOf(),
) : Note() {

    fun isTrace(): Boolean =
        arcType == NoteType.TRACE || arcType == NoteType.DESIGNANT

    companion object {
        fun easeFunc2(startPosition: Position, endPosition: Position, easeType: EaseType) = when (easeType) {
            EaseType.S -> easeFunc2(linear)
            EaseType.B -> bezierEaseFunc2(
                startPosition.x, startPosition.x, endPosition.x, endPosition.x
            )

            EaseType.SO -> easeFunc2(easeInSine, linear) // invert sIn and sOut
            EaseType.SI -> easeFunc2(easeOutSine, linear)
            EaseType.SISO -> easeFunc2(easeOutSine, easeInSine)
            EaseType.SOSI -> easeFunc2(easeInSine, easeOutSine)
            EaseType.SISI -> easeFunc2(easeOutSine)
            EaseType.SOSO -> easeFunc2(easeInSine)

            else -> {
                throw IllegalArgumentException("Invalid ease type: $easeType")
            }
        }
    }

    @Serializable
    class NoteType(val value: String) {
        companion object {
            val ARC = NoteType("false")
            val TRACE = NoteType("true")
            val DESIGNANT = NoteType("designant")

            fun fromValue(value: String): NoteType = when (value) {
                "false" -> ARC
                "true" -> TRACE
                "designant" -> DESIGNANT
                else -> NoteType(value)
            }
        }
    }

    @Serializable
    class EaseType(val value: String) {
        companion object {
            val S = EaseType("s")
            val B = EaseType("b")
            val SI = EaseType("si")
            val SO = EaseType("so")
            val SISI = EaseType("sisi")
            val SOSO = EaseType("soso")
            val SISO = EaseType("siso")
            val SOSI = EaseType("sosi")

            // alias
            val Linear = S
            val Bezier = B
            val SineIn = SI
            val SineOut = SO

            fun fromValue(value: String): EaseType = when (value) {
                "s" -> S
                "b" -> B
                "si" -> SI
                "so" -> SO
                "sisi" -> SISI
                "soso" -> SOSO
                "siso" -> SISO
                "sosi" -> SOSI
                else -> EaseType(value)
            }
        }
    }

    @Serializable
    class Color(val value: Int) {
        companion object {
            val BLUE = Color(0)
            val RED = Color(1)
            val GREEN = Color(2)
            val GRAY = Color(3)

            // alias
            val GREY = GRAY

            fun fromColorId(colorId: Int): Color = when (colorId) {
                0 -> BLUE
                1 -> RED
                2 -> GREEN
                3 -> GRAY
                else -> Color(colorId)
            }
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
    val ease: EaseType,
    val duration: Long,
) : TimedObject {

    @Serializable
    class EaseType(val value: String) {
        companion object {
            val L = EaseType("l")
            val QI = EaseType("qi")
            val QO = EaseType("qo")
            val RESET = EaseType("reset")

            // alias
            val S = L
            val Linear = L
            val CubicIn = QI
            val CubicOut = QO

            fun fromValue(value: String): EaseType = when (value) {
                "l" -> L
                "qi" -> QI
                "qo" -> QO
                "reset" -> RESET
                else -> EaseType(value)
            }
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

    companion object {
        infix fun <A : Number, B : Number> A.pos(that: B): Position = Position(this.toDouble(), that.toDouble())

        fun Pair<Double, Double>.toPosition(): Position {
            return this.first pos this.second
        }
    }

}
