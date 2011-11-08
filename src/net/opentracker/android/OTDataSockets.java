/*
 *  Copyright (C) 2011 Opentracker.net 
 * 
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 * 
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 * 
 *  The full license is located at the root of this distribution
 *  in the LICENSE file.
 *
 *  Please report bugs to support@opentracker.net
 *
 */
package net.opentracker.android;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.WebView;

public class OTDataSockets {

    private static final String TAG = OTDataSockets.class.getName();

    public static String getIpAddress() {
        Log.v(TAG, "getIpAddress()");

        try {
            for (Enumeration<NetworkInterface> en =
                    NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr =
                        intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
        }
        return null;
    }

    public static String getWifiInfo(final Context appContext) {
        Log.v(TAG, "getWifiInfo()");

        try {
            WifiManager wifiManager =
                    (WifiManager) appContext
                            .getSystemService(Context.WIFI_SERVICE);

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            Log.d("getWifiInfo(): ", wifiInfo.toString());
            return wifiInfo.toString();
        } catch (SecurityException ise) {
            Log.w(TAG, ise);
        }
        return "unknown";
    }

    public static String getUserAgent(final Context appContext) {
        Log.v(TAG, "getUserAgent()");
        // getting the user agent
        // http://developer.android.com/reference/android/webkit/WebSettings.html#getUserAgentString()
        // http://stackoverflow.com/questions/5638749/get-user-agent-in-my-app-which-doesnt-contain-a-webview
        WebView view = new WebView(appContext);
        return view.getSettings().getUserAgentString();
    }

    public static String getScreenWidth(final Context appContext) {
        Log.v(TAG, "getScreenWidth()");
        // http://stackoverflow.com/questions/1016896/android-how-to-get-screen-dimensions
        WindowManager windowManager =
                (WindowManager) appContext
                        .getSystemService(Context.WINDOW_SERVICE);
        int width = windowManager.getDefaultDisplay().getWidth();
        // int height = windowManager.getDefaultDisplay().getHeight();
        return "" + width;
    }

    public static String getScreenHeight(final Context appContext) {
        Log.v(TAG, "getScreenHeight()");
        // http://stackoverflow.com/questions/1016896/android-how-to-get-screen-dimensions
        WindowManager windowManager =
                (WindowManager) appContext
                        .getSystemService(Context.WINDOW_SERVICE);
        // int width = windowManager.getDefaultDisplay().getWidth();
        int height = windowManager.getDefaultDisplay().getHeight();
        return "" + height;
    }

    public static String getLocation(final Context appContext) {
        LocationManager locMgr =
                (LocationManager) appContext
                        .getSystemService(Context.LOCATION_SERVICE);
        return null;
    }

    public static boolean isNetworkAvailable(final Context appContext) {
        ConnectivityManager cm =
                (ConnectivityManager) appContext
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();

        return (info != null);
    }

    // public static String getResolution() {
    // // display mangaer Obj gets the resolution of the screen
    // //
    // http://stackoverflow.com/questions/1016896/android-how-to-get-screen-dimensions
    // DisplayMetrics dm = new DisplayMetrics();
    // getWindowManager().getDefaultDisplay().getMetrics(dm);
    // return dm.widthPixels + " x " + dm.heightPixels;
    // }

    //TODO: return wifi, mobile or airplane.
    public static String getNetworkType(final Context appContext) {
        Log.v(TAG, "getNetworkType()");
        try {

            ConnectivityManager connectivityManager =
                    (ConnectivityManager) appContext
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo =
                    connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo.getTypeName();

        } catch (SecurityException ise) {
            Log.w(TAG, ise);
        }
        return "unknown";
    }

    //TODO: return wifi, mobile or airplane.
    public static String getNetwork(final Context appContext) {
        Log.v(TAG, "getNetwork()");
        try {
//            TelephonyManager teleMan =
//                    (TelephonyManager) appContext
//                            .getSystemService(Context.TELEPHONY_SERVICE);
//            int networkType = teleMan.getNetworkType();
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) appContext
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo =
                    connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return "wifi";
            }
//            switch (networkType) {
//            case TelephonyManager.NETWORK_TYPE_CDMA:
//                return "cdma";
//            case TelephonyManager.NETWORK_TYPE_EDGE:
//                return "edge";
//            case TelephonyManager.NETWORK_TYPE_UMTS:
//                return "umts";
//            case TelephonyManager.NETWORK_TYPE_HSDPA:
//                return "hsdpa";
//            default:
//                return "unknown";
//            }
        } catch (SecurityException ise) {
            Log.w(TAG, ise);
        }
        return "no network";
    }

    /**
     * Gets the pretty string for this application's version.
     * 
     * @param appContext
     *            The context used to examine packages
     * @return The application's version as a pretty string
     */
    public static String getAppVersion(final Context appContext) {
        Log.v(TAG, "getAppVersion()");
        PackageManager pm = appContext.getPackageManager();

        try {
            return pm.getPackageInfo(appContext.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "unknown";
        }
    }

}