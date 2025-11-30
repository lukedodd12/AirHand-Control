package com.example.airhandcontrol

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun HandOverlay(landmarks: List<Pair<Float, Float>>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (landmarks.isEmpty()) return@Canvas

        val path = Path()
        val first = landmarks.first()
        path.moveTo(first.first * size.width, first.second * size.height)
        for (p in landmarks.drop(1)) {
            path.lineTo(p.first * size.width, p.second * size.height)
        }

        drawPath(path, color = Color.Green.copy(alpha = 0.8f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
