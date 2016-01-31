package com.ka34.nit_ibc_twitter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import com.google.gson.Gson;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;

//アプリケーションが落ちてもデータをshared-preferencesに維持する
public class Preferences {

    public List<Map<String,String>> PrefData; //取得した休講情報の格納場所
    public Deque<String> TweetQueue; //送信TweetのQueue保存場所
    public Deque<String[]> DMQueue; //送信DMのQueue保存場所

    // Preferenceのkeyは１つだけなので混乱ない
    private static final String USER_SETTING_PREF_KEY="USER_SETTING";

    // 保存情報取得メソッド
    public static Preferences getInstance(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        String PreferencesString = prefs.getString(USER_SETTING_PREF_KEY, "");

        Preferences instance;
        // 保存したオブジェクトを取得
        if( !TextUtils.isEmpty(PreferencesString)) {
            instance = gson.fromJson(PreferencesString, Preferences.class);
        }else {
            // 何も保存されてない 初期時点 この時はデフォルト値を入れて上げる
            instance = getDefaultInstance();
        }
        return instance;
    }

    // デフォルト値の入ったオブジェクトを返す
    public static Preferences getDefaultInstance(){
        Preferences instance = new Preferences();
        instance.PrefData = new ArrayList<>();
        instance.TweetQueue = new ArrayDeque<>();
        instance.DMQueue = new ArrayDeque<>();
        return instance;
    }

    // 状態保存メソッド
    public void savaInstance(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        // 現在のインスタンスの状態を保存
        prefs.edit().putString(USER_SETTING_PREF_KEY, gson.toJson(this)).apply();
    }

    //指定された所属の情報をPrefDataから捜索してArrayListで返す
    public List<Map<String,String>> GetfilterList(String grade, String dep, String clas){
        List<Map<String,String>> filterList;
//        Log.d("Preferences", "new");
        filterList = new ArrayList<>();
        List<String> filter = new ArrayList<>();
        filter.add(grade+dep);
        filter.add(grade+"年");
        filter.add("全学年");
        if(clas!=null){
            filter.add(grade+"の"+clas);
        }
//        Log.d("Preferences", "number");
//        Log.d("Preferences", String.valueOf(PrefData.size()));
        for (int i = 0; i < PrefData.size(); i++) {
            Map<String, String> tmpMap;
//            Log.d("Preferences", String.valueOf(PrefData.get(i)));
            tmpMap = PrefData.get(i);
            if (!tmpMap.get("type").equals("other")) {
//                Log.d(String.valueOf(i), String.valueOf(PrefData.get(i)));
                String tmpclas = tmpMap.get("clas");
                for (int j = 0; j < filter.size(); j++) {
                    if (tmpclas.equals(filter.get(j))) {
                        filterList.add(tmpMap);
                    }
                }
            }
        }

        //日付順にソート
        Collections.sort(filterList, new Comparator<Map<String, String>>() {
            public int compare(Map<String, String> map1, Map<String, String> map2) {

                String S1 = map1.get("compare");
                String S2 = map2.get("compare");

                return S1.compareTo(S2);
            }
        });
        return filterList;
    }
}
