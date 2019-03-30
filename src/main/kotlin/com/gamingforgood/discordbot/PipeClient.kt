package com.gamingforgood.discordbot

import java.io.IOException
import java.io.RandomAccessFile

class PipeClient(private val pipeName: String) : Thread() {

    protected lateinit var pipe: NamedPipe

    override fun run() {
        try {
            // Connect to the pipe
            pipe = NamedPipe(pipeName)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        runner@ while (pipe.connected) {
            // read commands
            val line = pipe.readLine()
            println("Pipe received: $line")
            val parts = line.split(";")
            val command = parts[0]
            val data = if (parts.size > 1) parts[1] else ""
            val message = handleCommonCommands(command, data)
//            handleMessage(message)
        }

    }

    fun send(message: Message) {
        pipe.send(message)
    }

    fun ping() {
        pipe.ping()
    }

//    abstract fun handleMessage(message: Message)

    private fun handleCommonCommands(command: String, contents: String): Message {
        val message = Message(command, contents)
        when (message.command) {
            "log" -> {
                log("pipe", "read: ${message.contents}")
            }

            "ping" -> {
                log("pipe", "ping, responding pong")
                pipe.pong()
            }

            "close" -> {
                pipe.close()
            }
        }
        return message
    }
}

class Message(var command: String, val contents: String) {
    var arrayContents: List<String>? = null

    init {
        if (command == "array") {
            // first item in the array is the real command
            val list = contents.split(",")
            command = list.first()
            arrayContents = list.takeLast(list.size - 1)
            log("pipe", "pipe got arrayContents: [ ${list.joinToString(", ")} ]")
        }
    }
}

class NamedPipe(name: String) : RandomAccessFile("\\\\.\\pipe\\$name", "rw") {
    var connected: Boolean = false
        private set

    init {
        connected = true
    }

    fun send(command: String) {
        writeString(command)
    }
    fun send(command: String, data: String) {
        writeString("$command;$data")
    }
    fun send(command: String, data: Array<String>) {
        send(command, data.joinToString(","))
    }

    fun send(message: Message) {
        if (message.arrayContents != null) {
            send("array", arrayOf(message.command, *message.arrayContents!!.toTypedArray()))
        } else {
            send(message.command, message.contents)
        }
    }

    fun ping() {
        send("ping")
    }

    fun pong() {
        send("pong")
    }

    private fun writeString(string: String) {
        try {
            writeChars("$string\n")
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    override fun close() {
        try {
            super.close()
        } catch (ex: IOException) {
            ex.printStackTrace()
        } finally {
            connected = false
        }
    }
}