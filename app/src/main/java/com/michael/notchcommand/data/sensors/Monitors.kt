package com.michael.notchcommand.data.sensors

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.BatteryManager
import android.os.Handler
import android.util.Log
import kotlin.math.sqrt

class BatteryMonitor(
    private val context: Context,
    private val sensorHandler: Handler,
    private val onLevel: (percent: Int, isCharging: Boolean) -> Unit
) {
    private var isRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(cntx: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL

            val percent = if (level >= 0 && scale > 0) {
                (level * 100f / scale).toInt()
            } else {
                100
            }
            // Execute on sensor thread or post callback
            sensorHandler.post {
                onLevel(percent, charging)
            }
        }
    }

    fun start() {
        if (isRegistered) return
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(receiver, filter, null, sensorHandler)
            isRegistered = true
        } catch (e: Exception) {
            Log.e("BatteryMonitor", "Error registering receiver", e)
        }
    }

    fun stop() {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(receiver)
            isRegistered = false
        } catch (e: Exception) {
            Log.e("BatteryMonitor", "Error unregistering receiver", e)
        }
    }
}

class AudioLevelMonitor(
    private val context: Context,
    private val sensorHandler: Handler,
    private val onAmplitude: (Float) -> Unit
) {
    private val sampleRate = 8000
    private var recorder: AudioRecord? = null
    private var running = false
    private var lastSilenceCheckTime = 0L
    private var silenceStart = 0L
    private var isPowerSavingPause = false
    private var powerSavingResumeTime = 0L

    fun start() {
        if (running) return
        running = true
        sensorHandler.post {
            runRecordingLoop()
        }
    }

    @SuppressLint("MissingPermission")
    private fun runRecordingLoop() {
        // Safe check for RECORD_AUDIO permission before allocating resource
        val hasPermission = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Log.w("AudioLevelMonitor", "No microphone permission granted for audio visualizer")
            running = false
            return
        }

        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(1024)

            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioLevelMonitor", "AudioRecord initialization failed")
                running = false
                return
            }

            recorder?.startRecording()
            val buf = ShortArray(bufferSize)

            while (running) {
                // Doze mode + Silence gate optimization:
                val now = System.currentTimeMillis()
                if (isPowerSavingPause) {
                    if (now < powerSavingResumeTime) {
                        Thread.sleep(1000) // Sleep 1 sec before checking again
                        continue
                    } else {
                        isPowerSavingPause = false
                        // Resume recording
                        recorder?.startRecording()
                        silenceStart = 0L
                    }
                }

                val readResult = recorder?.read(buf, 0, bufferSize) ?: -1
                if (readResult > 0) {
                    var sumOfSquares = 0L
                    for (i in 0 until readResult) {
                        sumOfSquares += buf[i] * buf[i]
                    }
                    val rms = sqrt(sumOfSquares.toDouble() / readResult).toFloat()
                    val normalized = (rms / Short.MAX_VALUE).coerceIn(0f, 1f)

                    // Inform listener
                    onAmplitude(normalized)

                    // Silence gate rule:
                    // If RMS stays below 0.01f (ambient silence) for 5 seconds, pause sampling for 1 minute to save battery
                    if (normalized < 0.01f) {
                        if (silenceStart == 0L) {
                            silenceStart = now
                        } else if (now - silenceStart > 5000L) {
                            // Initiating 1-minute battery saving sleep
                            Log.d("AudioLevelMonitor", "Silence gate triggered. Sleeping 1 minute to save battery.")
                            isPowerSavingPause = true
                            powerSavingResumeTime = now + 60000L
                            recorder?.stop()
                            onAmplitude(0f)
                        }
                    } else {
                        silenceStart = 0L
                    }
                }
                Thread.sleep(100) // Poll every 100ms for responsiveness with minimal CPU utilization
            }
        } catch (e: SecurityException) {
            Log.e("AudioLevelMonitor", "Mic permission denied during start", e)
        } catch (e: Exception) {
            Log.e("AudioLevelMonitor", "Error running mic sample loop", e)
        } finally {
            cleanup()
        }
    }

    private fun cleanup() {
        try {
            recorder?.stop()
        } catch (e: Exception) {}
        try {
            recorder?.release()
        } catch (e: Exception) {}
        recorder = null
    }

    fun stop() {
        running = false
    }
}
