package com.example.airhandcontrol

import android.content.Context
import android.graphics.PointF
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Simple store for on-device training samples (landmarks).
 * Stores JSON files under app files dir.
 */
class HandSampleStore(private val context: Context) {
    private val dir: File = File(context.filesDir, "hand_samples").apply { if (!exists()) mkdirs() }

    fun saveSample(name: String, landmarks: List<PointF>) {
        val file = File(dir, "${name}.csv")
        file.printWriter().use { out ->
            landmarks.forEach { out.println("${it.x},${it.y}") }
        }
    }

    fun listSamples(): List<File> = dir.listFiles()?.toList() ?: emptyList()
}
