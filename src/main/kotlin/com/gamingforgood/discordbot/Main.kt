package com.gamingforgood.discordbot

import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.audio.AudioReceiveHandler
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.audio.CombinedAudio
import net.dv8tion.jda.core.audio.UserAudio
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


val discord = JDABuilder("NTUyNDk3MjI0Njk3OTA1MTg0.D3QuSw.LDA7IgupKRuYimrEA9h0sx6i9WQ")
    .addEventListener(MyListener())
    .addEventListener()
    .build()!!

val server = UdpListener(9050)

fun main(args: Array<String>) {
    log("main", "DBot started with ${args.size} args: ${args.joinToString(", ")}")

    if (args.isEmpty()) {
        throw IllegalArgumentException("No program args! You must specify the pipe name like: java -jar dbot.jar pipe1")
    }

    listenOnPipe(args[0])

    // start audio relay socket
    server.start()
}

fun listenOnPipe(name: String) {
    val pipe = PipeClient(name)
    pipe.start()

    val stdin = Scanner(System.`in`)
    while(true) {
        if (stdin.hasNext()) {
            val line = stdin.nextLine()
            if (line == "ping") {
                pipe.ping()
            }
        }
    }
}

class MyListener : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        val message = event.message
        if (message.contentRaw.startsWith("p")) {
            val channel = event.channel
            channel.sendMessage("Pong!")
                .queue() // Important to call .queue() on the RestAction returned by sendMessage(...)
        }
    }

    override fun onReady(event: ReadyEvent?) {
        val testGuild = discord.guilds.find { guild -> guild.name == "best_testing_server" }
        val vchannel = testGuild!!.getVoiceChannelById(551446224000253973)
        val mgr = testGuild.audioManager

        mgr.sendingHandler = SendRoomAudio() // must be set to enable receiving audio
        mgr.setReceivingHandler(ReceiveDiscordAudio())

        // join voice channel
        mgr.openAudioConnection(vchannel)
    }

    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
    }
}

class ReceiveDiscordAudio : AudioReceiveHandler {

    /**
     * Mix discords stereo big endian audio to mono channel little endian
     * And send it to the unity udp client
     */
    override fun handleCombinedAudio(combinedAudio: CombinedAudio) {
        if (combinedAudio.users.size == 0) return
        val discordData = combinedAudio.getAudioData(1.0)

        // raw stereo data from discord, see this.INPUT_FORMAT
        val stereoBuffer = ByteBuffer.allocate(discordData.size)
        stereoBuffer.put(discordData)

        // mono buffer in little endian - the resampler in C# needs little endian
        val monoBufferLE = ByteBuffer.allocate(discordData.size / 2)
        monoBufferLE.order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until discordData.size step 4) {
            // combine stereo channels into one
            val left = stereoBuffer.getShort(i)
            val right = stereoBuffer.getShort(i + 2)
            val mixed: Int = (left + right) / 2

            monoBufferLE.putShort(i / 2, mixed.toShort())
        }
        val monoBytes = ByteArray(monoBufferLE.capacity())
        monoBufferLE.get(monoBytes)

        require(monoBytes.size == discordData.size / 2)
        server.sendToClient(monoBytes)
    }

    override fun canReceiveCombined(): Boolean {
        return true
    }

    override fun canReceiveUser(): Boolean { return false }
    override fun handleUserAudio(userAudio: UserAudio?) { }
}

class SendRoomAudio : AudioSendHandler {
    private val buffer: ToDiscordBuffer = ToDiscordBuffer.bufferObject

    override fun provide20MsAudio(): ByteArray {
        return buffer.read20MsOfAudio()
    }

    override fun canProvide(): Boolean {
        return buffer.isDataReady
    }

    override fun isOpus(): Boolean {
        return false
    }
}
