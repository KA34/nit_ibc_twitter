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


//アプリケーションが落ちてもデータをshared-preferencesに維持する
public class AccountData {
    public List<Map<String,String>> AccountData; //登録されたアカウントデータの保存場所

    // Preferenceのkeyは１つだけなので混乱ない　
    private static final String USER_SETTING_PREF_KEY="ACCOUNT_DATA";

    // 保存情報取得メソッド
    public static AccountData getInstance(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        String PreferencesString = prefs.getString(USER_SETTING_PREF_KEY, "");

        AccountData instance;
        // 保存したオブジェクトを取得
        if( !TextUtils.isEmpty(PreferencesString)) {
            instance = gson.fromJson(PreferencesString, AccountData.class);
        }else {
            // 何も保存されてない 初期時点 この時はデフォルト値を入れて上げる
            instance = getDefaultInstance();
        }
        return instance;
    }

    // デフォルト値の入ったオブジェクトを返す
    public static AccountData getDefaultInstance(){
        AccountData instance = new AccountData();
        instance.AccountData = new ArrayList<>();
        Map<String,String> tmpMap = new HashMap<>();
        tmpMap.put("id", "2391415987");
        tmpMap.put("clas", "4D");
        instance.AccountData.add(tmpMap);
        return instance;
    }

    // 状態保存メソッド
    public void savaInstance(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        // 現在のインスタンスの状態を保存
        prefs.edit().putString(USER_SETTING_PREF_KEY, gson.toJson(this)).apply();
    }

    //id捜索メソッド 戻り値:index(無:-1)
    public int Searchid(long id){
        Map<String,String> tmpMap;
        for (int i = 0; i < AccountData.size(); i++) {
            tmpMap = AccountData.get(i);
            if (Long.parseLong(tmpMap.get("id")) == id){
                return i;
            }
        }
        return -1;
    }
}
