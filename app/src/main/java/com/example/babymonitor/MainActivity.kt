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

        startButton.setOnClickListener {
            startMonitoringWithPermissionCheck()
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
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1001)
            statusText.text = "Status: waiting for permissions"
            return
        }

        startMonitorService(targetNumber)
    }

    private fun getMissingRuntimePermissions(): List<String> {
        val permissions = requiredPermissions.toMutableList()
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
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

        val permissionResults = permissions.mapIndexed { index, permission ->
            permission to grantResults.getOrNull(index)
        }.toMap()

        val requiredDenied = requiredPermissions.any { permission ->
            permissionResults[permission] != PackageManager.PERMISSION_GRANTED
        }

        if (requiredDenied) {
        if (requestCode != 1001) return

        val denied = grantResults.any { it != PackageManager.PERMISSION_GRANTED }
        if (denied) {
            statusText.text = "Status: required permissions denied"
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
