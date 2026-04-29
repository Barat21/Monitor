package com.example.babymonitor

import android.content.res.AssetManager
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

object BabyCryInference {

    fun create(assetManager: AssetManager): Interpreter {
        val bytes = assetManager.open("baby_cry_model.tflite").readBytes()
        val modelBuffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        modelBuffer.put(bytes)
        modelBuffer.rewind()
        return Interpreter(modelBuffer)
    }

    fun predictCryScore(interpreter: Interpreter, audio: ShortArray): Float {
        val input = ByteBuffer.allocateDirect(audio.size * 4).order(ByteOrder.nativeOrder())
        audio.forEach { sample ->
            input.putFloat(sample / 32768f)
        }
        val output = Array(1) { FloatArray(1) }
        interpreter.run(input.rewind(), output)
        return output[0][0]
    }
}
