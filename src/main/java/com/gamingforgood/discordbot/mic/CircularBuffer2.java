package com.gamingforgood.discordbot.mic;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Discord bot writes data to this buffer
 * Udp server reads it and forwards the data to unity dissonance
 */
public class CircularBuffer2 {

    static final short BUFFER_SIZE = 3840; // 48000 * 16 * 2 / 8 / (1000/20)

    private final int QUEUE_CAPACITY = 30;
    private static CircularBuffer2 buf = null;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private LinkedBlockingQueue<byte[]> buffersQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY); // x 20ms = length of queue
    private short write_index = 0;

    public boolean isDataReady() {
        return buffersQueue.size() >= 1;
    }

    private CircularBuffer2() {}

    public static CircularBuffer2 getBufferObject() {
        if(buf == null) {
            buf = new CircularBuffer2();
        }
        return buf;
    }

    public synchronized void writeToBuffer(byte[] data) {
        for (byte datum : data) {
            if (write_index >= BUFFER_SIZE) {
                if (buffersQueue.size() == QUEUE_CAPACITY) {
                    // drop the oldest buffer
                    buffersQueue.remove();
                    // and also drop this one (don't add it)
                }
                buffersQueue.add(buffer.clone());
                write_index = 0;
            }
            buffer[write_index] = datum;
            write_index++;
        }
    }

    public synchronized byte[] readDiscordAudio() {
        return buffersQueue.poll();
    }

}

