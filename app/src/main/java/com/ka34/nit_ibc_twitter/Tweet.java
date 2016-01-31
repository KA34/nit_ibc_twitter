package com.ka34.nit_ibc_twitter;

import android.content.Context;

import twitter4j.DirectMessage;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;
import android.util.Log;

public class Tweet {

    static Twitter twitter;
    // ここは別途OAuth認証して情報を取得する。。。
    public static final String oAuthConsumerKey = "";
    public static final String oAuthConsumerSecret = "";
    public static final String oAuthAccessToken = "";
    public static final String oAuthAccessTokenSecret = "";

    public Tweet(Context context){
        ConfigurationBuilder builder = new ConfigurationBuilder();
        {
            // アプリ固有の情報
            builder.setOAuthConsumerKey(oAuthConsumerKey);
            builder.setOAuthConsumerSecret(oAuthConsumerSecret);
            // アプリ＋ユーザー固有の情報
            builder.setOAuthAccessToken(oAuthAccessToken);
            builder.setOAuthAccessTokenSecret(oAuthAccessTokenSecret);
        }

        TwitterFactory tf = new TwitterFactory(builder.build());
        twitter = tf.getInstance();
    }

    public static Status exetweet(final String tw) {
        // Twitter4Jに対してOAuth情報を設定
        final Status[] status = new Status[1];
        new Thread(new Runnable() {
            @Override
            public void run() {
                //ついーとしてみる
                try {
                    status[0] = twitter.updateStatus(tw);
                } catch (TwitterException e) {
                    Preferences pref = Preferences.getInstance(ApplicationController.getInstance().getApplicationContext());
                    pref.TweetQueue.add(tw);
                    pref.savaInstance((ApplicationController.getInstance().getApplicationContext()));
                    Log.i("Tweet", "saved" + tw);
                    e.printStackTrace();
                }
            }
        }).start();
        return status[0];
    }

    /**
     * リプライを送信するメソッド
     * @param userName 送信先のユーザー名
     * @param Message ツイートするメッセージ
     * @throws TwitterException
     */
    public static void ReplyTweet (String userName, String Message) throws TwitterException {
        exetweet("@" + userName + " " + Message);
//        System.out.println("Reply [" + status.getText() + "].");
    }

    /**
     * リプライを送信するメソッド
     * @param id 送信先のid
     * @param Message ツイートするメッセージ
     * @throws TwitterException
     */
    public static void ReplyTweet (long id, String Message) throws TwitterException {
        User user = twitter.showUser(id);
        exetweet("@" + user.getScreenName() + " " + Message);
//        System.out.println("Reply [" + status.getText() + "].");
    }

    /**
     * 特定のツイートに向けたリプライを送信するメソッド
     * @param message 送信するメッセージ
     * @param statusId 送信先のツイートのStatus Id
     * @throws TwitterException
     */
    public static void ReplyTweet (final String username, final String message, final long statusId) throws TwitterException {
        final Status[] status = new Status[1];
        new Thread(new Runnable() {
            @Override
            public void run() {
                //ついーとしてみる
                try {
                    try {
                        StatusUpdate su = new StatusUpdate("@" + username + " " + message);
                        su.setInReplyToStatusId(statusId);
                        Log.d(message, String.valueOf(statusId));
                        status[0] = twitter.updateStatus(su);
                    } catch (NullPointerException e){
                        ReplyTweet(username, message, statusId);
                    }
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            }
        }).start();
//        System.out.println("Reply [" + status[0].getText() + "].");
    }

    /**
     * 特定のツイートに向けたリプライを送信するメソッド
     * @param message 送信するメッセージ
     * @param status 送信先のツイートのStatus
     */
    public static void ReplyTweet (String username, String message, Status status) throws TwitterException {
        ReplyTweet(username, message, status.getId());
    }

    public static void SendDirectMessage(final String username, final String message) {
        final DirectMessage[] dm = new DirectMessage[1];
        new Thread(new Runnable() {
            @Override
            public void run() {
                //ついーとしてみる
                try {
                    dm[0] = twitter.sendDirectMessage(username, message);
                } catch (TwitterException e) {
                    Preferences pref = Preferences.getInstance(ApplicationController.getInstance().getApplicationContext());
                    pref.DMQueue.add(new String[]{username, message});
                    pref.savaInstance((ApplicationController.getInstance().getApplicationContext()));
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
