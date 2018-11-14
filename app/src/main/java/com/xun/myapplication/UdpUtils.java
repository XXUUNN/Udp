package com.xun.myapplication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * @author- gongxun;
 * @create- 2018/11/13;
 * @desc -udp工具
 */
public class UdpUtils {

    /**
     * 发送packet 到对应 地址
     * @param socket socket
     * @param address 地址
     * @param port 端口
     * @param data 数据
     * @return true 成功
     */
    public static boolean sendPacket(DatagramSocket socket, InetAddress address,int port, byte[] data){
        boolean isSuccessful = false;
        if (!socket.isClosed()){
            DatagramPacket packet = new DatagramPacket(data, 0, data.length, address, port);
            try {
                socket.send(packet);
                isSuccessful = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return isSuccessful;
    }

}
