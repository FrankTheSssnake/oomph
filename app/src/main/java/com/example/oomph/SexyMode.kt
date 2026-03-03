package com.example.oomph

import java.util.ArrayDeque
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow

class SexyMode(private val soundResources: List<Int>) : Mode {
    private val impactTimestamps = ArrayDeque<Long>()
    private val windowMs = 5 * 60 * 1000L // 5 minutes

    override fun onImpact(): Int {
        val now = System.currentTimeMillis()
        impactTimestamps.addLast(now)
        
        // Remove timestamps outside the 5-minute window
        while (impactTimestamps.isNotEmpty() && (now - impactTimestamps.first) > windowMs) {
            impactTimestamps.removeFirst()
        }

        val currentIntensity = getIntensity()
        
        // Map intensity to sound index. 
        // Assuming intensity 0-60, map to available sounds.
        if (soundResources.isEmpty()) return -1
        val index = (currentIntensity / 60f * (soundResources.size - 1)).toInt().coerceIn(0, soundResources.size - 1)
        return soundResources[index]
    }

    override fun getIntensity(): Float {
        val now = System.currentTimeMillis()
        
        // Remove old timestamps before calculating intensity
        while (impactTimestamps.isNotEmpty() && (now - impactTimestamps.first) > windowMs) {
            impactTimestamps.removeFirst()
        }

        var intensity = min(impactTimestamps.size, 60).toFloat()
        
        if (impactTimestamps.isNotEmpty()) {
            val secondsSinceLastImpact = (now - impactTimestamps.last) / 1000f
            // intensity *= 0.5^(seconds_since_last_impact / 30)
            intensity *= 0.5f.pow(secondsSinceLastImpact / 30f)
        }
        
        return intensity
    }
}
