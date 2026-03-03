package com.example.oomph

interface Mode {
    /**
     * Called when an impact is detected.
     * @return The ID of the sound to play from SoundPool.
     */
    fun onImpact(): Int

    /**
     * Optional: Returns current intensity for display (used in SexyMode).
     */
    fun getIntensity(): Float = 0f
}
