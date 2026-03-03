package com.example.oomph

import kotlin.random.Random

class HaloMode(private val soundResources: List<Int>) : Mode {
    override fun onImpact(): Int {
        if (soundResources.isEmpty()) return -1
        return soundResources[Random.nextInt(soundResources.size)]
    }
}
