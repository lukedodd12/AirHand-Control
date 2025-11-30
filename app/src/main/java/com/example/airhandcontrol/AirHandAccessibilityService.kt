package com.example.airhandcontrol

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import android.util.DisplayMetrics
import android.util.Log

class AirHandAccessibilityService : AccessibilityService() {
    private val TAG = "AirHandService"

    private var displayWidth = 1080
    private var displayHeight = 1920

    override fun onServiceConnected() {
        super.onServiceConnected()
        val metrics = resources.displayMetrics
        displayWidth = metrics.widthPixels
        displayHeight = metrics.heightPixels
        Log.i(TAG, "Service connected. Display: ${displayWidth}x${displayHeight}")
        AirHandServiceHolder.instance = this
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun movePointerNormalized(nx: Float, ny: Float) {
        // Map normalized coords (0..1) to screen
        val x = (nx * displayWidth).toInt().coerceIn(0, displayWidth)
        val y = (ny * displayHeight).toInt().coerceIn(0, displayHeight)
        dispatchMoveGesture(x, y)
    }

    private fun dispatchMoveGesture(x: Int, y: Int) {
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        val stroke = GestureDescription.StrokeDescription(path, 0, 1)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
            }
        }, null)
    }

    fun clickAtNormalized(nx: Float, ny: Float) {
        val x = (nx * displayWidth).toInt().coerceIn(0, displayWidth)
        val y = (ny * displayHeight).toInt().coerceIn(0, displayHeight)
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        val down = GestureDescription.StrokeDescription(path, 0, 1)
        val gesture = GestureDescription.Builder().addStroke(down).build()
        dispatchGesture(gesture, null, null)
    }
}

// Provide a global holder for the service instance so Activities can call control methods.
object AirHandServiceHolder {
    var instance: AirHandAccessibilityService? = null
}

