package com.tinsic.app.core.utils

import com.tinsic.app.domain.karaoke.model.LyricLine
import java.io.BufferedReader
import java.io.StringReader
import java.util.regex.Pattern

object LrcParser {
    private val PATTERN_LINE = Pattern.compile("((\\[\\d{2}:\\d{2}\\.\\d{2,3}])+)(.*)")
    private val PATTERN_TIME = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})]")

    fun parse(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val reader = BufferedReader(StringReader(lrcContent))
        var lineText = reader.readLine()
        
        while (lineText != null) {
            val matcher = PATTERN_LINE.matcher(lineText)
            if (matcher.find()) {
                val timeGroup = matcher.group(1)
                val content = matcher.group(3)?.trim() ?: ""
                
                val timeMatcher = PATTERN_TIME.matcher(timeGroup)
                while (timeMatcher.find()) {
                    val min = timeMatcher.group(1).toLong()
                    val sec = timeMatcher.group(2).toLong()
                    val mil = timeMatcher.group(3).toLong()
                    
                    // Convert to seconds (Double)
                    // Note: mil could be 2 digits (1/100s) or 3 digits (1/1000s)
                    // Standard LRC is usually 2 digits
                    val timeSec = min * 60 + sec + (mil / if(timeMatcher.group(3).length == 3) 1000.0 else 100.0)
                    
                    // Parse Singer Tag <1>, <2>, <3>
                    var singerId = 1
                    var isDuet = false
                    var cleanContent = content

                    if (content.startsWith("<1>")) {
                        singerId = 1
                        cleanContent = content.substring(3).trim()
                    } else if (content.startsWith("<2>")) {
                        singerId = 2
                        cleanContent = content.substring(3).trim()
                    } else if (content.startsWith("<3>")) {
                        singerId = 3 // Special ID for BOTH
                        isDuet = true
                        cleanContent = content.substring(3).trim()
                    }

                    lines.add(LyricLine(timeSec, cleanContent, singerId, isDuet))
                }
            }
            lineText = reader.readLine()
        }

        return lines.sortedBy { it.startTime }
    }
}
