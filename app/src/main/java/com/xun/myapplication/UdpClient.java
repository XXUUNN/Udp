package com.xun.myapplication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

/**
 * @author- gongxun;
 * @create- 2018/11/13;
 * @desc - udp客户端
 */
public class UdpClient {
    private DatagramSocket socket;

    public UdpClient(String clientIp, int clientPort) throws UnknownHostException, SocketException {
        InetAddress address = InetAddress.getByName(clientIp);
        socket = new DatagramSocket(clientPort, address);
    }

    public UdpClient (int clientPort) throws SocketException {
        socket = new DatagramSocket(clientPort);
    }

    public void sendMessage(final String msg, final String serverIp, final int serverPort) {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final InetAddress address = InetAddress.getByName(serverIp);
                    byte[] bytes = msg.getBytes("utf-8");
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, serverPort);
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
