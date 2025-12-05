package com.tairitsu.compose

open class NoteFilter {
    operator fun invoke(note: Note): Note {
        return when (note) {
            is NormalNote -> filterNormalNote(note)
            is HoldNote -> filterHoldNote(note)
            is ArcNote -> filterArcNote(note)
            is ArcTapNote -> filterArcTapNote(note)
        }
    }

    open fun filterNormalNote(note: NormalNote): Note = note
    open fun filterHoldNote(note: HoldNote): Note = note
    open fun filterArcNote(note: ArcNote): Note = note
    open fun filterArcTapNote(note: ArcTapNote): Note = note
}

open class EventFilter {
    operator fun invoke(event: TimedObject): TimedObject {
        return when (event) {
            is Timing -> filterTiming(event)
            is Scenecontrol -> filterScenecontrol(event)
            is Camera -> filterCamera(event)
            else -> filterElse(event)
        }
    }

    open fun filterTiming(event: Timing): TimedObject = event
    open fun filterScenecontrol(sc: Scenecontrol): TimedObject = sc
    open fun filterCamera(camera: Camera): TimedObject = camera
    open fun filterElse(timedObject: TimedObject): TimedObject = timedObject
}

fun interface TimingGroupSpecialEffectFilter {
    fun filter(fx: TimingGroup.SpecialEffect): TimingGroup.SpecialEffect

    companion object {
        val DEFAULT = TimingGroupSpecialEffectFilter { it }
    }
}