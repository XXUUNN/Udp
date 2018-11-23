package com.xun.myapplication;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author- gongxun;
 * @create- 2018/11/15;
 * @desc - 可调节录制音量大小的录制类
 */
public class CustomAudioRecord {
    private static float[] volumeFactor = new float[50];

    static {
        volumeFactor[0] = 0;
        for (int i = 1; i < 50; i ++) {
            volumeFactor[i] = (float) Math.pow(10, (i*2 - 50) / 20);
        }
    }

    private byte[] bufferInByte;

    private AudioRecord audioRecord;

    private short[] buffer;

    private int bufferSizeInRead;
    private int bufferSizeInReadByte;

    private ExecutorService service;

    public CustomAudioRecord() {
        int minBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        this.audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                44100, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, minBufferSize * 2);

        bufferSizeInRead = minBufferSize / 2;
        bufferSizeInReadByte = minBufferSize;
        buffer = new short[bufferSizeInRead];
        bufferInByte = new byte[bufferSizeInReadByte];
        service = Executors.newSingleThreadExecutor();
    }

    private boolean isNeedRecord;

    private Future task;

    public void stopRecording(){
        isNeedRecord = false;
        audioRecord.stop();
        if (task != null){
            if (!task.isCancelled()){
                task.cancel(false);
            }
        }
    }
    public void startRecording(final File file) {
        isNeedRecord = true;
        audioRecord.startRecording();
        task = service.submit(new Runnable() {
            @Override
            public void run() {
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(file);
                    while (isNeedRecord) {
                        int read = audioRecord.read(bufferInByte, 0, bufferSizeInRead);
                        if (read < 0) {
                            continue;
                        }
                        Log.e("ffffffffff", "1111run: ffff"+read );
                        outputStream.write(bufferInByte, 0, read);
                        Log.e("ffffffffff", "222run: ffff"+read );
                        outputStream.flush();
                        Log.e("ffffffffff", "333run: ffff"+read );
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    if (outputStream!= null){
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public int level = 25;

    private void control(short[] pcm,int len,int level){
        float factor = volumeFactor[level];
        for (int i = 0; i < len; i++) {
            float temp = pcm[i] * factor;
            if (temp>32767){
                temp = 32767;
            }
            if (temp<-32768){
                temp = -32768;
            }
            pcm[i] = (byte) temp;
        }
    }

}
