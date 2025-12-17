package com.tinsic.app.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiTranslationRepository {

    suspend fun translateLyrics(lines: List<String>, apiKey: String, targetLanguage: String): List<String> = withContext(Dispatchers.IO) {
        if (lines.isEmpty()) return@withContext emptyList()

        android.util.Log.d("GeminiRepo", "Translating ${lines.size} lines to $targetLanguage with key: $apiKey")

        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash", 
            apiKey = apiKey
        )

        // Strategy: Send numbered lines to force strict alignment.
        // Input format: "Index| Text"
        // Expected output: "Index| TranslatedText"
        val prompt = buildString {
            append("Translate the following song lyrics to $targetLanguage line by line.\n")
            append("Each line starts with an ID (e.g., '0| '). Return the translated line starting with the SAME ID.\n")
            append("Format your response exactly as: 'ID| Translated Text'.\n")
            append("If a line is instrumental or empty, return 'ID| '.\n")
            append("Do NOT merge lines. The output must have the same number of lines with matching IDs.\n\n")
            
            lines.forEachIndexed { index, line ->
                append("$index| $line\n")
            }
        }

        try {
            val response = generativeModel.generateContent(prompt)
            val text = response.text ?: ""
            android.util.Log.d("GeminiRepo", "Raw response length: ${text.length}")
            
            // Map to store translations by ID
            val translationMap = mutableMapOf<Int, String>()
            
            text.lines().forEach { line ->
                // Check if line starts with an integer followed by '|'
                val delimiterIndex = line.indexOf('|')
                if (delimiterIndex != -1) {
                    val idPart = line.substring(0, delimiterIndex).trim()
                    val id = idPart.toIntOrNull()
                    if (id != null) {
                        val content = line.substring(delimiterIndex + 1).trim()
                        translationMap[id] = content
                    }
                }
            }
            
            // Reconstruct the list ensuring 1:1 mapping
            val result = List(lines.size) { index ->
                translationMap[index] ?: "" // Default to empty if missing
            }

            android.util.Log.d("GeminiRepo", "Parsed ${translationMap.size} valid lines. Returning ${result.size} aligned lines.")
            result
        } catch (e: Exception) {
            android.util.Log.e("GeminiRepo", "Translation error (Ask Gemini)", e)
            e.printStackTrace()
            emptyList()
        }
    }
}
