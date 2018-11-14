package com.xun.myapplication;

import android.app.Application;

import com.facebook.stetho.Stetho;

/**
 * @author- Administrator;
 * @create- 2018/11/12;
 * @desc -
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Stetho.initializeWithDefaults(this);
    }
}
