package com.ka34.nit_ibc_twitter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.DirectMessage;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.UserStreamListener;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

//StreamingAPIで監視
public class StreamingService extends Service {

    String myid = "@nitibc_info";
    @Override
    public void onCreate() {
        // 認証キーを設定
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(Tweet.oAuthConsumerKey);
        builder.setOAuthConsumerSecret(Tweet.oAuthConsumerSecret);
        builder.setOAuthAccessToken(Tweet.oAuthAccessToken);
        builder.setOAuthAccessTokenSecret(Tweet.oAuthAccessTokenSecret);


        // Configurationを作る
        Configuration conf = builder.build();

        // TwitterStreamのインスタンス作成
        TwitterStream twitterStream = new TwitterStreamFactory(conf).getInstance();

        // Listenerを登録
        twitterStream.addListener(new Listener());

        // 実行
        twitterStream.user();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** TweetのListener */
    class Listener implements UserStreamListener {

        //TLに変化があった時に呼び出される
        public void onStatus(Status status) {
/*            Double lat = null;
            Double lng = null;
            String[] urls = null;
            String[] medias = null;

            //. 位置情報が含まれていれば取得する
            GeoLocation location = status.getGeoLocation();
            if( location != null ){
                double dlat = location.getLatitude();
                double dlng = location.getLongitude();
                lat = dlat;
                lng = dlng;
            }
*/
            //. ツイート本文にリンクURLが含まれていれば取り出す
/*            URLEntity[] uentitys = status.getURLEntities();
            if( uentitys != null && uentitys.length > 0 ){
                List list = new ArrayList();
                for( int i = 0; i < uentitys.length; i ++ ){
                    URLEntity uentity = uentitys[i];
                    String expandedURL = uentity.getExpandedURL();
                    list.add( expandedURL );
                }
                urls = ( String[] )list.toArray( new String[0] );
            }

            //. ツイート本文に画像／動画URLが含まれていれば取り出す
            MediaEntity[] mentitys = status.getMediaEntities();
            if( mentitys != null && mentitys.length > 0 ){
                List list = new ArrayList();
                for( int i = 0; i < mentitys.length; i ++ ){
                    MediaEntity mentity = mentitys[i];
                    String expandedURL = mentity.getExpandedURL();
                    list.add( expandedURL );
                }
                medias = ( String[] )list.toArray( new String[0] );
            }
*/
            long id = status.getId(); //. ツイートID
            String text = status.getText(); //. ツイート本文
            long userid = status.getUser().getId(); //. ユーザーID
            String username = status.getUser().getScreenName(); //. ユーザー表示名
            Date created = status.getCreatedAt(); //. ツイート日時

            Log.i("reply", "id = " + id + ", userid = " + userid + ", username = " + username + ", text = " + text + ", Date = " + created );

            //replyを受信した時
            if (userid != 4763208625.0 && !text.startsWith("RT") && text.contains(myid)){
                int index = text.indexOf(myid);
                String tweet = text.substring(0,index) + text.substring(index+myid.length());
                TweetContains(id, tweet, userid, username, false);
            }

            //取得漏れ時の対策手動RT
            if(userid == 4763208625.0 && text.startsWith("RT") && text.contains(myid)){
                int index = text.indexOf(myid);
                String tweet = text.substring(0,index) + text.substring(index+myid.length());
                username = text.substring(text.indexOf("@")+1, text.indexOf(":"));
                try {
                    User user = Tweet.twitter.showUser(username);
                    userid = user.getId();
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                TweetContains(id, tweet, userid, username, false);
            }

        }

        public void onDeletionNotice(StatusDeletionNotice sdn) {
            //System.out.println("onDeletionNotice.");
        }

        public void onTrackLimitationNotice(int i) {
            //System.out.println("onTrackLimitationNotice.(" + i + ")");
        }

        public void onScrubGeo(long lat, long lng) {
            //System.out.println("onScrubGeo.(" + lat + ", " + lng + ")");
        }

        public void onException(Exception excptn) {
            //System.out.println("onException.");
        }

        public void onStallWarning(StallWarning arg0) {
            // TODO Auto-generated method stub
        }

        //所属の形式を変更 (4D→4年D科 2S3→2年S科3組)
        private String MakeClass(String aff){
            String grade = aff.substring(0, 1);
            String dep = aff.substring(1, 2);
            if (aff.length() == 2){
                return grade + "年" + dep + "科";
            } else {
                String clas = aff.substring(2,3);
                return  grade + "年" + dep + "科" + clas + "組";
            }
        }

        //正規表現パターン中に()で囲われた部分を文字列から抽出する
        //regex 正規表現パターン
        //terget 抽出対象文字列
        private String extractMatchString(String regex, String target) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(target);
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                return null;
            }
        }

        //Tweet文字列から指定された命令を判別して実行する
        //id ツイートid
        //tweet ツイート本文
        //userid 対象のユーザーID(数字のみで表されるアカウントごとに割り振られるID, screennameでない)
        //username 対象のユーザーネーム (@hogehogeで表されるscreenname)
        //dm DirectMessageから呼び出されればtrue リプライから呼び出されればfalse
        private void TweetContains(long id, String tweet, long userid, String username, boolean dm){
            Preferences pref;
            AccountData account;
            AddClasData addclas;
            Map<String,String> tmpMap = new HashMap<>();

            if (tweet.contains("登録")){

                tweet = Normalizer.normalize(tweet, Normalizer.Form.NFKC);
                Pattern pattern = Pattern.compile("[12][MSEDC][12345]|[345][MSEDC]");
                Matcher matcher = pattern.matcher(tweet);
                if (matcher.find()) {
                    //登録成功
                    tweet =  extractMatchString("([12][MSEDC][12345]|[345][MSEDC])", tweet);
                    account = AccountData.getInstance(ApplicationController.getInstance().getApplicationContext());
                    int result = account.Searchid(userid);

                    if (result == -1){
                        //新規登録
                        tmpMap.put("id", String.valueOf(userid));
                        tmpMap.put("clas", tweet);
                        if (dm) tmpMap.put("dm","");
                        account.AccountData.add(tmpMap);
                        account.savaInstance(ApplicationController.getInstance().getApplicationContext());

                        //登録完了Tweet
                        if (dm){
                            Reply(username, MakeClass(tweet) + "で登録されました.所属変更は,「登録」と所属(例:4D,1S2)を含めて,\n情報請求は「情報」を含めて,\n登録解除は「解除」を含めてダイレクトメッセージを送信して下さい。", id, true);
                        } else {
                            Reply(username, MakeClass(tweet) + "で登録されました.所属変更は,「登録」と所属(例:4D,1S2)を含めて,\n情報請求は「情報」を含めて,\n登録解除は「解除」を含めてリプライして下さい。", id, false);
                        }
                        jouhou(username, id, dm, tweet);

                    } else {
                        account = AccountData.getInstance(ApplicationController.getInstance().getApplicationContext());
                        tmpMap = account.AccountData.get(result);
                        if (dm^tmpMap.containsKey("dm")){
                            if (dm){
                                String aff = tmpMap.get("clas");
                                tmpMap = new HashMap<>();
                                tmpMap.put("id", String.valueOf(userid));
                                tmpMap.put("clas", tweet);
                                tmpMap.put("dm", "");
                                account.AccountData.set(result, tmpMap);
                                account.savaInstance(ApplicationController.getInstance().getApplicationContext());

                                //変更完了dm送信
                                Reply(username, MakeClass(aff) + "(リプライ)から" + MakeClass(tweet) + "(ダイレクトメッセージ) に登録情報が更新されました.", id, true);

                            } else {
                                String aff = tmpMap.get("clas");
                                tmpMap = new HashMap<>();
                                tmpMap.put("id", String.valueOf(userid));
                                tmpMap.put("clas", tweet);
                                account.AccountData.set(result, tmpMap);
                                account.savaInstance(ApplicationController.getInstance().getApplicationContext());

                                //変更完了reply
                                Reply(username,MakeClass(aff) + "(ダイレクトメッセージ)から" + MakeClass(tweet)+"(リプライ)に登録情報が更新されました.", id, false);

                            }
                            jouhou(username, id, dm, tweet);
                        } else {
                            if (tmpMap.get("clas").equals(tweet)){
                                //登録済み
                                //既に登録されています。
                                String aff = tmpMap.get("clas");
                                Reply(username, "このアカウントは"+ MakeClass(aff) +"で既に登録されています.", id, dm);
                            } else {
                                //既存ユーザー
                                String aff = tmpMap.get("clas");
                                tmpMap = new HashMap<>();
                                tmpMap.put("id", String.valueOf(userid));
                                tmpMap.put("clas", tweet);
                                if (dm) tmpMap.put("dm","");
                                account.AccountData.set(result, tmpMap);
                                account.savaInstance(ApplicationController.getInstance().getApplicationContext());

                                //変更完了Tweet
                                Reply(username,MakeClass(aff) + "から" + MakeClass(tweet)+"に登録情報が更新されました.", id, dm);
                                jouhou(username, id, dm, tweet);
                            }
                        }
                    }
                } else {
                    //登録失敗Tweet
                    Reply(username, "登録に失敗しました.Tweetの形式が間違っている可能性があります.\nユーザー登録・変更は、「登録」と所属(例:4D,1S2)を含めて送信して下さい。", id, dm);
                }
            } else if (tweet.contains("情報")){
                //情報請求
                account = AccountData.getInstance(ApplicationController.getInstance().getApplicationContext());
                int result = account.Searchid(userid);
                if (result == -1){
                    //未登録ユーザー
                    Reply(username, "このアカウントは登録されていません.ユーザー登録・変更は、「登録」と所属(例:4D,1S2)を含めて送信して下さい.", id, dm);
                } else {
                    //既存ユーザ
                    tmpMap = account.AccountData.get(result);
                    String aff = tmpMap.get("clas");

                    jouhou(username, id, dm, aff);

                }
            } else if (tweet.contains("解除")){
                //登録解除
                account = AccountData.getInstance(ApplicationController.getInstance().getApplicationContext());
                int result = account.Searchid(userid);
                if (result == -1){
                    //未登録ユーザー
                    Reply(username, "このアカウントは登録されていません.", id, dm);
                } else {
                    //既存ユーザ
                    account.AccountData.remove(result);
                    account.savaInstance(ApplicationController.getInstance().getApplicationContext());

                    //解除完了Tweet
                    Reply(username, "登録解除が完了しました.", id, dm);
                }
            } else if(tweet.contains("上上下下左右左右BA")){
                    Reply(username, "イキスギィ！ｲｸｲｸｲｸ(≧Д≦)ンアッー！", id, dm);
            } else if(tweet.contains("全データ")) {
                pref = Preferences.getInstance(ApplicationController.getInstance().getApplicationContext());
                if (pref.PrefData.size() == 0){
                    Reply(username, "明日以降に掲載されている休講・授業変更等の情報はありません.", id, dm);
                } else {
                    int limit;
                    if (dm) {
                        limit = 10000;
                    } else {
                        limit = 140;
                    }
                    String message = "現在掲載されている情報は" + pref.PrefData.size() + "件です.";
                    int count = username.length() + message.length() + 2;
                    for (int i = 0; i < pref.PrefData.size(); i++) {
                        tmpMap = pref.PrefData.get(i);
                        String date = tmpMap.get("date");
                        String type = tmpMap.get("type");
                        String term = tmpMap.get("term");
                        String cont = tmpMap.get("cont");
                        String cla = tmpMap.get("clas");
                        String line = "\n【" + type + "】" + date + " " + term + "限 " + cla + " \n" + cont;
                        if (count + line.length() <= limit) {
                            message += line;
                            count += line.length();
                        } else {
                            Reply(username, message, id, dm);
                            if (i < pref.PrefData.size()) {
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
                        Reply(username, message, id, dm);
                    }
                }
            } else {
                if (dm){
                    Reply(username, "メッセージを認識出来ませんでした.\nユーザー登録・変更は,「登録」と所属(例:4D,1S2)を含めて,\n情報請求は「情報」を含めて,\n登録解除は「解除」を含めて送信して下さい.", id, true);
                } else {
                    Reply(username, "リプライを認識出来ませんでした.\nユーザー登録・変更は、「登録」と所属(例:4D,1S2)を含めて,\n情報請求は「情報」を含めて,\n登録解除は「解除」を含めてリプライして下さい。", id, false);

                }
            }
        }

        private void jouhou(String username, long id, boolean dm, String aff){
            String grade = aff.substring(0, 1);
            String dep = aff.substring(1,2);
            String clas;
            if (aff.length() == 2){
                clas = null;
            } else {
                clas = aff.substring(2,3);
            }
            Preferences pref = Preferences.getInstance(ApplicationController.getInstance().getApplicationContext());
            List<Map<String,String>> filterList = pref.GetfilterList(grade, dep, clas);
            if (filterList.size() == 0){
                if (clas != null){
                    Reply(username, "明日以降に" + grade + "の" + clas + ", " + grade + "年" + dep + "科で掲載されている休講・授業変更等の情報はありません.", id, dm);
                } else {
                    Reply(username, "明日以降に" + grade + "年" + dep + "科で掲載されている休講・授業変更等の情報はありません.", id, dm);
                }
            } else {

                int limit;
                if (dm){
                    limit = 10000;
                } else {
                    limit = 140;
                }
                String message = "現在" + aff + "で掲載されている情報は" + filterList.size() + "件です.";
                int count = username.length() + message.length() + 2;
                Map<String,String> tmpMap;
                for (int i = 0; i < filterList.size(); i++) {
                    tmpMap = filterList.get(i);
                    String date = tmpMap.get("date");
                    String type = tmpMap.get("type");
                    String term = tmpMap.get("term");
                    String cont = tmpMap.get("cont");
                    String cla = tmpMap.get("clas");
                    String line = "\n【" + type + "】" + date + " " + term + "限 " + cla + " \n" + cont;
                    if (count + line.length() <= limit) {
                        message += line;
                        count += line.length();
                    } else {
                        Reply(username, message, id, dm);
                        Log.d("reply", "limit="+limit+"\n"+message);
                        if (i < filterList.size()){
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
                    Reply(username, message, id, dm);
                    Log.d("reply2", "limit="+limit+"\n"+message);
                }
            }

        }

        @Override
        public void onDeletionNotice(long l, long l1) {

        }

        @Override
        public void onFriendList(long[] longs) {

        }

        @Override
        public void onFavorite(User user, User user1, Status status) {

        }

        @Override
        public void onUnfavorite(User user, User user1, Status status) {

        }

        @Override
        public void onFollow(User user, User user1) {

        }

        @Override
        public void onUnfollow(User user, User user1) {

        }

        //DirectMessageを受信した時に呼び出される
        @Override
        public void onDirectMessage(DirectMessage directMessage) {
            long id = directMessage.getId(); //. ツイートID
            String text = directMessage.getText(); //. ツイート本文
            long userid = directMessage.getSenderId(); //. ユーザーID
            String username = directMessage.getSenderScreenName(); //. ユーザー表示名
            Date created = directMessage.getCreatedAt(); //. ツイート日時

            Log.i("DirectMessage", "id = " + id + ", userid = " + userid + ", username = " + username + ", text = " + text + ", Date = " + created);

            if (userid != 4763208625.0){
                TweetContains(id, text, userid, username, true);
            } else {
                if(text.contains("一斉配信")){
                    AccountData account;
                    Preferences pref;
                    account = AccountData.getInstance(ApplicationController.getInstance().getApplicationContext());
                    pref = Preferences.getInstance(ApplicationController.getInstance().getApplicationContext());
                    Map<String,String> tmpMap;
                    text = text.substring(6-1);
                    for (int i = 0; i < account.AccountData.size(); i++) {
                        String mes = text;
                        tmpMap = account.AccountData.get(i);
                        User user = null;
                        try {
                            user = Tweet.twitter.showUser(Long.valueOf(tmpMap.get("id")));
                        } catch (TwitterException e) {
                            e.printStackTrace();
                        }
                        assert user != null;
                        String name = user.getScreenName();

                        int count = name.length() + 2;

                        Boolean dm = tmpMap.containsKey("dm");

                        int limit;
                        if (dm){
                            limit = 10000;
                        } else {
                            limit = 140;
                        }

                        while(count + mes.length() >= limit){
                            pref.TweetQueue.add("@" + name + " " + mes.substring(0,limit-count));
                            mes = mes.substring(limit-count);
                        }

                        if (!mes.isEmpty()) {
                            if (dm){
                                pref.DMQueue.add(new String[]{name, mes});
                            } else {
                                pref.TweetQueue.add("@" + name + " " + mes);
                            }
                        }
                    }
                    pref.savaInstance(ApplicationController.getInstance().getApplicationContext());
                }
                if (text.contains("キュー確認")){
                    Preferences pref = Preferences.getInstance(ApplicationController.getInstance().getApplicationContext());
                    String mes = "";
                    if (pref.TweetQueue.peek() != null){
                        mes += "Tweet: " + pref.TweetQueue.size() + "件";
                        while (pref.TweetQueue.peek() != null) {
                            mes += "\n" + pref.TweetQueue.poll();
                        }

                    }
                    if (pref.DMQueue.peek() != null){
                        if (!mes.isEmpty()){
                            mes += "\n\n";
                        }
                        mes += "DM: " + pref.DMQueue.size() + "件";
                        while (pref.DMQueue.peek() != null){
                            mes += "\n" + pref.DMQueue.peek()[0] + ":" + pref.DMQueue.poll()[1];
                        }
                    }
                    if (!mes.isEmpty()){
                        pref = Preferences.getInstance(ApplicationController.getInstance().getApplicationContext());
                        pref.DMQueue.add(new String[]{username, mes});
                        pref.savaInstance(ApplicationController.getInstance().getApplicationContext());
                    } else {
                        pref = Preferences.getInstance(ApplicationController.getInstance().getApplicationContext());
                        pref.DMQueue.add(new String[]{username, "キュー無し"});
                        pref.savaInstance(ApplicationController.getInstance().getApplicationContext());
                    }
                }
                if (text.contains("キュー削除")){
                    Preferences pref = Preferences.getInstance(ApplicationController.getInstance().getApplicationContext());
                    pref.TweetQueue = new ArrayDeque<>();
                    pref.DMQueue.add(new String[]{username, "削除成功"});
                    pref.savaInstance(ApplicationController.getInstance().getApplicationContext());
                }
            }
        }

        @Override
        public void onUserListMemberAddition(User user, User user1, UserList userList) {

        }

        @Override
        public void onUserListMemberDeletion(User user, User user1, UserList userList) {

        }

        @Override
        public void onUserListSubscription(User user, User user1, UserList userList) {

        }

        @Override
        public void onUserListUnsubscription(User user, User user1, UserList userList) {

        }

        @Override
        public void onUserListCreation(User user, UserList userList) {

        }

        @Override
        public void onUserListUpdate(User user, UserList userList) {

        }

        @Override
        public void onUserListDeletion(User user, UserList userList) {

        }

        @Override
        public void onUserProfileUpdate(User user) {

        }

        @Override
        public void onUserSuspension(long l) {

        }

        @Override
        public void onUserDeletion(long l) {

        }

        @Override
        public void onBlock(User user, User user1) {

        }

        @Override
        public void onUnblock(User user, User user1) {

        }

        @Override
        public void onRetweetedRetweet(User user, User user1, Status status) {

        }

        @Override
        public void onFavoritedRetweet(User user, User user1, Status status) {

        }

        @Override
        public void onQuotedTweet(User user, User user1, Status status) {

        }

        private void Reply(String username, String message, long id, boolean dm){
            if (dm){
               Tweet.SendDirectMessage(username,message);
            } else {
                try {
                    Tweet.ReplyTweet(username, message, id);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}


