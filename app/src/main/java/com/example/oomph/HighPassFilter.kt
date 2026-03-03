package com.example.oomph

import kotlin.math.sqrt

/**
 * High-pass filter to remove gravity from accelerometer data.
 * Formula: filtered = alpha * (prev_filtered + current - prev_raw)
 */
class HighPassFilter(private val alpha: Float = 0.9f) {
    private var prevRawX = 0f
    private var prevRawY = 0f
    private var prevRawZ = 0f

    private var prevFilteredX = 0f
    private var prevFilteredY = 0f
    private var prevFilteredZ = 0f

    /**
     * Processes raw accelerometer values and returns the magnitude of the filtered acceleration.
     */
    fun filter(x: Float, y: Float, z: Float): Float {
        prevFilteredX = alpha * (prevFilteredX + x - prevRawX)
        prevFilteredY = alpha * (prevFilteredY + y - prevRawY)
        prevFilteredZ = alpha * (prevFilteredZ + z - prevRawZ)

        prevRawX = x
        prevRawY = y
        prevRawZ = z

        // Return the magnitude of the filtered acceleration vector
        return sqrt(prevFilteredX * prevFilteredX + prevFilteredY * prevFilteredY + prevFilteredZ * prevFilteredZ)
    }

    fun reset() {
        prevRawX = 0f
        prevRawY = 0f
        prevRawZ = 0f
        prevFilteredX = 0f
        prevFilteredY = 0f
        prevFilteredZ = 0f
    }
}
