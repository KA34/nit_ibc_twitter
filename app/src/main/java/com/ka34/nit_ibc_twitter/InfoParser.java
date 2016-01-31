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
            Document document2 = Jsoup.connect("https://f65fec82d3baf4bbc4d2bab12233737fffd2033c-www.googledrive.com/host/0BwTonu4uzP9sfnBGQVFzYWFwelp1aDFEbXE3cEhQTDY3YWRJM1E0NlFSQnNldTJBUE5fRkU/").get();
            Elements body2 = document2.getElementsByClass("information");
            ListData.makeData = body2.toString();
//                Log.d("html",document2.html());
//            ParseInfo();
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
//        Log.i("a", "ParseComplete");
    }

    private void ParseHTML(){
        boolean todayFlag = false;
        String str = "";
        Document doc = Jsoup.parse(ListData.infoData);
        Elements ele = doc.getElementsByTag("tr");
        ListData.infoData = ele.toString();
        String[] trList = ListData.infoData.split("</tr>", 0);
        for (int i=0; i<trList.length; i++) {
            if (todayFlag) { break; }
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
                        if(extractMatchString("^[●◎☆]\\S+\\s+(\\d|\\d([・，,－]\\d)*)限\\s",brList[n])!=null) {
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
                                    if (!DeleteSearch(brList[0],clas,term)) {
                                        AddparseList(brList[0], type, clas, term, cont, compare);
                                    }
/*                                } else {
                                    Log.d(brList[0], clas); 所属例外 */
                                }
                            }

                        }else{
                            other = other +"\n"+ brList[n];
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
    private void ParseInfo(){
        Map<String,String> tmpMap;
        data.parseList = new ArrayList<>();
        String[] brList = ListData.makeData.split("</p>", 0);
        for (int i = 1; i < brList.length-1; i++) {
            brList[i] = brList[i].substring(6);
            String[] dataList = brList[i].split(",", 0);
            if (Integer.valueOf(getDate()) < Integer.valueOf(dataList[1])) {
                tmpMap = new HashMap<>();
                switch (dataList[0]) {
                    case "delete":
                        tmpMap.put("date", dataList[2]);
                        tmpMap.put("clas", dataList[3]);
                        tmpMap.put("term", dataList[3]);
                        data.deleteList.add(tmpMap);
                        break;
                    case "remake":
                        tmpMap.put("date", dataList[2]);
                        tmpMap.put("clas", dataList[4]);
                        tmpMap.put("term", dataList[5]);
                        data.deleteList.add(tmpMap);
                        tmpMap = new HashMap<>();
                        tmpMap.put("compare", dataList[1]);
                        tmpMap.put("date", dataList[2]);
                        tmpMap.put("type", dataList[3]);
                        tmpMap.put("clas", dataList[4]);
                        tmpMap.put("term", dataList[5]);
                        tmpMap.put("cont", dataList[6]);
                        data.parseList.add(tmpMap);
                        break;
                    case "new":
                        if (dataList[3].equals("other")) {
                            tmpMap.put("compare", dataList[1]);
                            tmpMap.put("date", dataList[2]);
                            tmpMap.put("type", dataList[3]);
                            tmpMap.put("cont", dataList[4]);
                            data.parseList.add(tmpMap);
                            break;
                        } else {
                            tmpMap.put("compare", dataList[1]);
                            tmpMap.put("date", dataList[2]);
                            tmpMap.put("type", dataList[3]);
                            tmpMap.put("clas", dataList[4]);
                            tmpMap.put("term", dataList[5]);
                            tmpMap.put("cont", dataList[6]);
                            data.parseList.add(tmpMap);
                            break;
                        }
                }
            }
        }
    }
    private void AllocateList(){
        pref = Preferences.getInstance(mContext);
        for (int i = 0; i < data.parseList.size(); i++) {
            Map<String, String> parseMap = data.parseList.get(i);
            if(pref.PrefData != null) {
                for (int j = 0; j < pref.PrefData.size(); j++) {
                    Map<String, String> prefMap = pref.PrefData.get(j);
                    if (Integer.valueOf(prefMap.get("compare")) < Integer.valueOf(getToday())) {
                        pref.PrefData.remove(j);
                        j--;
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
        pref.PrefData = data.matchList;
        for (int i = 0; i < data.parseList.size(); i++) {
            Log.d("Allocate", String.valueOf(data.parseList.get(i)));
        }
    } //parseList:新規 matchList:維持 prefData:完成

    public void MakeTweetData(){
        String[] dep = new String[]{"M","S","E","D","C"};
        if (data.parseList.size() != 0){
            for (int i = 1; i <= 5; i++) {
                for (int j = 0; j < 5; j++) {
                    for (int k = 1; k <= 5; k++) {
                        List<Map<String,String>> newfilterList = data.GetfilterList(String.valueOf(i), dep[j], String.valueOf(k));
                        if (newfilterList.size() != 0){
                            pref = Preferences.getInstance(ApplicationController.getInstance().getApplicationContext());
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
                            }
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
            pref.savaInstance(mContext);
        }
    }
    public void MakeTweet() {
        account = AccountData.getInstance(ApplicationController.getInstance().getApplicationContext());
        pref = Preferences.getInstance(ApplicationController.getInstance().getApplicationContext());
        Boolean save = false;
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
                String message = "休講・授業変更情報が追加されました.";
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
    private boolean DeleteSearch(String date, String clas, String term){
        for (int q = 0; q < data.deleteList.size(); q++) {
            Map<String, String> tmpdelMap = data.deleteList.get(q);
            Log.d("", tmpdelMap.get("date") + Normalizer.normalize(date, Normalizer.Form.NFKC) + tmpdelMap.get("clas") + Normalizer.normalize(clas, Normalizer.Form.NFKC) + tmpdelMap.get("term") + Normalizer.normalize(term, Normalizer.Form.NFKC));
                if (tmpdelMap.get("date").equals(date) && tmpdelMap.get("clas").equals(clas) && tmpdelMap.get("term").equals(term)) {
                data.deleteList.remove(q);
                return true;
            }
        }
        return false;
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
