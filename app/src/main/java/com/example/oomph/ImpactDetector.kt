package com.example.oomph

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

/**
 * Detects physical impacts using the accelerometer.
 * Implements an "arm and trigger" logic to avoid mid-air triggers.
 */
class ImpactDetector(
    context: Context,
    private var threshold: Float = 3.0f * 9.81f, // Default 3g
    private val cooldownMs: Long = 750,
    private val onImpact: (Float) -> Unit,
    private val onAmplitudeUpdate: (Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val highPassFilter = HighPassFilter(alpha = 0.9f)
    
    private var lastImpactTime = 0L
    private var isArmed = false
    private var peakMagnitude = 0f

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        highPassFilter.reset()
        isArmed = false
    }

    /**
     * Set threshold in g-force.
     */
    fun setThreshold(gValue: Float) {
        this.threshold = gValue * 9.81f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val filteredMagnitude = highPassFilter.filter(event.values[0], event.values[1], event.values[2])
        onAmplitudeUpdate(filteredMagnitude)

        val now = System.currentTimeMillis()

        if (!isArmed) {
            // Check if we hit the threshold to "arm" the detector
            if (filteredMagnitude > threshold && (now - lastImpactTime) > cooldownMs) {
                isArmed = true
                peakMagnitude = filteredMagnitude
                Log.d("ImpactDetector", "Armed! Magnitude: $filteredMagnitude")
            }
        } else {
            // Keep track of the highest magnitude during this impact event
            if (filteredMagnitude > peakMagnitude) {
                peakMagnitude = filteredMagnitude
            }
            
            // Trigger when the signal falls back below 50% of the threshold
            // This indicates the impact "spike" is subsiding
            if (filteredMagnitude < (threshold * 0.5f)) {
                isArmed = false
                lastImpactTime = now
                Log.d("ImpactDetector", "Impact Triggered! Peak: $peakMagnitude")
                onImpact(peakMagnitude)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
