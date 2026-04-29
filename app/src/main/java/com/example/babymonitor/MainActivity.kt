package com.example.babymonitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val statusText = findViewById<TextView>(R.id.statusText)
        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)

        startButton.setOnClickListener {
            requestRuntimePermissions()
            val targetNumber = phoneInput.text.toString().trim()
            if (targetNumber.isNotEmpty()) {
                val intent = Intent(this, CryMonitorService::class.java).apply {
                    action = CryMonitorService.ACTION_START
                    putExtra(CryMonitorService.EXTRA_PHONE_NUMBER, targetNumber)
                }
                ContextCompat.startForegroundService(this, intent)
                statusText.text = "Status: monitoring..."
            } else {
                statusText.text = "Status: enter a phone number"
            }
        }

        stopButton.setOnClickListener {
            val intent = Intent(this, CryMonitorService::class.java).apply {
                action = CryMonitorService.ACTION_STOP
            }
            startService(intent)
            statusText.text = "Status: stopped"
        }
    }

    private fun requestRuntimePermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1001)
        }
    }
}
