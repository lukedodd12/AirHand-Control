package com.example.airhandcontrol

package com.example.airhandcontrol

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max


/**
 * HandProcessor runs TensorFlow Lite models to extract hand landmarks from image frames.
 *
 * IMPORTANT: You must place the TFLite models into `app/src/main/assets`:
 * - `hand_landmark.tflite` (MediaPipe hand landmark model)
 * - optionally `palm_detection.tflite`
 *
 * This class contains a simplified pipeline suitable as a starting point.
 */
class HandProcessor(private val context: Context) {
    private val TAG = "HandProcessor"
    private var landmarkInterpreter: Interpreter? = null

    init {
        try {
            landmarkInterpreter = Interpreter(loadModelFile("hand_landmark.tflite"))
        } catch (e: Exception) {
            Log.w(TAG, "Could not load hand_landmark.tflite from assets. Add models to app/src/main/assets", e)
        }
    }

    private fun loadModelFile(assetName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(assetName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val channel = inputStream.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.length)
    }

    data class HandLandmarks(val landmarks: List<PointF>)

    /**
     * Estimate landmarks from a camera `Bitmap`.
     * This method attempts to adapt to common MediaPipe hand landmark TFLite models.
     * The model must be placed into `app/src/main/assets/hand_landmark.tflite`.
     */
    fun estimateLandmarks(bitmap: Bitmap): HandLandmarks? {
        val interp = landmarkInterpreter ?: return null

        // Inspect input tensor to determine required input size
        val inputTensor = interp.getInputTensor(0)
        val shape = inputTensor.shape() // typically [1, H, W, 3]
        val inputHeight: Int
        val inputWidth: Int
        if (shape.size == 4) {
            inputHeight = shape[1]
            inputWidth = shape[2]
        } else if (shape.size == 3) {
            inputHeight = shape[0]
            inputWidth = shape[1]
        } else {
            Log.w(TAG, "Unexpected input tensor shape: ${shape.joinToString()}")
            return null
        }

        // Resize bitmap preserving aspect by center-cropping to square then scaling
        val cropSize = max(bitmap.width, bitmap.height)
        val scaled = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)

        val inputByteBuffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3).order(ByteOrder.nativeOrder())
        // Normalize to [0,1]
        for (y in 0 until inputHeight) {
            for (x in 0 until inputWidth) {
                val px = scaled.getPixel(x, y)
                val r = ((px shr 16) and 0xFF) / 255.0f
                val g = ((px shr 8) and 0xFF) / 255.0f
                val b = (px and 0xFF) / 255.0f
                inputByteBuffer.putFloat(r)
                inputByteBuffer.putFloat(g)
                inputByteBuffer.putFloat(b)
            }
        }
        inputByteBuffer.rewind()

        // Prepare output buffer. Many MediaPipe hand landmark models output either [1,21,3]
        // or a flat vector like [1,63]. We'll try both shapes.
        val outTensor = interp.getOutputTensor(0)
        val outShape = outTensor.shape()
        val outSize = outShape.fold(1) { acc, v -> acc * v }
        val outputBuffer = FloatArray(outSize)

        try {
            interp.run(inputByteBuffer, outputBuffer)
        } catch (e: Exception) {
            Log.w(TAG, "TFLite run failed", e)
            return null
        }

        val landmarks = mutableListOf<PointF>()
        if (outSize == 21 * 3 || outSize == 63) {
            // Flatten: [1,63]
            for (i in 0 until 21) {
                val x = outputBuffer[i * 3]
                val y = outputBuffer[i * 3 + 1]
                // Some models output normalized coordinates in [0,1]
                landmarks.add(PointF(x, y))
            }
        } else if (outShape.size >= 3 && outShape[outShape.size - 2] == 21 && outShape[outShape.size - 1] >= 2) {
            // shape like [1,21,3]
            // flatten order: normally will be [1,21,3]
            for (i in 0 until 21) {
                val base = i * 3
                val x = outputBuffer[base]
                val y = outputBuffer[base + 1]
                landmarks.add(PointF(x, y))
            }
        } else {
            Log.w(TAG, "Unexpected output shape: ${outShape.joinToString()}")
            return null
        }

        return HandLandmarks(landmarks)
    }

    fun close() {
        landmarkInterpreter?.close()
    }
}

