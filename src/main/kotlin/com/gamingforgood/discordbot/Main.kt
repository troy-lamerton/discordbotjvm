package com.gamingforgood.discordbot

import com.gamingforgood.discordbot.mic.AudioPlayer
import com.gamingforgood.discordbot.mic.CircularBuffer
import com.gamingforgood.discordbot.mic.CircularBuffer2
import com.gamingforgood.discordbot.mic.MicRecord
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.audio.AudioReceiveHandler
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.audio.CombinedAudio
import net.dv8tion.jda.core.audio.UserAudio
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import javax.sound.sampled.*


val discord = JDABuilder("NTUyNDk3MjI0Njk3OTA1MTg0.D3QuSw.LDA7IgupKRuYimrEA9h0sx6i9WQ")
    .addEventListener(MyListener())
    .addEventListener()
    .build()!!

fun main() {
    log("main", "start jda bot")

    // start audio relay server
    val server = UdpListener(9050)
    server.start()

    // start microphone
//    val format = AudioFormat(48000f, 16, 2, true, true)

//    val recorder = MicRecord(format, getMicrophoneLine(format)!!)
//    recorder.start()
//    recorder.join()
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
        // join test voice channel
        mgr.openAudioConnection(vchannel)
//        mgr.sendingHandler = SendMicrophone()
        mgr.setReceivingHandler(ReceiveAudio())
        log("onReady", "Receiving audio...")
    }

    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
    }
}

class ReceiveAudio : AudioReceiveHandler {
    private val buffer: CircularBuffer2 = CircularBuffer2.getBufferObject()

    override fun handleCombinedAudio(combinedAudio: CombinedAudio) {
        if (combinedAudio.users.size == 0) return // silence
        buffer.writeToBuffer(combinedAudio.getAudioData(1.0))
    }

    override fun canReceiveCombined(): Boolean {
        return true
    }

    override fun canReceiveUser(): Boolean { return false }
    override fun handleUserAudio(userAudio: UserAudio?) { }
}

class SendMicrophone : AudioSendHandler {
    private val buffer: CircularBuffer = CircularBuffer.getBufferObject()

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
