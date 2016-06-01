package net.flowgrammer.flowsmssender.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by neox on 5/15/16.
 */
public class Setting {

    private static final String PREFS = "prefs";

    Context mContext;
    SharedPreferences mSharedPreferences;

    public Setting(Context context) {
        mContext = context;
        mSharedPreferences = mContext.getSharedPreferences(PREFS, mContext.MODE_PRIVATE);
    }

//    public String authKey() {
////        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(PREFS, mContext.MODE_PRIVATE);
//        String auth = mSharedPreferences.getString("auth_key", "");
//        return auth;
//    }
//
//    public void setAuthKey(String authKey) {
////        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(PREFS, mContext.MODE_PRIVATE);
//        SharedPreferences.Editor e = mSharedPreferences.edit();
//        e.putString("auth_key", authKey);
//        e.commit();
//    }

    public static String cookie(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS, context.MODE_PRIVATE);
        String auth = sharedPreferences.getString("cookie", "");
        return auth;
    }

    public static void setCookie(Context context, String cookie) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS, context.MODE_PRIVATE);
        SharedPreferences.Editor e = sharedPreferences.edit();
        e.putString("cookie", cookie);
        e.commit();
    }

}
