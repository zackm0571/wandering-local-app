package life.wanderinglocal;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import static life.wanderinglocal.Constants.PREFS_NAME;


public class WLPreferences {
    public static void saveStringPref(Context context, String key, String value) {
        Log.d(WLPreferences.class.getSimpleName(), String.format("Storing preference: {key: %s, value: %s", key, value));
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(key, value);
        prefs.apply();
    }

    public static String loadStringPref(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(key, "");
    }

    public static String loadStringPref(Context context, String key, String defValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(key, defValue);
    }
}
