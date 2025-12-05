package com.tairitsu.compose.filter

import com.tairitsu.compose.ArcNote
import com.tairitsu.compose.ArcNote.Color
import com.tairitsu.compose.ArcTapNote
import com.tairitsu.compose.HoldNote
import com.tairitsu.compose.NormalNote
import com.tairitsu.compose.Note
import com.tairitsu.compose.NoteFilter
import com.tairitsu.compose.Position.Companion.pos

object MirrorNoteFilter : NoteFilter() {
    override fun filterNormalNote(note: NormalNote): Note {
        return NormalNote(note.time, 5 - note.column)
    }

    override fun filterHoldNote(note: HoldNote): Note {
        return HoldNote(note.time, note.endTime, 5 - note.column)
    }

    override fun filterArcNote(note: ArcNote): Note {
        val color = when (note.color) {
            Color.BLUE -> Color.RED
            Color.RED -> Color.BLUE
            else -> note.color
        }

        val startPosition = (1.0 - note.startPosition.x) pos note.startPosition.y
        val endPosition = (1.0 - note.endPosition.x) pos note.endPosition.y

        return note.copy(
            color = color,
            startPosition = startPosition,
            endPosition = endPosition
        )
    }

    override fun filterArcTapNote(note: ArcTapNote): Note = note
}
