package com.gamingforgood.discordbot

import com.gamingforgood.discordbot.mic.CircularBuffer2
import java.net.*

class UdpListener(port: Int) : Thread() {
    val server = DatagramSocket(port)
    lateinit var clientIpAddress: InetAddress
    var clientPort: Int = 0
    lateinit var bufferFromDiscord: CircularBuffer2

    override fun run() {
        log("udp","Udp server begin - waiting for client")

        val receiveHelloData = ByteArray(128)
        val receiveHelloPacket = DatagramPacket(receiveHelloData, receiveHelloData.size)
        while (true) {
            server.receive(receiveHelloPacket)
            val sentence = String(receiveHelloPacket.data)
            if (sentence == "hello") {
                log("udp","Client connected")
                break
            }
        }
        clientIpAddress = receiveHelloPacket.address
        clientPort = receiveHelloPacket.port

        val helloResponse = "HELLO".toByteArray()
        val helloRespPacket = createPacket(helloResponse)
        server.send(helloRespPacket)

        // send data continuously
        var sendData: ByteArray
        log("udp", "server is looping")
        while (true) {
            // TODO
            if (bufferFromDiscord.isDataReady) {
                val packet = createPacket(bufferFromDiscord.readDiscordAudio())
                server.send(packet)
            }
        }
    }

    private fun createPacket(data: ByteArray): DatagramPacket {
        return DatagramPacket(data, data.size, clientIpAddress, clientPort)
    }
}