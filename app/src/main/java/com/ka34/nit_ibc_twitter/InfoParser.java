package com.ka34.nit_ibc_twitter;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import java.text.Normalizer;
import java.util.Calendar;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.TwitterException;
import twitter4j.User;

public class InfoParser extends AsyncTask<String, Void, Integer>  {

    Context mContext;
    Preferences pref;
    AccountData account;

    Boolean save;

    // インスタンス生成
    public InfoParser(Context context) {
        mContext = context;
    }
    ListData data = new ListData();


    private String getToday(){
        Calendar cal = Calendar.getInstance();
        String year = String.valueOf(cal.get(Calendar.YEAR));
        String month, day;
        if (cal.get(Calendar.MONTH)+1 < 10) {
            month = "0" + String.valueOf(cal.get(Calendar.MONTH)+1);// 0 - 11
        } else {
            month = String.valueOf(cal.get(Calendar.MONTH)+1);// 0 - 11
        }
        if (cal.get(Calendar.DAY_OF_MONTH) < 10){
            day = "0" + String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        } else {
            day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        }

        return year + month + day;
    }
    @Override
    protected Integer doInBackground(String... url) {
        try {
            Document document = Jsoup.connect(url[0]).get();
            Elements body = document.getElementsByClass("oshirase");
            ListData.infoData = body.toString();

            ParseHTML();
            AllocateList(); //parseList:新規 matchList:維持 prefData:削除
            MakeTweetData();
            MakeTweet();

        } catch (Exception e) {
            e.printStackTrace();
        }


        return 1;
    }

    @Override
    protected void onPostExecute(Integer result) {
        Log.d("a", "ParseComplete");
    }

    private void ParseHTML(){
        boolean todayFlag = false;
        boolean breakFlag = false;
        String str = "";
        Document doc = Jsoup.parse(ListData.infoData);
        Elements ele = doc.getElementsByTag("tr");
        ListData.infoData = ele.toString();
        String[] trList = ListData.infoData.split("</tr>", 0);
        for (int i=0; i<trList.length; i++) {
            if (todayFlag) { breakFlag = true; }
            String[] tdList = trList[i].split("</td>",0);
            String regex = "(\\d{4}年\\d+月\\d+日\\（\\p{InCJKUnifiedIdeographs}\\）)";
            tdList[0] = extractMatchString(regex, tdList[0]);
            if(Integer.valueOf(getDate()) > Integer.valueOf(getCompare(tdList[0],tdList[0]))){ break; }
            String[] pList = tdList[2].split("</p>", 0);
//            Log.d(tdList[0], String.valueOf(getNumber(tdList[2])));
//                Log.d("td "+String.valueOf(i),tdList[0]);//更新日
            for (int j=1; j<pList.length-1; j++) {      //length-1
                pList[j] = pList[j].substring(4);
                String[] spList = pList[j].split("<span style=\"text-decoration: underline;\">");
                ArrayList<String> spArrayList = new ArrayList<>();
                Pattern p = Pattern.compile("^<br>");
                for (int k = 1; k <spList.length ; k++) {
                    Matcher m = p.matcher(spList[k]);
                    if (m.find()) {spList[k] = spList[k].substring(4);}
                    Pattern ps = Pattern.compile("(^</span>$)|(^</span><br>$)");
                    Matcher ms = ps.matcher(spList[k]);
                    Pattern ps2 = Pattern.compile("^</span>[●◎☆]");
                    Matcher ms2 = ps2.matcher(spList[k]);
                    if (!ms.find()) {
                        if(ms2.find()) {
                            spList[k] = spList[k].substring(7);
                            spArrayList.set(spArrayList.size()-1,spList[k-1] + spList[k]);
                        } else {
                            spArrayList.add(spList[k]);
                        }
                    }
                }
                String[] temp = spArrayList.toArray(new String[spArrayList.size()]);
                spList = temp;

                for (int k = 0; k <spList.length ; k++) {
                    ArrayList<String> tmpList = new ArrayList<>();
//                        Log.d("a", spList[k]);
                    String[] brList = spList[k].split("<br>");
                    Pattern pbr = Pattern.compile("(^</span>)|(</span>$)");
                    Pattern pbr2 = Pattern.compile("</span>[●◎☆]");
                    Pattern blankbr = Pattern.compile("^　+$");
                    for (int l = 0; l <brList.length ; l++) {
                        Matcher mbr = pbr.matcher(brList[l]);
                        brList[l] = mbr.replaceFirst("");
                        Matcher mbr2 = pbr2.matcher(brList[l]);
                        if(mbr2.find()) {
                            String[] tmp = brList[l].split("</span>");
                            spArrayList = new ArrayList<>();
                            for (int m = 0; m <brList.length ; m++) {
                                if(m==l){
                                    spArrayList.add(tmp[0]);
                                    spArrayList.add(tmp[1]);
                                } else {
                                    spArrayList.add(brList[m]);
                                }
                            }
                            temp = spArrayList.toArray(new String[spArrayList.size()]);
                            brList = temp;
                        }
                        str = str+"\n"+brList[l];

                        Matcher mblankbr = blankbr.matcher(brList[l]);
                        if (!mblankbr.find()){
                            tmpList.add(brList[l]);
//                                Log.d("brList" + String.valueOf(l) + " ", brList[l]);
                        }
                    }
                    temp = tmpList.toArray(new String[tmpList.size()]);
                    brList = temp;

                    String other = null;

                    for (int n = 1; n<brList.length; n++) {

                        if(extractMatchString("^[●◎☆]\\S+\\s+(\\d|\\d([・，,－]\\d)*)限\\s",brList[n])!=null) {
                            String type;
                            if (brList[n].indexOf("☆")==0) {
                                type = "変更";
                            }else if(brList[n].indexOf("●")==0) {
                                type = "休講";
                            }else if(brList[n].indexOf("◎")==0){
                                type = "補講";
                            } else {
                                type = "?";
                            }
                            String compare = getCompare(brList[0],tdList[0]);
                            if (Integer.valueOf(compare) < Integer.valueOf(getToday())){todayFlag=true;}
                            String clas,term,cont;
                            clas = extractMatchString("^[●◎☆](\\S+)",brList[n]);
                            term = extractMatchString("\\s(\\d|\\d([・，,]\\d)*)限\\s",brList[n]);
                            cont = extractMatchString("限\\s+(.+)$",brList[n]);
                            Pattern pnbsp = Pattern.compile("&nbsp;");
                            Matcher mnbsp;
                            if (cont != null) {
                                mnbsp = pnbsp.matcher(cont);
                                cont = mnbsp.replaceAll("");
                            }
                            if (clas != null) {

                                if(clas.matches("^[１２]－[１２３４５]")) {
                                    Pattern pbar = Pattern.compile("－");
                                    Matcher mbar = pbar.matcher(clas);
                                    clas = mbar.replaceAll("の");
                                }
                                if(clas.matches("^[１２３４５][ＭＳＥＤＣ]")||clas.matches("^[１２３４５]年$")||clas.matches("^[１２]の[１２３４５]")||clas.matches("^[１２３４５]年留学生")) {
                                    AddparseList(brList[0], type, clas, term, cont, compare);

//                                Log.d(brList[0], clas); 所属例外
                                }
                            }

                        }else{
                            Pattern pp = Pattern.compile("[●◎☆]");
                            Matcher mp = pp.matcher(brList[n-1]);
                            if (mp.find()){
                                Map<String,String> tmpMap = data.parseList.get(data.parseList.size() - 1);
                                String date = tmpMap.get("date");
                                String type = tmpMap.get("type");
                                String clas = tmpMap.get("clas");
                                String term = tmpMap.get("term");
                                String cont = tmpMap.get("cont") + brList[n].trim();
                                String compare = tmpMap.get("compare");
                                data.parseList.remove(data.parseList.size() - 1);
                                AddparseList(date,type,clas,term,cont,compare);
                            }
                        }
                    }
/*                    if (other!=null){
                        other = other.substring(5);
                        tmpMap.put("date", brList[0]);
                        tmpMap.put("update", tdList[0]);
                        tmpMap.put("type", "other");
                        tmpMap.put("cont",other);
                        data.parseList.add(tmpMap);
                    }
*/                }
            }
            if (breakFlag) { break; }
        }
        //昨日以前を削除
        for (int i = 0; i < data.parseList.size(); i++) {
            Map<String,String> tmpMap = data.parseList.get(i);
            if (Integer.valueOf(tmpMap.get("compare")) < Integer.valueOf(getToday())) {
                data.parseList.remove(i);
                i--;
            }
        }
    }

    private void AllocateList(){
        pref = Preferences.getInstance(ApplicationController.getInstance().getApplicationContext());
        save = false;
        Log.v("size", String.valueOf(data.parseList.size()));
        for (int i = 0; i < data.parseList.size(); i++) {
//            Log.d("a", String.valueOf(data.parseList.get(i)));
            Map<String, String> parseMap = data.parseList.get(i);
            if(pref.PrefData != null) {
                for (int j = 0; j < pref.PrefData.size(); j++) {
                    Map<String, String> prefMap = pref.PrefData.get(j);
//                    Log.d("compare", prefMap.get("compare") + "  " + getToday());
                    if (Integer.valueOf(prefMap.get("compare")) < Integer.valueOf(getToday())) {
                        pref.PrefData.remove(j);
                        j--;
                        save = true;
                    } else {
                        if (parseMap.get("date").equals(prefMap.get("date"))
                                && parseMap.get("clas").equals(prefMap.get("clas"))
                                && parseMap.get("term").equals(prefMap.get("term"))
                                && parseMap.get("type").equals(prefMap.get("type"))
                                && parseMap.get("cont").equals(prefMap.get("cont")))
                        {
//                            Log.d(String.valueOf(i), String.valueOf(parseMap));
                            // 完全一致
                            data.matchList.add(parseMap);
                            data.parseList.remove(i);
                            pref.PrefData.remove(j);
                            i--;
                            break;
                        }
                    }
                }
            }
        }

        data.deleteList = new ArrayList<>();
        data.deleteList = pref.PrefData;
        pref.PrefData = data.matchList;

        if (data.parseList.size() != 0 || data.deleteList.size() != 0){
            Calendar cal = Calendar.getInstance();
            String month, day, hour, min, sec;

            month = String.valueOf(cal.get(Calendar.MONTH)+1);// 0 - 11
            day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));

            if(cal.get(Calendar.HOUR_OF_DAY) < 10){
                hour = "0" + String.valueOf(cal.get(Calendar.HOUR_OF_DAY));
            } else {
                hour = String.valueOf(cal.get(Calendar.HOUR_OF_DAY));
            }
            if (cal.get(Calendar.MINUTE) < 10){
                min = "0" + String.valueOf(cal.get(Calendar.MINUTE));
            } else {
                min = String.valueOf(cal.get(Calendar.MINUTE));
            }
            if (cal.get(Calendar.SECOND) < 10){
                sec = "0" + String.valueOf(cal.get(Calendar.SECOND));
            } else {
                sec = String.valueOf(cal.get(Calendar.SECOND));
            }

            if (data.parseList.size() != 0) {
                String message = month + "/" + day + " " + hour + ":" + min + ":" + sec + "\n" + "休講情報の追加を検知しました";
                int count = message.length();
                int limit = 140;

                for (int i = 0; i < data.parseList.size(); i++) {
                    Log.d("Allocate", String.valueOf(data.parseList.get(i)));
                    Map<String, String> tmpMap = data.parseList.get(i);
                    String date = tmpMap.get("date");
                    String type = tmpMap.get("type");
                    String term = tmpMap.get("term");
                    String cont = tmpMap.get("cont");
                    String cla = tmpMap.get("clas");
                    String line =  "\n【" + type + "】" + date + " " + term + "限 " + cla + " ←New!\n" + cont;
                    if (count + line.length() <= limit) {
                        message += line;
                        count += line.length();
                    } else {
                        Log.d("add", String.valueOf(count));
                        pref.TweetQueue.add(message);
                        if (i < data.parseList.size()) {
                            message = month + "/" + day + " " + hour + ":" + min + ":" + sec + " 追加続き";
                            count = message.length();
                            message += line;
                            count += line.length();
                        } else {
                            message = "";
                        }
                    }
                }
                if (!message.isEmpty()) {
                    pref.TweetQueue.add(message);
                }
            }
            if (data.deleteList.size() != 0) {
                String message = month + "/" + day + " " + hour + ":" + min + ":" + sec + "\n" + "休講情報の削除を検知しました";
                int count = message.length();
                int limit = 140;

                for (int i = 0; i < data.deleteList.size(); i++) {
                    Log.d("Allocate", String.valueOf(data.deleteList.get(i)));
                    Map<String, String> tmpMap = data.deleteList.get(i);
                    String date = tmpMap.get("date");
                    String type = tmpMap.get("type");
                    String term = tmpMap.get("term");
                    String cont = tmpMap.get("cont");
                    String cla = tmpMap.get("clas");
                    String line =  "\n【" + type + "】" + date + " " + term + "限 " + cla + " ←Deleted!\n" + cont;
                    if (count + line.length() <= limit) {
                        message += line;
                        count += line.length();
                    } else {
                        Log.d("add", String.valueOf(count));
                        pref.TweetQueue.add(message);
                        if (i < data.deleteList.size()) {
                            message = month + "/" + day + " " + hour + ":" + min + ":" + sec + " 削除続き";
                            count = message.length();
                            message += line;
                            count += line.length();
                        } else {
                            message = "";
                        }
                    }
                }
                if (!message.isEmpty()) {
                    pref.TweetQueue.add(message);
                }
            }
            save = true;
        }

    } //parseList:新規 deleteList:消えた prefData:完成

    public void MakeTweetData(){
        String[] dep = new String[]{"M","S","E","D","C"};
        if (data.parseList.size() != 0){
            for (int i = 1; i <= 5; i++) {
                for (int j = 0; j < 5; j++) {
                    for (int k = 1; k <= 5; k++) {
                        List<Map<String,String>> newfilterList = data.GetfilterList(String.valueOf(i), dep[j], String.valueOf(k));
                        List<Map<String,String>> newdeletefilterList = data.GetdeletefilterList(String.valueOf(i), dep[j], String.valueOf(k));
                        if (newfilterList.size() != 0 || newdeletefilterList.size() != 0){
                            List<Map<String,String>> filterList = pref.GetfilterList(String.valueOf(i), dep[j], String.valueOf(k));
                            List<String> tmpList = new ArrayList<>();
//                        Log.d("a","pref");
                            if (filterList.size() != 0){
                                for (int l = 0; l < filterList.size(); l++) {
                                    Map<String, String> tmpMap = filterList.get(l);
                                    String date = tmpMap.get("date");
                                    String type = tmpMap.get("type");
                                    String term = tmpMap.get("term");
                                    String cont = tmpMap.get("cont");
                                    String cla = tmpMap.get("clas");
                                    String line = "\n【" + type + "】" + date + " " + term + "限 " + cla + " \n" + cont;
                                    tmpList.add(line);
                                }
                                tmpList.add("\n");
                            }if (newfilterList.size() != 0) {
                                for (int l = 0; l < newfilterList.size(); l++) {
                                    Log.d("MakeTweetData", String.valueOf(i) + String.valueOf(j) + String.valueOf(k) + String.valueOf(newfilterList.get(l)));
                                    Map<String, String> tmpMap = newfilterList.get(l);
                                    String date = tmpMap.get("date");
                                    String type = tmpMap.get("type");
                                    String term = tmpMap.get("term");
                                    String cont = tmpMap.get("cont");
                                    String cla = tmpMap.get("clas");
                                    String line = "\n【" + type + "】" + date + " " + term + "限 " + cla + " ←New!\n" + cont;
                                    tmpList.add(line);
                                }
                                if (newdeletefilterList.size() != 0){
                                    tmpList.add("\n");
                                }
                            }
                            for (int l = 0; l < newdeletefilterList.size(); l++) {
                                Log.d("MakeTweetData", String.valueOf(i) + String.valueOf(j) + String.valueOf(k) + String.valueOf(newdeletefilterList.get(l)));
                                Map<String, String> tmpMap = newdeletefilterList.get(l);
                                String date = tmpMap.get("date");
                                String type = tmpMap.get("type");
                                String term = tmpMap.get("term");
                                String cont = tmpMap.get("cont");
                                String cla = tmpMap.get("clas");
                                String line = "\n【" + type + "】" + date + " " + term + "限 " + cla + " ←削除されました\n" + cont;
                                tmpList.add(line);
                            }
                            if (i < 3){
                                data.TweetData.put(String.valueOf(i) + dep[j] + String.valueOf(k), tmpList);
                                Log.d("MakeTweetDataResult", String.valueOf(i) + dep[j] + String.valueOf(k) + tmpList);
                            } else {
                                data.TweetData.put(String.valueOf(i) + dep[j], tmpList);
                                Log.d("MakeTweetDataResult", String.valueOf(i) + dep[j] + tmpList);
                            }
                        }
                        if (i >= 3){
                            break;
                        }
                    }
                }
            }
            pref.PrefData.addAll(data.parseList);
        }
    }
    public void MakeTweet() {
        account = AccountData.getInstance(ApplicationController.getInstance().getApplicationContext());
        for (int i = 0; i < account.AccountData.size(); i++) {
            Map<String,String> tmpMap = account.AccountData.get(i);
            if (data.TweetData.get(tmpMap.get("clas")) != null){
                List<String> tmpList = data.TweetData.get(tmpMap.get("clas"));
                User user = null;
                try {
                    user = Tweet.twitter.showUser(Long.valueOf(tmpMap.get("id")));
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                assert user != null;
                String username = user.getScreenName();
                String message = "休講・授業変更情報が更新されました.";
                int count = username.length() + message.length() + 2;

                Boolean dm = tmpMap.containsKey("dm");
                int limit;
                if (dm){
                    limit = 10000;
                } else {
                    limit = 140;
                }

                for (int j = 0; j < tmpList.size(); j++) {
                    String line = tmpList.get(j);
                    if (count + line.length() <= limit) {
                        message += line;
                        count += line.length();
                    } else {
                        Log.d("add", String.valueOf(count));
                        pref.TweetQueue.add("@" + username + " " + message);
                        if (j < tmpList.size()){
                            message = "続き";
                            count = username.length() + message.length() + 2;
                            message += line;
                            count += line.length();
                        } else {
                            message = "";
                        }
                    }
                }
                if (!message.isEmpty()) {
                    if (dm){
                        pref.DMQueue.add(new String[]{username, message});
                    } else {
                        pref.TweetQueue.add("@" + username + " " + message);
                    }
                }
                save = true;
            }
        }
        if (save) pref.savaInstance(ApplicationController.getInstance().getApplicationContext());
    }
    public static String extractMatchString(String regex, String target) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(target);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }
    private void AddparseList(String date, String type, String clas, String term, String cont, String compare){
        Map<String,String> tmpMap = new HashMap<>();
        tmpMap.put("date", Normalizer.normalize(date, Normalizer.Form.NFKC));
//            tmpMap.put("update", update);
        tmpMap.put("type", Normalizer.normalize(type, Normalizer.Form.NFKC));
        tmpMap.put("clas", Normalizer.normalize(clas, Normalizer.Form.NFKC));
        tmpMap.put("term", Normalizer.normalize(term, Normalizer.Form.NFKC));
        tmpMap.put("cont", Normalizer.normalize(cont, Normalizer.Form.NFKC));
        tmpMap.put("compare", compare);
//        Log.d("a", String.valueOf(tmpMap));
        data.parseList.add(tmpMap);
    }
    private String fullWidthNumberToHalfWidthNumber(String str) {
        if (str == null) {
            throw new IllegalArgumentException();
        }
        StringBuilder sb = new StringBuilder(str);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if ('０' <= c
                    && c <= '９') {
                sb.setCharAt(i, (char) (c - '０' + '0'));
            }
        }
        return sb.toString();
    }
    private String getCompare(String brlist, String tdlist){
        Pattern pmonth = Pattern.compile("\\d\\d月");
        Matcher mmonth = pmonth.matcher(brlist);
        String month;
        if (mmonth.find()){
            month = extractMatchString("(\\d\\d)月", brlist);
        } else {
            month = "0"+ extractMatchString("(\\d)月", brlist);
        }
        Pattern pd = Pattern.compile("\\d\\d日");
        Matcher md = pd.matcher(brlist);
        String date;
        if (md.find()){
            date = extractMatchString("(\\d\\d)日",brlist);
        } else {
            date = "0"+extractMatchString("(\\d)日",brlist);
        }
        String year = extractMatchString("(\\d\\d\\d\\d)年",tdlist);
        return fullWidthNumberToHalfWidthNumber(year+month+date);
    }
    private String getDate(){
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = 1 + cal.get(Calendar.MONTH);// 0 - 11
        if (month < 4){
            return String.valueOf(year-1) + "0331";
        } else {
            return String.valueOf(year) + "0331";
        }
    }
}
