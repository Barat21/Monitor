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

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1001
    }

    private val requiredPermissions = listOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS
    )

    private lateinit var phoneInput: EditText
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        phoneInput = findViewById(R.id.phoneInput)
        statusText = findViewById(R.id.statusText)
        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)
        val testAlertButton = findViewById<Button>(R.id.testAlertButton)

        startButton.setOnClickListener {
            startMonitoringWithPermissionCheck()
        }

        testAlertButton.setOnClickListener {
            val targetNumber = phoneInput.text.toString().trim()
            if (targetNumber.isEmpty()) {
                statusText.text = "Status: enter a phone number"
                return@setOnClickListener
            }

            val missingRequired = requiredPermissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingRequired.isNotEmpty()) {
                val missingLabels = missingRequired.joinToString(", ") { permissionLabel(it) }
                statusText.text = "Status: required permissions denied -> $missingLabels"
                return@setOnClickListener
            }

            val intent = Intent(this, CryMonitorService::class.java).apply {
                action = CryMonitorService.ACTION_TEST_ALERT
                putExtra(CryMonitorService.EXTRA_PHONE_NUMBER, targetNumber)
            }
            startService(intent)
            statusText.text = "Status: testing call + SMS"
        }

        stopButton.setOnClickListener {
            val intent = Intent(this, CryMonitorService::class.java).apply {
                action = CryMonitorService.ACTION_STOP
            }
            startService(intent)
            statusText.text = "Status: stopped"
        }
    }

    private fun startMonitoringWithPermissionCheck() {
        val targetNumber = phoneInput.text.toString().trim()
        if (targetNumber.isEmpty()) {
            statusText.text = "Status: enter a phone number"
            return
        }

        val missing = getMissingRuntimePermissions()
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSIONS_REQUEST_CODE)
            val labels = missing.joinToString(", ") { permissionLabel(it) }
            statusText.text = "Status: grant permissions -> $labels"
            return
        }

        startMonitorService(targetNumber)
    }

    private fun getMissingRuntimePermissions(): List<String> {
        val permissions = requiredPermissions.toMutableList()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
    }


    private fun permissionLabel(permission: String): String = when (permission) {
        Manifest.permission.RECORD_AUDIO -> "Microphone"
        Manifest.permission.CALL_PHONE -> "Phone"
        Manifest.permission.SEND_SMS -> "SMS"
        Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
        else -> permission.substringAfterLast('.')
    }

    private fun startMonitorService(targetNumber: String) {
        val intent = Intent(this, CryMonitorService::class.java).apply {
            action = CryMonitorService.ACTION_START
            putExtra(CryMonitorService.EXTRA_PHONE_NUMBER, targetNumber)
        }
        ContextCompat.startForegroundService(this, intent)
        statusText.text = "Status: monitoring..."
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != PERMISSIONS_REQUEST_CODE) return

        val deniedRequired = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (deniedRequired.isNotEmpty()) {
            val missingLabels = deniedRequired.joinToString(", ") { permissionLabel(it) }
            statusText.text = "Status: required permissions denied -> $missingLabels"
            return
        }

        val targetNumber = phoneInput.text.toString().trim()
        if (targetNumber.isNotEmpty()) {
            startMonitorService(targetNumber)
        } else {
            statusText.text = "Status: enter a phone number"
        }
    }
}
