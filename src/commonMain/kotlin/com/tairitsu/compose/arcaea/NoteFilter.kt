package com.tairitsu.compose.arcaea

abstract class NoteFilter {
    operator fun invoke(note: Note): Note {
        return when (note) {
            is NormalNote -> filterNormalNote(note)
            is HoldNote -> filterHoldNote(note)
            is ArcNote -> filterArcNote(note)
        }
    }

    abstract fun filterNormalNote(note: NormalNote): Note
    abstract fun filterHoldNote(note: HoldNote): Note
    abstract fun filterArcNote(note: ArcNote): Note
}
