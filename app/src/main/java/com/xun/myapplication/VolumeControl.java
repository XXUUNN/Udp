package com.xun.myapplication;

/**
 * @author- gongxun;
 * @create- 2018/11/15;
 * @desc - 声音控制
 */
public class VolumeControl {
    private static float[] volumeFactor = new float[50];

    static {
        volumeFactor[0] = 0;
        for (int i = 1; i < 50; i ++) {
            volumeFactor[i] = (float) Math.pow(10, (i*2 - 50) / 20);
        }
    }

    /**
     *
     * @param pcm 原pcm数据
     * @param len pcm的有效长度
     * @param level 音量等级 （0-50） 25为原声 0无声音 50最大
     */
    private static void agentVolume(short[] pcm,int len,int level){
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
