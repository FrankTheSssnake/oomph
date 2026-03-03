package com.example.oomph

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.SoundPool
import android.util.Log

class AudioManagerWrapper(private val context: Context) {
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<Int, Int>() // Resource ID to Sound ID
    private val durationMap = mutableMapOf<Int, Long>() // Resource ID to Duration (ms)
    private var volume = 1.0f
    
    private var lastPlayTime = 0L
    private var currentSoundDuration = 0L

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Set maxStreams to 1 to help prevent overlaps at the system level if desired,
        // but we'll also manage it manually for precise control.
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()
    }

    /**
     * Preloads sounds and retrieves their durations.
     */
    fun loadSounds(resourceIds: List<Int>) {
        val retriever = MediaMetadataRetriever()
        resourceIds.forEach { resId ->
            try {
                // Load into SoundPool
                val soundId = soundPool.load(context, resId, 1)
                soundMap[resId] = soundId

                // Get duration
                val afd = context.resources.openRawResourceFd(resId)
                retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                durationMap[resId] = duration
                afd.close()
            } catch (e: Exception) {
                Log.e("AudioManager", "Error loading sound $resId", e)
            }
        }
        retriever.release()
    }

    /**
     * Plays a sound only if the previous one has finished.
     */
    fun playSound(resId: Int) {
        val now = System.currentTimeMillis()
        val elapsed = now - lastPlayTime

        if (elapsed < currentSoundDuration) {
            Log.d("AudioManager", "Still playing previous sound, skipping. Remaining: ${currentSoundDuration - elapsed}ms")
            return
        }

        val soundId = soundMap[resId]
        val duration = durationMap[resId] ?: 0L

        if (soundId != null) {
            soundPool.play(soundId, volume, volume, 1, 0, 1f)
            lastPlayTime = now
            currentSoundDuration = duration
        } else {
            Log.w("AudioManager", "Sound resource $resId not loaded")
        }
    }

    fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0.0f, 1.0f)
    }

    fun release() {
        soundPool.release()
    }
}
