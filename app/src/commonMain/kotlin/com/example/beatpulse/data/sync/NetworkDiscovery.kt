package com.example.beatpulse.data.sync

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.ktor.utils.io.core.*

class NetworkDiscovery(private val deviceName: String = "DuWave Device") {
    
    private val discoveryPort = 8081
    private val beaconMessagePrefix = "DUWAVE_NODE|"
    private val pairMessagePrefix = "DUWAVE_PAIR|"
    private val pairOkMessagePrefix = "DUWAVE_PAIR_OK|"
    private val uniqueId = kotlin.random.Random.nextInt(10000, 99999).toString()
    val currentPin = kotlin.random.Random.nextInt(1000, 9999).toString()
    
    private val _discoveredDevices = MutableStateFlow<Map<String, String>>(emptyMap()) // Map of IP to DeviceName
    val discoveredDevices: StateFlow<Map<String, String>> = _discoveredDevices.asStateFlow()
    
    private var scope: CoroutineScope? = null
    
    fun start() {
        if (scope != null) return
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        // Shared Socket for both Listener and Broadcaster
        scope?.launch {
            try {
                val selectorManager = SelectorManager(Dispatchers.IO)
                val socket = aSocket(selectorManager).udp().bind(io.ktor.network.sockets.InetSocketAddress("0.0.0.0", discoveryPort)) {
                    broadcast = true
                }
                
                // Broadcaster Job
                launch {
                    val broadcastAddress = io.ktor.network.sockets.InetSocketAddress("255.255.255.255", discoveryPort)
                    while (isActive) {
                        try {
                            val packet = buildPacket {
                                writeText("$beaconMessagePrefix$deviceName|$uniqueId")
                            }
                            socket.send(Datagram(packet, broadcastAddress))
                        } catch (e: Exception) {
                            e.printStackTrace() // Ignore unreachable network exceptions and try again
                        }
                        delay(3000) // Announce every 3 seconds
                    }
                }
                
                // Listener Loop
                while (isActive) {
                    try {
                        val datagram = socket.receive()
                        val msg = datagram.packet.readText()
                        if (msg.startsWith(beaconMessagePrefix)) {
                            val payload = msg.removePrefix(beaconMessagePrefix)
                            val parts = payload.split("|")
                            val name = parts.firstOrNull() ?: "Unknown"
                            val senderId = parts.getOrNull(1)
                            
                            if (senderId == uniqueId) continue
                            
                            val senderAddress = datagram.address as io.ktor.network.sockets.InetSocketAddress
                            val addressString = senderAddress.hostname
                            
                            val current = _discoveredDevices.value.toMutableMap()
                            if (current[addressString] != name) {
                                current[addressString] = name
                                _discoveredDevices.value = current
                                
                                // Reply directly to the sender's discovery port via Unicast
                                try {
                                    val replyPacket = io.ktor.utils.io.core.buildPacket {
                                        writeText("$beaconMessagePrefix$deviceName|$uniqueId")
                                    }
                                    val replyAddress = io.ktor.network.sockets.InetSocketAddress(senderAddress.hostname, discoveryPort)
                                    socket.send(Datagram(replyPacket, replyAddress))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } else if (msg.startsWith(pairMessagePrefix)) {
                            val payload = msg.removePrefix(pairMessagePrefix)
                            val parts = payload.split("|")
                            val targetPin = parts.firstOrNull() ?: ""
                            val senderId = parts.getOrNull(1)
                            
                            if (senderId == uniqueId) continue
                            
                            if (targetPin == currentPin) {
                                val senderAddress = datagram.address as io.ktor.network.sockets.InetSocketAddress
                                try {
                                    val replyPacket = buildPacket {
                                        writeText("$pairOkMessagePrefix$deviceName|$uniqueId")
                                    }
                                    socket.send(Datagram(replyPacket, senderAddress))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } else if (msg.startsWith(pairOkMessagePrefix)) {
                            val payload = msg.removePrefix(pairOkMessagePrefix)
                            val parts = payload.split("|")
                            val name = parts.firstOrNull() ?: "Unknown"
                            val senderId = parts.getOrNull(1)
                            
                            if (senderId == uniqueId) continue
                            
                            val senderAddress = datagram.address as io.ktor.network.sockets.InetSocketAddress
                            val addressString = senderAddress.hostname
                            
                            val current = _discoveredDevices.value.toMutableMap()
                            if (current[addressString] != name) {
                                current[addressString] = name
                                _discoveredDevices.value = current
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun stop() {
        scope?.cancel()
        scope = null
    }

    fun pairWithPin(pin: String, onComplete: ((Boolean) -> Unit)? = null) {
        scope?.launch {
            try {
                val selectorManager = SelectorManager(Dispatchers.IO)
                val tempSocket = aSocket(selectorManager).udp().bind(io.ktor.network.sockets.InetSocketAddress("0.0.0.0", 0)) {
                    broadcast = true
                }
                
                launch {
                    val packetData = "$pairMessagePrefix$pin|$uniqueId"
                    val broadcastAddress = io.ktor.network.sockets.InetSocketAddress("255.255.255.255", discoveryPort)
                    try {
                        tempSocket.send(Datagram(buildPacket { writeText(packetData) }, broadcastAddress))
                    } catch (e: Exception) {}
                    
                    val subnets = listOf("192.168.100.", "192.168.1.", "192.168.0.", "10.0.0.", "172.16.0.", "192.168.18.")
                    for (subnet in subnets) {
                        for (i in 1..254) {
                            if (!isActive) return@launch
                            val addr = io.ktor.network.sockets.InetSocketAddress("$subnet$i", discoveryPort)
                            try {
                                tempSocket.send(Datagram(buildPacket { writeText(packetData) }, addr))
                            } catch (e: Exception) {}
                        }
                    }
                }
                
                // Wait for pair OK response on this temp socket (hole punch)
                val result = withTimeoutOrNull(3000L) {
                    var success = false
                    while(isActive) {
                        val datagram = tempSocket.receive()
                        val msg = datagram.packet.readText()
                        if (msg.startsWith(pairOkMessagePrefix)) {
                            val payload = msg.removePrefix(pairOkMessagePrefix)
                            val parts = payload.split("|")
                            val name = parts.firstOrNull() ?: "Unknown"
                            val senderId = parts.getOrNull(1)
                            
                            if (senderId != uniqueId) {
                                val senderAddress = datagram.address as io.ktor.network.sockets.InetSocketAddress
                                val addressString = senderAddress.hostname
                                
                                val current = _discoveredDevices.value.toMutableMap()
                                current[addressString] = name
                                _discoveredDevices.value = current
                                success = true
                                break
                            }
                        }
                    }
                    success
                }
                
                tempSocket.close()
                onComplete?.invoke(result == true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete?.invoke(false)
            }
        }
    }
}
