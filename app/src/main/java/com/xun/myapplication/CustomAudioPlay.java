package com.xun.myapplication;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.text.util.Linkify;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author- gongxun;
 * @create- 2018/11/23;
 * @desc - 实时播放音频数据
 */
public class CustomAudioPlay {

    private AudioTrack audioTrack;
    private ExecutorService service;
    private boolean running;
    private boolean isPrepared = false;
    /**
     * 写入的播放数据池
     */
    private LinkedBlockingQueue<byte[]> queue;

    public void prepare() {
        int minBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, minBufferSize * 2, AudioTrack.MODE_STREAM);

        service = Executors.newFixedThreadPool(3);

        queue = new LinkedBlockingQueue<>(10);

        isPrepared = true;
    }

    public void startPlay() {

        if (!isPrepared){
            throw RuntimeException("please use prepare() before startPlay()");
            return;
        }

        audioTrack.play();

        audioTrack.stop();

        running = true;

        service.submit(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        byte[] buffer = queue.take();
                        audioTrack.write(buffer,0,buffer.length);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                running = false;
            }
        });
    }

    public void writeData(byte[] data){
        writeData(data,data.length);
    }

    public void writeData(byte[] data,int len){
        if (running && isPrepared){
            byte[] copyData = Arrays.copyOf(data,len);
            boolean isOk = queue.offer(copyData);
            if (!isOk){
                queue.poll();
                queue.offer(copyData);
            }
        }
    }

}
