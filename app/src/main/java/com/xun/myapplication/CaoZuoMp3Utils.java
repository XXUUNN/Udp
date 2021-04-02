package com.xun.myapplication;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;


public class CaoZuoMp3Utils {
    /**
     * 返回分离出MP3文件中的数据帧的文件路径
     * 将MP3文件的ID3v2 和 ID3v1 剥掉，剩下数据帧集合
     */
    public static byte[] ID3V2Data, ID3V1Data;

    public static String fenLiData(String path) throws IOException {
        File file = new File(path);// 原文件
        String fileName = Environment.getExternalStorageDirectory() + "";
        File file1 = new File(fileName + "/01.mp3");// 分离ID3V2后的文件,这是个中间文件，最后要被删除
        File file2 = new File(fileName + "/001.mp3");// 分离id3v1后的文件
        RandomAccessFile rf = new RandomAccessFile(file, "rw");// 随机读取文件
        FileOutputStream fos = new FileOutputStream(file1);
        byte ID3[] = new byte[3];
        rf.read(ID3);
        String ID3str = new String(ID3);
        // 分离ID3v2  
        if (ID3str.equals("ID3")) {
            rf.seek(6); //第7个字节到第10个字节表示标签大小 
            byte[] ID3size = new byte[4];
            rf.read(ID3size);
            int size1 = (ID3size[0] & 0x7f) << 21;
            int size2 = (ID3size[1] & 0x7f) << 14;
            int size3 = (ID3size[2] & 0x7f) << 7;
            int size4 = (ID3size[3] & 0x7f);
            int size = size1 + size2 + size3 + size4 + 10;
            ID3V2Data = new byte[size];
            //保存
            rf.read(ID3V2Data);
            rf.seek(size);  //指针移动到标签尾巴处。
            int lens = 0;
            byte[] bs = new byte[1024 * 4];
            while ((lens = rf.read(bs)) != -1) {
                fos.write(bs, 0, lens);
            }
            fos.close();
            rf.close();
        } else {// 否则完全复制文件  
            int lens = 0;
            rf.seek(0);
            byte[] bs = new byte[1024 * 4];
            while ((lens = rf.read(bs)) != -1) {
                fos.write(bs, 0, lens);
            }
            fos.close();
            rf.close();
        }
        RandomAccessFile raf = new RandomAccessFile(file1, "rw");
        byte TAG[] = new byte[3];
        ID3V1Data = new byte[128];
        raf.seek(raf.length() - 128);//文件最后128个字节，表示id3v1  
        raf.read(TAG);
        //保存
        raf.read(ID3V1Data);
        String tagstr = new String(TAG);
        if (tagstr.equals("TAG")) {
            FileOutputStream fs = new FileOutputStream(file2);
            raf.seek(0);
            byte[] bs = new byte[(int) (raf.length() - 128)];
            raf.read(bs);
            fs.write(bs);
            raf.close();
            fs.close();
        } else {// 否则完全复制内容至file2  
            FileOutputStream fs = new FileOutputStream(file2);
            raf.seek(0);
            byte[] bs = new byte[1024 * 4];
            int len = 0;
            while ((len = raf.read(bs)) != -1) {
                fs.write(bs, 0, len);
            }
            raf.close();
            fs.close();
        }
        if (file1.exists())// 删除中间文件  
        {
            file1.delete();

        }
        return file2.getAbsolutePath();
    }

    /**
     * 分离出数据帧每一帧的大小,以及每一帧的数据，并存在list数组里面
     * 失败则返回空
     *
     * @param path
     * @return mp3帧列表
     * @throws IOException
     */
    public static List<byte[]> initMP3Frame(String path) {
        File file = new File(path);
        List<Integer> list = new ArrayList<Integer>();
        List<byte[]> dataList = new ArrayList<byte[]>();

        int preLen = 0;
        int framSize = 0;
        RandomAccessFile rad = null;
        try {
            rad = new RandomAccessFile(file, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (framSize < file.length()) {
            byte[] head = new byte[4];
            try {
                rad.seek(framSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                rad.read(head);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int bitRate = getBitRate((head[2] >> 4) & 0x0f) * 1000;
            int sampleRate = getsampleRate((head[2] >> 2) & 0x03);
            int paing = (head[2] >> 1) & 0x01;
            int len = 0;
            if (bitRate == 0 || sampleRate == 0) {
                if (preLen != 0) {
                    len = preLen;
                } else {
                    return null;
                }
            } else {
                len = 144 * bitRate / sampleRate + paing;//layer2和layer3的算法  layer1的算法有变
                preLen = len;
            }
            list.add(len);// 将数据帧的长度添加进来
            //根据长度，取出一帧量的数据
            byte[] bs = new byte[len];
            try {
                rad.read(bs);
                dataList.add(bs);
            } catch (IOException e) {
                e.printStackTrace();
            }

            framSize += len;
        }
        return dataList;
    }

    public static int initMP3Frame(String path, List<byte[]> dataList) {
        File file = new File(path);
        List<Integer> list = new ArrayList<Integer>();

        int preLen = 0;
        int framSize = 0;
        int sampleRate = 0;
        RandomAccessFile rad = null;
        try {
            rad = new RandomAccessFile(file, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (framSize < file.length()) {
            byte[] head = new byte[4];
            try {
                rad.seek(framSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                rad.read(head);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int bitRate = getBitRate((head[2] >> 4) & 0x0f) * 1000;
            sampleRate = getsampleRate((head[2] >> 2) & 0x03);
            int paing = (head[2] >> 1) & 0x01;
            int len = 0;
            if (bitRate == 0 || sampleRate == 0) {
                if (preLen != 0) {
                    len = preLen;
                } else {
                    return 0;
                }
            } else {
                len = 144 * bitRate / sampleRate + paing;//layer2和layer3的算法  layer1的算法有变
                preLen = len;
            }
            list.add(len);// 将数据帧的长度添加进来
            //根据长度，取出一帧量的数据
            byte[] bs = new byte[len];
            try {
                rad.read(bs);
                dataList.add(bs);
            } catch (IOException e) {
                e.printStackTrace();
            }
            framSize += len;
        }
        return sampleRate;
    }

    /**
     * mp3 的关键信息
     */
    public static class Mp3KeyInfo {
        /**
         * 采样率
         */
        public int sampleRate;

        /**
         * 一帧的采样个数有
         */
        public int sampleCount;

        /**
         * 采样频率
         */
        public int SamplingFrequency;

        public Mp3KeyInfo(int sampleRate, int sampleCount, int samplingFrequency) {
            this.sampleRate = sampleRate;
            this.sampleCount = sampleCount;
            SamplingFrequency = samplingFrequency;
        }
    }

    public static Mp3KeyInfo getMP3Frame(String path, List<byte[]> dataList) {
        File file = new File(path);
        byte[] head = null;
        int frameSize = 0;
        int sampleRate = 0;
        int frequency = 0;
        int sampleCount = 0;
        int version = 0;
        int layer = 0;
        int len = 0;

        RandomAccessFile rad = null;
        try {
            rad = new RandomAccessFile(file, "rw");
            head = new byte[4];
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (rad == null) {
            return null;
        }
        while (frameSize < file.length()) {
            //先读出head  四字节
            try {
                rad.seek(frameSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                rad.read(head);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //有的mp3第一帧不是音频 头的第一字节都是ff byte值为-1

            if (head[0] != -1) {
                frameSize += 4;
                continue;
            }

            //取出版本
            version = getVersion(head);
            layer = getLayer(head);
            frequency = getFrequency(head, version);
            if (frequency == 0) {
                return null;
            }
            sampleRate = getSampleRate(head, version, layer);
            if (frequency == 0) {
                return null;
            }
            sampleCount = getSampleCount(version, layer);
            if (frequency == 0) {
                return null;
            }
            int padding = getPadding(head);
            len = sampleCount * sampleRate / frequency / 8 + padding;
            if (len == 0) {
                return null;
            }
            //根据长度，取出一帧量的数据
            try {
                rad.seek(frameSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] bs = new byte[len];
            try {
                rad.read(bs);
                dataList.add(bs);
            } catch (IOException e) {
                e.printStackTrace();
            }
            frameSize += len;
        }
        //当每一帧的数据都一样
        Mp3KeyInfo info = new Mp3KeyInfo(sampleRate, sampleCount, frequency);
        return info;
    }

    /**
     * 是否有padding
     * 第三字节 7位 00000010 0x10
     *
     * @param header 头部四字节
     * @return 1 需要调整
     */
    private static int getPadding(byte[] header) {
        int padding = (header[2] & 0x10) >> 1;
        return padding;
    }

    public static final int MPEG2point5 = 0;
    public static final int UNDEFINED = 1;
    public static final int MPEG2 = 2;
    public static final int MPEG1 = 3;

    /**
     * 获取mp3的 00-MPEG 2.5   01-未定义     10-MPEG 2     11-MPEG 1
     * 第二字节的 4和5位 00011000 0x18
     *
     * @param head 四字节的 头
     */
    private static int getVersion(byte[] head) {
        int version = (head[1] & 0x18) >> 3;
        return version;
    }

    public static final int LAYER_UNDEFINED = 0;
    public static final int LAYER_3 = 1;
    public static final int LAYER_2 = 2;
    public static final int LAYER_1 = 3;

    /**
     * 获取layer层
     * 第二字节的  67位  00000110 0x06
     * 00-未定义      01-Layer 3     10-Layer 2      11-Layer 1
     *
     * @param head 四字节头
     * @return layer
     */
    private static int getLayer(byte[] head) {
        int layer = (head[1] & 0x06) >> 1;
        return layer;
    }

    /**
     * 采样率 查表
     * 横向依次(V1,L1),(V1,L2),(V1,L3),(V2,L1),(V2,L2),(V2,L3)
     */
    private static final int[][] SAMPLE_RATE_TABLE =
            {{0, 0, 0, 0, 0, 0},//0000
                    {32, 32, 32, 32, 8, 8},//0001
                    {64, 48, 40, 48, 16, 16},//0010
                    {96, 56, 48, 56, 24, 24},//0011
                    {128, 64, 56, 64, 32, 32},//0100
                    {160, 80, 64, 80, 40, 40},//0101
                    {192, 96, 80, 96, 48, 48},//0110
                    {224, 112, 96, 112, 56, 56},//0111
                    {256, 128, 112, 128, 64, 64},//1000
                    {288, 160, 128, 144, 80, 80},//1001
                    {320, 192, 160, 160, 96, 96},//1010
                    {352, 224, 192, 176, 112, 112},//1011
                    {384, 256, 224, 192, 128, 128},//1100
                    {416, 320, 256, 224, 144, 144},//1101
                    {448, 384, 320, 256, 160, 160},//1110
                    {-1, -1, -1, -1, -1, -1}//1111 不允许
            };

    /**
     * 获取采样率
     * 第三字节的 1234位 11110000 0xf0
     * 和layer version有关
     * {@link #SAMPLE_RATE_TABLE}
     *
     * @param head 四字节头
     * @return sampleRate
     */
    private static int getSampleRate(byte[] head, int version, int layer) {
        if (layer < 1) {
            return 0;
        }
        int index = (head[2] & 0xf0) >> 4;
        int column = 0;
        if (version == MPEG1) {
            //L1  L2  L3
            column += 3 - layer;
        } else if (version == MPEG2 || version == MPEG2point5) {
            //L1  L2  L3
            column += 3;
            column += 3 - layer;
        } else {
            return 0;
        }
        int sampleRate = SAMPLE_RATE_TABLE[index][column] * 1000;
        return sampleRate;
    }

    /**
     * 采样频率表
     * 横向 00 01 10 11
     * 纵向 MPEG-1 MPEG-2 MPEG-2.5
     * MPEG-1：  00-44.1kHz    01-48kHz    10-32kHz      11-未定义
     * MPEG-2：  00-22.05kHz   01-24kHz    10-16kHz      11-未定义
     * MPEG-2.5： 00-11.025kHz 01-12kHz    10-8kHz       11-未定义
     */
    public static final int[][] SAMPLE_FREQUENCY_TABLE =
            {{44100, 48000, 32000, -1},
                    {22050, 24000, 16000, -1},
                    {11025, 12000, 8000, -1}};

    /**
     * 获取采样频率
     * 第三字节 56位 00001100 0x0c
     * MPEG-1：  00-44.1kHz    01-48kHz    10-32kHz      11-未定义
     * MPEG-2：  00-22.05kHz   01-24kHz    10-16kHz      11-未定义
     * MPEG-2.5： 00-11.025kHz 01-12kHz    10-8kHz       11-未定义
     * {@link #SAMPLE_FREQUENCY_TABLE}
     *
     * @param head    四字节的头
     * @param version 编码版本
     * @return 采样频率
     */
    private static int getFrequency(byte[] head, int version) {
        //横向
        int index = (head[2] & 0x0c) >> 2;
        //纵向
        int raw = 0;
        if (version == MPEG1) {
            raw = 0;
        } else if (version == MPEG2) {
            raw = 1;
        } else if (version == MPEG2point5) {
            raw = 2;
        } else {
            return 0;
        }
        return SAMPLE_FREQUENCY_TABLE[raw][index];
    }

    /**
     * 采样数目 表格
     * 横向 mpeg1 mpeg2 mpeg2.5
     * 纵向 layer1 layer2 layer3
     */
    public static final int[][] SAMPLE_COUNT_TABLE =
            {{384, 384, 384},
                    {1152, 1152, 1152},
                    {1152, 576, 576}};

    /**
     * 获取采样数 一帧
     * {@link #SAMPLE_COUNT_TABLE}
     *
     * @return sampleCount
     */
    private static int getSampleCount(int version, int layer) {

        if (layer > 0 && layer <= 3) {
            if (version == MPEG2point5) {
                return SAMPLE_COUNT_TABLE[3 - layer][2];
            } else if (version == MPEG2) {
                return SAMPLE_COUNT_TABLE[3 - layer][1];
            } else if (version == MPEG1) {
                return SAMPLE_COUNT_TABLE[3 - layer][0];
            }
        }
        return 0;
    }


    /**
     * 返回切割后的MP3文件的路径 返回null则切割失败 开始时间和结束时间的整数部分都是秒，以秒为单位
     *
     * @param list
     * @param startTime
     * @param stopTime
     * @return
     * @throws IOException
     */
    public static String CutingMp3(String path, String name,
                                   List<Integer> list, double startTime, double stopTime)
            throws IOException {
        File file = new File(path);
        String luJing = "/storage/emulated/0/" + "HH音乐播放器/切割/";
        File f = new File(luJing);
        f.mkdirs();
        int start = (int) (startTime / 0.026);
        int stop = (int) (stopTime / 0.026);
        if ((start > stop) || (start < 0) || (stop < 0) || (stop > list.size())) {
            return null;
        } else {
            long seekStart = 0;// 开始剪切的字节的位置  
            for (int i = 0; i < start; i++) {
                seekStart += list.get(i);
            }
            long seekStop = 0;// 结束剪切的的字节的位置  
            for (int i = 0; i < stop; i++) {
                seekStop += list.get(i);
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.seek(seekStart);
            File file1 = new File(luJing + name + "(HH切割).mp3");
            FileOutputStream out = new FileOutputStream(file1);
            byte[] bs = new byte[(int) (seekStop - seekStart)];
            raf.read(bs);
            out.write(bs);
            raf.close();
            out.close();
            File filed = new File(path);
            if (filed.exists())
                filed.delete();
            return file1.getAbsolutePath();
        }

    }

    private static int getBitRate(int i) {
        int a[] = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224,
                256, 320, 0};
        return a[i];
    }

    private static int getsampleRate(int i) {
        int a[] = {44100, 48000, 32000, 0};
        return a[i];
    }

    /**
     * 返回合并后的文件的路径名,默认放在第一个文件的目录下
     *
     * @param path
     * @param path1
     * @param name
     * @return
     * @throws IOException
     */
    public static String heBingMp3(String path, String path1, String name) throws IOException {
        String fenLiData = fenLiData(path);
        String fenLiData2 = fenLiData(path1);
        File file = new File(fenLiData);
        File file1 = new File(fenLiData2);
        String luJing = "/storage/emulated/0/" + "HH音乐播放器/合并/";
        File f = new File(luJing);
        f.mkdirs();
        //生成处理后的文件  
        File file2 = new File(luJing + name + "(HH合并).mp3");
        FileInputStream in = new FileInputStream(file);
        FileOutputStream out = new FileOutputStream(file2);
        byte bs[] = new byte[1024 * 4];
        int len = 0;
        //先读第一个  
        while ((len = in.read(bs)) != -1) {
            out.write(bs, 0, len);
        }
        in.close();
        out.close();
        //再读第二个  
        in = new FileInputStream(file1);
        out = new FileOutputStream(file2, true);//在文件尾打开输出流
        len = 0;
        byte bs1[] = new byte[1024 * 4];
        while ((len = in.read(bs1)) != -1) {
            out.write(bs1, 0, len);
        }
        in.close();
        out.close();
        if (file.exists()) file.delete();
        if (file1.exists()) file1.delete();
        return file2.getAbsolutePath();
    }
}  