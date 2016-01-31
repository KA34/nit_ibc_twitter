package com.ka34.nit_ibc_twitter;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import java.util.Timer;
import java.util.TimerTask;

public class InfoService extends Service {

    final String url = "http://www.ibaraki-ct.ac.jp/?page_id=501";


    private Timer timer = null;
    Handler mHandler = new Handler();


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("service", "onStartCommand");

        timer = new Timer();

        timer.schedule( new TimerTask(){
            @Override
            public void run(){
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("InfoService", "Start InfoService");
                        InfoParser parseTask = new InfoParser(getApplicationContext());
                        parseTask.execute(url);
                    }
                });


            }
        }, 0, 1000*60*10); //10分ごとに休講情報確認

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d("service", "onDestroy");
        super.onDestroy();
        // Looper終了
        // timer cancel
        if( timer != null ){
            timer.cancel();
            timer = null;
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        // エラーになるので、とりあえず入れてありますが使いません
        return null;
    }
}
