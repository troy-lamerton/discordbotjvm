package com.gamingforgood.discordbot.mic;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class CircularBuffer {

    static final short BUFFER_SIZE = 3840; // 48000 * 16 * 2 / 8 / (1000/20)

    private final int QUEUE_CAPACITY = 30;
    private static CircularBuffer buf = null;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private LinkedBlockingQueue<byte[]> buffersQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY); // x 20ms = length of queue
    private short write_index = 0;

    public boolean isDataReady() {
        return buffersQueue.size() >= 1;
    }

    private CircularBuffer() {}

    public static CircularBuffer getBufferObject() {
        if(buf == null) {
            buf = new CircularBuffer();
        }
        return buf;
    }

    synchronized void writeToBuffer(byte[] data) {
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

    public synchronized byte[] read20MsOfAudio() {
        return buffersQueue.poll();
    }

}

