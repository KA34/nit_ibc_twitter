package com.ka34.nit_ibc_twitter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    AccountData account;
    Preferences pref;
    TweetQueueService queue;
    ListData data;
    ArrayAdapter<String> adapter;

    ListView lv;

    //Service立ち上げにしか使わない
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = Preferences.getInstance(ApplicationController.getInstance().getApplicationContext());
        account = AccountData.getInstance(ApplicationController.getInstance().getApplicationContext());


/*        String[] lines = new String[account.AccountData.size()];
        for (int i = 0; i < account.AccountData.size(); i++) {
            Map<String,String> tmpMap = account.AccountData.get(i);
            lines[i] = tmpMap.get("id") + " " + tmpMap.get("clas");
        }
*/

        String[] lines = new String[pref.PrefData.size()];
        for (int i = 0; i < pref.PrefData.size(); i++) {
            Map<String,String> tmpMap = pref.PrefData.get(i);
            String date = tmpMap.get("date");
            String type = tmpMap.get("type");
            String term = tmpMap.get("term");
            String cont = tmpMap.get("cont");
            String cla = tmpMap.get("clas");
            lines[i] = date + " " + term + "限 " + cla + " \n" + cont;
        }
        lv = (ListView) findViewById(R.id.listView);

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_expandable_list_item_1, lines);

        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;
                Map<String,String> tmpMap = pref.PrefData.get(position);
                String date = tmpMap.get("date");
                String type = tmpMap.get("type");
                String term = tmpMap.get("term");
                String cont = tmpMap.get("cont");
                String cla = tmpMap.get("clas");
                String item = date + " " + term + "限 " + cla + " \n" + cont;
                Toast.makeText(getApplicationContext(), item + " clicked",
                        Toast.LENGTH_LONG).show();
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;
                String item = (String) listView.getItemAtPosition(position);
                pref.PrefData.remove(position);
                pref.savaInstance(ApplicationController.getInstance().getApplicationContext());
                String[] lines = new String[pref.PrefData.size()];
                for (int i = 0; i < pref.PrefData.size(); i++) {
                    Map<String,String> tmpMap = pref.PrefData.get(i);
                    String date = tmpMap.get("date");
                    String type = tmpMap.get("type");
                    String term = tmpMap.get("term");
                    String cont = tmpMap.get("cont");
                    String cla = tmpMap.get("clas");
                    lines[i] = date + " " + term + "限 " + cla + " \n" + cont;
                }
                adapter.notifyDataSetChanged();
                lv.setAdapter(adapter);
                return false;
            }
        });

        StreamingService st = new StreamingService();
        startService(new Intent(MainActivity.this, StreamingService.class));
//        InfoParser parseTask = new InfoParser(getApplicationContext());
//        parseTask.execute(url);
        Intent intent = new Intent(getApplication(), InfoService.class);
        startService(intent);

        TweetQueueService queue = new TweetQueueService();
        startService(new Intent(MainActivity.this, TweetQueueService.class));

        Tweet tweet = new Tweet(getApplicationContext());
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // メニューバーからのSettingアラートを構築、表示、打ち込まれた文字列をファイルに保存
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.settings,(ViewGroup)findViewById(R.id.layout_root));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");
        builder.setView(layout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                EditText idtext
                        = (EditText)layout.findViewById(R.id.id);

                String id   = idtext.getText().toString();

                EditText classtext
                        = (EditText)layout.findViewById(R.id.clas);

                String clas   = classtext.getText().toString();

                Map<String,String> tmpMap = new HashMap<>();
                tmpMap.put("id", id);
                tmpMap.put("clas", clas);
                account.AccountData.add(tmpMap);
                account.savaInstance(getApplicationContext());
            }

        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Cancel ボタンクリック処理
            }
        });

        // 表示
        builder.create().show();
        return super.onOptionsItemSelected(item);
    }
}

