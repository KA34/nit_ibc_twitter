package com.ka34.nit_ibc_twitter;

import android.app.Application;

public class ApplicationController extends Application {
    private static ApplicationController sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    public static synchronized ApplicationController getInstance() {
        return sInstance;
    }
}