package com.tairitsu.compose.arcaea

import com.tairitsu.compose.arcaea.DifficultyContext.Companion.wrap
import com.tairitsu.compose.arcaea.serializer.DifficultyRatingClassSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A difficulty of the song
 */
@Serializable
class Difficulty {

    /**
     * The rating class of the difficulty, 0 for past, 1 for present, 2 for future, and 3 for beyond
     */
    var ratingClass: RatingClass = RatingClass.PAST
        internal set

    /**
     * The rating of the difficult, 0 for `?`, other number for corresponding number value.
     * before 3.0.0 it is 0 for `?`, 10 for `9`, 11 for `10` and 1~9 for corresponding number value.
     */
    var rating: Int = 0

    @Serializable(with = DifficultyRatingClassSerializer::class)
    enum class RatingClass(val rating: Int) {
        PAST(0),
        PRESENT(1),
        FUTURE(2),
        BEYOND(3),
        ETERNAL(4),
    }

    /**
     * The designer of the chart of the difficulty
     */
    var chartDesigner: String = "Kotlin"

    /**
     * The designer of the jacket of the difficulty
     */
    var jacketDesigner: String = "Kotlin"

    /**
     * The chart content of the difficulty
     */
    @Transient
    val chart: Chart = Chart()

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
