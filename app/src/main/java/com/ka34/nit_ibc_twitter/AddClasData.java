package com.ka34.nit_ibc_twitter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddClasData {
    private static final String TAG = AddClasData.class.getSimpleName();
    private final AddClasData self = this;

    public Map<String,List<Map<String,String>>> AddClasData; //登録されたアカウントデータの保存場所

    // Preferenceのkeyは１つだけなので混乱ない　
    private static final String USER_SETTING_PREF_KEY="ADDCLAS_DATA";

    // 保存情報取得メソッド
    public static AddClasData getInstance(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        String PreferencesString = prefs.getString(USER_SETTING_PREF_KEY, "");

        AddClasData instance;
        // 保存したオブジェクトを取得
        if( !TextUtils.isEmpty(PreferencesString)) {
            instance = gson.fromJson(PreferencesString, AddClasData.class);
        }else {
            // 何も保存されてない 初期時点 この時はデフォルト値を入れて上げる
            instance = getDefaultInstance();
        }
        return instance;
    }

    // デフォルト値の入ったオブジェクトを返す
    public static AddClasData getDefaultInstance(){
        AddClasData instance = new AddClasData();
        instance.AddClasData = new HashMap<>();
        return instance;
    }

    // 状態保存メソッド
    public void savaInstance(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        // 現在のインスタンスの状態を保存
        prefs.edit().putString(USER_SETTING_PREF_KEY, gson.toJson(this)).apply();
    }
}
