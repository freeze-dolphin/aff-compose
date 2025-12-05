package com.tairitsu.compose

abstract class NoteFilter {
    operator fun invoke(note: Note): Note {
        return when (note) {
            is NormalNote -> filterNormalNote(note)
            is HoldNote -> filterHoldNote(note)
            is ArcNote -> filterArcNote(note)
            is ArcTapNote -> filterArcTapNote(note)
        }
    }

    abstract fun filterNormalNote(note: NormalNote): Note
    abstract fun filterHoldNote(note: HoldNote): Note
    abstract fun filterArcNote(note: ArcNote): Note

    abstract fun filterArcTapNote(note: ArcTapNote): Note
}

abstract class EventFilter {
    operator fun invoke(event: TimedObject): TimedObject {
        return when (event) {
            is Timing -> filterTiming(event)
            is Scenecontrol -> filterScenecontrol(event)
            is Camera -> filterCamera(event)
            else -> filterElse(event)
        }
    }

    abstract fun filterTiming(event: Timing): TimedObject
    abstract fun filterScenecontrol(sc: Scenecontrol): TimedObject
    abstract fun filterCamera(camera: Camera): TimedObject
    abstract fun filterElse(timedObject: TimedObject): TimedObject
}
