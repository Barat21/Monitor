package com.example.babymonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.os.IBinder
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class CryMonitorService : Service() {
    companion object {
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        private const val CHANNEL_ID = "cry_monitor_channel"
        private const val SAMPLE_RATE = 16000
        private const val WINDOW_SIZE = 16000
    }

    private val running = AtomicBoolean(false)
    private var phoneNumber: String = ""

    private fun failAndStop(message: String) {
        Log.e("CryMonitorService", message)
        running.set(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER).orEmpty()
                startForeground(1, buildNotification("Listening for baby cry"))
                if (!running.get()) {
                    running.set(true)
                    monitorAudio()
                }
            }
            ACTION_STOP -> {
                running.set(false)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun monitorAudio() {
        thread(start = true) {
            val interpreter = runCatching { BabyCryInference.create(assets) }
                .getOrElse { error ->
                    failAndStop("Unable to load baby_cry_model.tflite from assets: ${error.message}")
                    return@thread
                }

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                WINDOW_SIZE * 2
            )

            val buffer = ShortArray(WINDOW_SIZE)
            var consecutiveAlerts = 0
            val recordingStarted = runCatching {
                audioRecord.startRecording()
                true
            }.getOrElse { error ->
                failAndStop("Unable to start microphone recording: ${error.message}")
                false
            }

            if (!recordingStarted) {
                audioRecord.release()
                interpreter.close()
                return@thread
            }

            while (running.get()) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val score = BabyCryInference.predictCryScore(interpreter, buffer)
                    if (score >= 0.95f) {
                        consecutiveAlerts++
                    } else {
                        consecutiveAlerts = 0
                    }

                    if (consecutiveAlerts >= 3) {
                        triggerEmergency(phoneNumber)
                        consecutiveAlerts = 0
                        Thread.sleep(60_000)
                    }
                }
            }

            audioRecord.stop()
            audioRecord.release()
            interpreter.close()
        }
    }

    private fun triggerEmergency(number: String) {
        if (number.isBlank()) return

        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$number")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(callIntent)

        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(number, null, "Baby is crying. Please check immediately.", null, null)
    }

    private fun buildNotification(content: String): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Baby Cry Monitor", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Baby Cry Monitor Active")
            .setContentText(content)
            .build()
    }
}
