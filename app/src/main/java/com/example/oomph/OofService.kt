package com.example.oomph

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class OofService : Service() {

    private lateinit var audioManager: AudioManagerWrapper
    private lateinit var impactDetector: ImpactDetector
    private lateinit var painMode: PainMode
    private lateinit var sexyMode: SexyMode
    private lateinit var haloMode: HaloMode
    
    private var currentMode: Mode? = null
    private val CHANNEL_ID = "OofBackgroundChannel"
    private lateinit var prefs: SharedPreferences

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "threshold" -> updateThreshold()
            "mode" -> updateMode()
            "volume" -> updateVolume()
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("oof_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)

        createNotificationChannel()
        startForeground(1, createNotification())

        setupAudioManager()
        setupModes()
        setupImpactDetector()
        
        updateThreshold()
        updateMode()
        updateVolume()
    }

    private fun setupAudioManager() {
        audioManager = AudioManagerWrapper(this)
        val painSounds = (1..10).mapNotNull { getRawResId("pain_$it") }
        val sexySounds = (1..60).mapNotNull { getRawResId("sexy_$it") }
        val haloSounds = (1..9).mapNotNull { getRawResId("halo_$it") }

        audioManager.loadSounds(painSounds + sexySounds + haloSounds)

        painMode = PainMode(painSounds)
        sexyMode = SexyMode(sexySounds)
        haloMode = HaloMode(haloSounds)
    }

    private fun setupModes() {
        // Initial mode will be set in updateMode()
    }

    private fun setupImpactDetector() {
        impactDetector = ImpactDetector(
            context = this,
            onImpact = {
                currentMode?.let { mode ->
                    val soundId = mode.onImpact()
                    if (soundId != -1) {
                        audioManager.playSound(soundId)
                    }
                }
            },
            onAmplitudeUpdate = {}
        )
        impactDetector.start()
    }

    private fun updateThreshold() {
        val gValue = prefs.getFloat("threshold", 5.0f)
        impactDetector.setThreshold(gValue)
    }

    private fun updateMode() {
        val modeName = prefs.getString("mode", "pain")
        currentMode = when (modeName) {
            "sexy" -> sexyMode
            "halo" -> haloMode
            else -> painMode
        }
    }

    private fun updateVolume() {
        val volume = prefs.getFloat("volume", 1.0f)
        audioManager.setVolume(volume)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Oof Impact Detection",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Oof is active")
            .setContentText("Listening for impacts in background")
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        impactDetector.stop()
        audioManager.release()
    }

    private fun getRawResId(name: String): Int? {
        val id = resources.getIdentifier(name, "raw", packageName)
        return if (id != 0) id else null
    }
}
