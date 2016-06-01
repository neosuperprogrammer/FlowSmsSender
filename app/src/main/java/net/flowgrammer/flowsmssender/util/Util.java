package net.flowgrammer.flowsmssender.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;

import org.apache.http.Header;

/**
 * Created by neox on 5/16/16.
 */
public class Util {
    private static final String LOG_TAG = Util.class.getSimpleName();

    public static void saveCookie(Context context, Header[] headers) {
        for (Header header : headers) {
//            Log.i(LOG_TAG, header.getName() + " : " + header.getValue());
            if (header.getName().equalsIgnoreCase("set-cookie")) {
                String cookie = header.getValue();
                String [] cookieValues = cookie.split(";");
                for (String cookieValue : cookieValues) {
                    String [] keyValues = cookieValue.split("=");
                    String key = keyValues[0];
                    if (key.equalsIgnoreCase("connect.sid")) {
                        Log.e(LOG_TAG, "save cookie : " + keyValues[1]);
                        Setting.setCookie(context, keyValues[1]);
                        return;
                    }
                }
            }
        }
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }
}
