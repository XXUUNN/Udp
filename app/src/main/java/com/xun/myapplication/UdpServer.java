package com.xun.myapplication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * @author- gongxun;
 * @create- 2018/11/13;
 * @desc - server udp  最大允许10个客户端连接。如果客户端超过10min没有发消息，则视为断开连接，从服务端去除。
 * 实现，设置一个缓冲数据池（最大10个，包含对应客户端的数据），一个记录 客户端最近消息时间的集合，
 * 一个定时器 每30s判断客户端的最近连接状态，超过10min则去除数据缓冲池和最近记录的 对应客户端，
 * 超过10个则开线程 对新发送消息的客户端线程返回服务端连接已打上限。
 */

public class UdpServer extends Thread {

    private static final String DATA_START_TAG = "{\"data\"";
    private static final int DATA_START_TAG_LENGTH = DATA_START_TAG.length();

    private static final int DEFAULT_UDP_PORT = 50035;
    /**
     * 默认的最大的客户端缓存数
     */
    public static final int DEFAULT_MAX_CLIENT = 10;
    /**
     * 默认的最大的客户端的活跃时间 超过此时间 判定断开 清楚客户端信息缓存信息
     */
    public static final int DEFAULT_MAX_CLIENT_ACTIVE_TIME = 10 * 60 * 1000;

    private InetAddress address;
    DatagramSocket socket;
    DatagramPacket dataReceived;
    byte[] buffer;

    boolean isNeedStop = false;

    private boolean isRunning = false;

    private int cachePoolMax = DEFAULT_MAX_CLIENT;

    private int activeTimeMax = DEFAULT_MAX_CLIENT_ACTIVE_TIME;

    private int serverPort = DEFAULT_UDP_PORT;

    private ConcurrentHashMap<String, ClientCacheInfo> cacheDataArr;

    private Timer scanTimer;

    public UdpServer(String ip) throws UnknownHostException {
        super();
        buffer = new byte[256];
        cacheDataArr = new ConcurrentHashMap<>(10);
        address = InetAddress.getByName(ip);
    }

    public UdpServer(String ip,int serverPort) throws UnknownHostException  {
        super();
        buffer = new byte[256];
        cacheDataArr = new ConcurrentHashMap<>(10);
        this.serverPort = serverPort;
        address = InetAddress.getByName(ip);
    }

    public void startServer() throws SocketException {

        if (isRunning) {
            return;
        }
        isRunning = true;
        isNeedStop = false;
        socket = new DatagramSocket(serverPort,address);
        dataReceived = new DatagramPacket(buffer,256);

        start();

        startScanTimer();
    }

    /**
     * 开启客户端活跃扫描器 更新客户端缓存
     */
    private void startScanTimer() {
        scanTimer = new Timer();
        scanTimer.scheduleAtFixedRate(new ClientActiveScan(cacheDataArr,activeTimeMax),
                activeTimeMax,activeTimeMax);
    }

    public void stopServer() {
        isNeedStop = true;

        if (!socket.isClosed()) {
            socket.close();
            socket = null;
            dataReceived = null;

            cacheDataArr.clear();
        }

        scanTimer.cancel();
    }

    @Override
    public void run() {
        if (listener != null) {
            listener.onServerStart();
        }
        while (!isNeedStop) {
            try {
                socket.receive(dataReceived);

                //判断哪个客户端的数据包
                String clientTag = dataReceived.getAddress().getHostName() +":"+ dataReceived.getPort();
                ClientCacheInfo clientCacheInfo = cacheDataArr.get(clientTag);

                StringBuilder sb;
                if (clientCacheInfo != null) {
                    sb = clientCacheInfo.payloadSb;
                    clientCacheInfo.activeTime = System.currentTimeMillis();
                } else {
                    if (cacheDataArr.size() >= cachePoolMax) {
                        if (listener != null) {
                            Executors.newSingleThreadExecutor().submit(new Runnable() {
                                @Override
                                public void run() {
                                    if (!socket.isClosed()) {
                                        listener.onClientCacheRefused(dataReceived, socket);
                                    }
                                }
                            });
                        }
                        continue;
                    } else {
                        //未存储此客户端信息
                        sb = new StringBuilder();
                        cacheDataArr.put(clientTag, new ClientCacheInfo(sb,
                                System.currentTimeMillis()));
                    }
                }

                byte[] data = dataReceived.getData();
                sb.append(new String(data, 0, dataReceived.getLength()));
                int startIndex = sb.indexOf(DATA_START_TAG);
                if (startIndex >= 0) {
                    int startIndexNext = sb.indexOf(DATA_START_TAG, startIndex + DATA_START_TAG_LENGTH);
                    if (startIndexNext >= 0) {
                        //内容就在这两个中间
                        final String payload = sb.subSequence(startIndex, startIndexNext).toString();
                        if (listener != null) {
                            Executors.newSingleThreadExecutor().submit(new Runnable() {
                                @Override
                                public void run() {
                                    if (!socket.isClosed()) {
                                        listener.onMessageReceived(dataReceived, socket, payload);
                                    }
                                }
                            });
                        }
                        sb.delete(0, startIndexNext);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                isRunning = false;
                if (listener != null) {
                    listener.onServerStop(e.getMessage());
                }
            }
        }
        isRunning = false;
    }

    private OnUdpListener listener;

    public void setUdpListener(OnUdpListener listener) {
        this.listener = listener;
    }

    private static class ClientActiveScan extends TimerTask {

        private ConcurrentHashMap<String, ClientCacheInfo> map;

        private int activeTimeMax;

        public ClientActiveScan(ConcurrentHashMap<String, ClientCacheInfo> map, int activeTimeMax) {
            this.map = map;
            this.activeTimeMax = activeTimeMax;
        }

        @Override
        public void run() {
            if (map != null) {
                Iterator<String> iterator = map.keySet().iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    ClientCacheInfo info = map.get(key);
                    if (info != null) {
                        long activeTime = info.activeTime;
                        if (System.currentTimeMillis() - activeTime > activeTimeMax) {
                            //超过了 活跃时间 去除此客户端
                            map.remove(key);
                        }
                    }
                }
            }
        }
    }

    /**
     * 客户端的 payload 和 包特征信息
     */
    private static class ClientCacheInfo {
        /**
         * 数据包的信息
         */
        public StringBuilder payloadSb;

        /**
         * 最后一次包的时间（服务端时间）
         */
        public long activeTime;

        public ClientCacheInfo(StringBuilder payloadSb, long activeTime) {
            this.payloadSb = payloadSb;
            this.activeTime = activeTime;
        }
    }

    /**
     * @author- gongxun;
     * @create- 2018/11/13;
     * @desc - udp消息接收回调
     */
    public interface OnUdpListener {


        /**
         * udp服务端开始接收消息
         */
        void onServerStart();

        /**
         * udp服务端停止接收消息
         * @param stopReason udp停止的原因 正常停止值为null
         */
        void onServerStop(String stopReason);

        /**
         * udp服务端接收到了完整的一个消息
         * @param datagramPacket 包信息
         * @param socket 套接字
         * @param message payload
         */
        void onMessageReceived(DatagramPacket datagramPacket, DatagramSocket socket, String message);

        /**
         * 客户端连接数超限制 不缓存此客户端的信息
         * 可以在此方法 给客户端回送信息
         *
         * 不阻塞其他客户端的消息
         * @param dataReceived 客户端发的包
         * @param socket 套接字
         */
        void onClientCacheRefused(DatagramPacket dataReceived, DatagramSocket socket);
    }
}
