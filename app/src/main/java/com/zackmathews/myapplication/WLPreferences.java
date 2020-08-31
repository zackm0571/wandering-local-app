package com.zackmathews.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import static com.zackmathews.myapplication.Constants.PREFS_NAME;

public class WLPreferences {
    static void saveStringPref(Context context, String key, String value) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(key, value);
        prefs.apply();
    }

    static String loadStringPref(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(key, "");
    }
    static String loadStringPref(Context context, String key, String defValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(key, defValue);
    }
}
