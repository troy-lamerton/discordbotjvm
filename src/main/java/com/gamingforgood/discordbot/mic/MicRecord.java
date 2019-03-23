package com.gamingforgood.discordbot.mic;


import javax.sound.sampled.*;

public class MicRecord extends Thread {

    private AudioFormat format;
    private TargetDataLine targetDataLine;

    public MicRecord(AudioFormat format, TargetDataLine line) {
        this.format = format;
        this.targetDataLine = line;
    }

    public void run() {
        CircularBuffer buf;
        try {
            targetDataLine.open(format, CircularBuffer.BUFFER_SIZE * 2);
            targetDataLine.start();
        }
        catch(Exception e) {
            System.out.println(e.toString());
        }

        int dataRead = -1;
        System.out.println("line buffer size is " + targetDataLine.getBufferSize());
        byte[] targetData = new byte[targetDataLine.getBufferSize() / 5];
        final int dataReadLength = targetData.length;
        buf = CircularBuffer.getBufferObject();

        while(true) {
            try {
                //read from mic
                dataRead = targetDataLine.read(targetData, 0, dataReadLength);
                if (dataRead == -1) {
                    System.out.println("Error reading microphone input");
                } else {
                    buf.writeToBuffer(targetData);
                }
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
    }
}

