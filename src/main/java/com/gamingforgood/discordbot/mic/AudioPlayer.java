package com.gamingforgood.discordbot.mic;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class AudioPlayer extends Thread {

    AudioFormat format;
    SourceDataLine speakerLine;

    public AudioPlayer(AudioFormat format, SourceDataLine speakerLine) {
        this.format = format;
        this.speakerLine = speakerLine;
//        DataLine.Info info  = new DataLine.Info(SourceDataLine.class, format);
//        try {
//            this.speakerLine = (SourceDataLine) AudioSystem.getLine(info);
//        } catch (Exception ex) {
//            System.out.println("Erorr open speaker");
//            ex.printStackTrace();
//            System.exit(1);
//        }
    }

    public void run() {

        try {
            speakerLine.open(format);
            speakerLine.start();
        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(1);
        }

        CircularBuffer2 buf = CircularBuffer2.getBufferObject();

        while(true) {
            try {
                if (buf.isDataReady()) {
                    byte[] tempBuf = buf.readDiscordAudio();
                    // play audio
                    int length = tempBuf.length;
                    speakerLine.write(tempBuf, 0, length);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(2);
            }
        }
    }
}

