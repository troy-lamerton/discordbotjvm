package com.gamingforgood.discordbot

import java.net.*

class UdpListener(port: Int) : Thread() {
    private val server = DatagramSocket(port)
    private lateinit var clientIpAddress: InetAddress
    private var clientPort: Int = 0
    private val bufferFromDiscord = FromDiscordBuffer.bufferObject

    var running = true

    override fun run() {
        log("udp","Udp server begin - waiting for client")

        val receiveHelloData = ByteArray(128)
        val receiveHelloPacket = DatagramPacket(receiveHelloData, receiveHelloData.size)
        while (running) {
            server.receive(receiveHelloPacket)
            val sentence = String(receiveHelloPacket.data).trim()
            if (sentence.startsWith("hello")) {
                log("udp","Client connected")
                break
            } else {
                log("udp","Client should send 'hello' first! not '$sentence'")
            }
        }
        if (!running) return

        clientIpAddress = receiveHelloPacket.address
        clientPort = receiveHelloPacket.port

        val helloResponse = "HELLO".toByteArray()
        val helloRespPacket = createPacket(helloResponse)
        server.send(helloRespPacket)

        // send data continuously
        log("udp", "Sending all audio")
        while (running) {
            // TODO
            if (bufferFromDiscord.isDataReady) {
                val packet = createPacket(bufferFromDiscord.readDiscordAudio())
                server.send(packet)
            }
        }
    }

    fun disconnect() {
        running = false
    }

    private fun createPacket(data: ByteArray): DatagramPacket {
        return DatagramPacket(data, data.size, clientIpAddress, clientPort)
    }
}