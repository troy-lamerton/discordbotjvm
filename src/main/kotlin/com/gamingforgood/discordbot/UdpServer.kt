package com.gamingforgood.discordbot

import java.net.*

class UdpListener(port: Int) : Thread() {
    val socket = DatagramSocket(port)
    private var clientIpAddress: InetAddress? = null
    private var clientPort: Int = -1
//    private val bufferFromDiscord = FromDiscordBuffer.bufferObject
    private val bufferToDiscord = ToDiscordBuffer.bufferObject
    private var didWarn = false
    var running = true
    val clientIsConnected
        get() = clientIpAddress != null

    fun sendToClient(data: ByteArray) {
        if (!server.clientIsConnected) {
//            log("warn", "No client is connected to udp server, dropping ${data.size} bytes")
            return
        }
        val packet = CreatePacket(data)
        socket.send(packet)
    }

    override fun run() {

        val receiveHelloData = ByteArray(128)
        val receiveHelloPacket = DatagramPacket(receiveHelloData, receiveHelloData.size)
        while (running) {
            log("udp","Udp socket begin - waiting for client")
            socket.receive(receiveHelloPacket)
            val sentence = String(receiveHelloPacket.data).trim()
            if (sentence.startsWith("hello")) {
                log("udp","Client connected")
                break
            } else if (!didWarn) {
                didWarn = true
                log("udp","Client should send 'hello' first! not '$sentence'")
            }
        }
        if (!running) return

        clientIpAddress = receiveHelloPacket.address
        clientPort = receiveHelloPacket.port

        val helloResponse = "HELLO".toByteArray()
        sendToClient(helloResponse)

        // send data continuously
        log("udp", "Forwarding audio to discord")
        val pcmSample = ByteArray(1920 * 2) // constant set in unity C# project
        val pcmSamplePacket = DatagramPacket(pcmSample, pcmSample.size)
        while (running) {
            socket.receive(pcmSamplePacket) // read bytes from unity into the packet
            bufferToDiscord.writeToBuffer(pcmSamplePacket) // write bytes to buffer for JDA to collect
        }
    }

    fun disconnect() {
        running = false
    }

    private fun CreatePacket(data: ByteArray): DatagramPacket {
        return DatagramPacket(data, data.size, clientIpAddress, clientPort)
    }
}