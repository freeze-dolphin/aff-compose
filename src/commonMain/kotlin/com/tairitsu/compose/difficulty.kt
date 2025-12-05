package com.tairitsu.compose

import com.tairitsu.compose.DifficultyContext.Companion.wrap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A difficulty of the song
 */
@Serializable
class Difficulty(
    @Transient
    val chartConfiguration: Chart.Configuration = Chart.Configuration.DEFAULT,
) {

    /**
     * The chart content of the difficulty
     */
    @Transient
    val chart: Chart = Chart(this.chartConfiguration)

    /**
     * The context while mapping in code.
     */
    @Transient
    val context: DifficultyContext = DifficultyContext()

    init {
        context["AffComposeTimingGroupStack"] = ArrayDeque<TimingGroup>()
    }

    /**
     * Get the current context.
     */
    internal val currentTimingGroup: TimingGroup
        get() {
            return if (context.timingGroupStack.isEmpty()) {
                chart.mainTiming
            } else {
                context.timingGroupStack.last()
            }
        }

    /**
     * Add note filter.
     */
    fun addNoteFilter(filter: NoteFilter) {
        currentTimingGroup.addNoteFilter(filter)
    }

    /**
     * Pop note filter
     */
    fun popNoteFilter() {
        currentTimingGroup.popNoteFilter()
    }

    companion object {
        val DifficultyContext.timingGroupStack: ArrayDeque<TimingGroup>
            get() = this.wrap("AffComposeTimingGroupStack")
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