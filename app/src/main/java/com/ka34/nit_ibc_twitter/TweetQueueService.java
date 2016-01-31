package com.ka34.nit_ibc_twitter;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class TweetQueueService extends Service {

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
                        Preferences pref = Preferences.getInstance(ApplicationController.getInstance().getApplicationContext());
                        if (pref.TweetQueue.peek() != null) {
                            Tweet.exetweet(pref.TweetQueue.poll());
                            pref.savaInstance(ApplicationController.getInstance().getApplicationContext());
                        }
                        if (pref.DMQueue.peek() != null){
                            Tweet.SendDirectMessage(pref.DMQueue.peek()[0],pref.DMQueue.poll()[1]);
                            pref.savaInstance(ApplicationController.getInstance().getApplicationContext());
                        }
                    }
                });
            }
        }, 0, 1000*3); //3秒ごとにツイート

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
