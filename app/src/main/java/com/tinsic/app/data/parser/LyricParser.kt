package com.tinsic.app.data.parser

import com.tinsic.app.data.model.LyricLine

object LyricParser {
    /**
     * Parses LRC format lyrics
     * Format: [mm:ss.xx]Lyric text
     * Example: [00:12.00]First line of lyrics
     */
    fun parseLrc(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        
        lrcContent.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            
            // Match pattern: [mm:ss.xx]text
            val regex = """\[(\d{2}):(\d{2})\.(\d{2})\](.*)""".toRegex()
            val match = regex.find(trimmed)
            
            match?.let {
                val (minutes, seconds, centiseconds, text) = it.destructured
                
                // Convert to milliseconds
                val timeMs = (minutes.toLong() * 60 * 1000) +
                            (seconds.toLong() * 1000) +
                            (centiseconds.toLong() * 10)
                
                if (text.isNotBlank()) {
                    lines.add(LyricLine(timeMs, text.trim(), ""))
                }
            }
        }
        
        // Sort by time
        return lines.sortedBy { it.timeMs }
    }
}
