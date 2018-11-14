package com.xun.myapplication;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ArrayList<UdpClient> list = new ArrayList<>();
    private String ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //获取wifi服务
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //判断wifi是否开启
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
         ip = intToIp(ipAddress);

        try {
            UdpClient udpClient = new UdpClient( ip,5555);
            UdpClient udpClient1 = new UdpClient( ip,5556);
            UdpClient udpClient2 = new UdpClient( ip,5557);
            UdpClient udpClient3 = new UdpClient( ip,5558);
            UdpClient udpClient4 = new UdpClient( ip,5559);
            UdpClient udpClient5 = new UdpClient(ip,5550);
            UdpClient udpClient6 = new UdpClient(ip,5551);
            UdpClient udpClient7 = new UdpClient(ip,5552);
            UdpClient udpClient8 = new UdpClient(ip,5553);
            UdpClient udpClient9 = new UdpClient(ip,5554);
            UdpClient udpClient10 = new UdpClient(ip,5542);

            list.add(udpClient);
            list.add(udpClient1);
            list.add(udpClient2);
            list.add(udpClient3);
            list.add(udpClient4);
            list.add(udpClient5);
            list.add(udpClient6);
            list.add(udpClient7);
            list.add(udpClient8);
            list.add(udpClient9);
            list.add(udpClient10);

        } catch (Exception e) {
            e.printStackTrace();
        }

        UdpServer server = null;
        try {
            server = new UdpServer(ip,9999);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        server.setUdpListener(new UdpServer.OnUdpListener() {
            @Override
            public void onServerStart() {
                Log.e(TAG, "onServerStart: " );
            }

            @Override
            public void onServerStop(String stopReason) {
                Log.e(TAG, "onServerStop: " );
            }

            @Override
            public void onMessageReceived(final DatagramPacket datagramPacket, DatagramSocket socket, final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, datagramPacket.getAddress().getHostName()
                                +datagramPacket.getPort()+":"+message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onClientCacheRefused(DatagramPacket dataReceived, DatagramSocket socket) {
                Log.e(TAG, "onClientCacheRefused: "+dataReceived.getAddress().getHostName()
                +dataReceived.getPort()+new String(dataReceived.getData(),0,dataReceived.getLength()));
            }
        });
        try {
            server.startServer();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private String intToIp(int i) {

        return (i & 0xFF ) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ( i >> 24 & 0xFF) ;
    }

    int count = 0;
    public void onSendClick(View view) {
        list.get(count).sendMessage("{\"data\":" +
                count +
                "}",ip,9999);

        count++;

        if (count>10){
            count = 0;
        }
    }
}
