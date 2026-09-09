package com.example.beatpulse.data.sync

import kotlinx.coroutines.launch

object EcosystemManager {
    val server = EcosystemServer()
    val discovery = NetworkDiscovery()
    
    private var isStarted = false
    
    fun startEcosystem() {
        if (isStarted) return
        isStarted = true
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            server.startServer()
            discovery.start()
        }
    }
    
    fun stopEcosystem() {
        if (!isStarted) return
        isStarted = false
        server.stopServer()
        discovery.stop()
    }
}
