package com.tairitsu.compose.arcaea

object PostProcessor {
    fun Note.hitsound(hitsound: String) {
        rawHitsound("${hitsound}_wav")
    }

    fun Note.rawHitsound(rawHitsound: String) {
        if (this !is ArcNote) throw IllegalArgumentException("Hitsound is only available for ArcNotes")
        hitSound = rawHitsound
    }

    fun Note.arcResolution(res: Double) {
        if (this !is ArcNote) throw IllegalArgumentException("Hitsound is only available for ArcNotes")
        arcResolution = res
    }
}