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
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.WebView;

public class OTDataSockets {

    // TODO: return wifi, mobile or airplane.
    @SuppressWarnings("unchecked")
    private static HashMap cacheType = new HashMap();

    private static final long EXPIRE_MS = 5 * 1000l * 60l;

    // private static OTLocationService locationService = null;

    private static final String TAG = OTDataSockets.class.getName();

    /**
     * Gets the pretty string for this application's version.
     * 
     * @param appContext
     *            The context used to examine packages
     * @return The application's version as a pretty string
     */
    public static String getAppVersion(final Context appContext) {

        long t0 = System.currentTimeMillis();
        Log.v(TAG, "getAppVersion()");
        PackageManager pm = appContext.getPackageManager();

        try {

            t0 = System.currentTimeMillis() - t0;
            Log.d(TAG, t0 + "[ms]");
            return pm.getPackageInfo(appContext.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {

            t0 = System.currentTimeMillis() - t0;
            Log.d(TAG, t0 + "[ms]");
            return "unknown";
        }
    }

    public static String getDevice() {
        long t0 = System.currentTimeMillis();
        String model = android.os.Build.MODEL;
        t0 = System.currentTimeMillis() - t0;
        Log.d(TAG, t0 + "[ms]");
        return model;
    }

    public static String getIpAddress() {

        long t0 = System.currentTimeMillis();
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
                        Log.d(TAG, t0 + "[ms]");
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
        }
        return null;
    }

    public static String getLastCoordinates(final Context appContext) {
        long t0 = System.currentTimeMillis();
        Log.v(TAG, "getLastCoordinates()");

        LocationManager locationManager =
                (LocationManager) appContext
                        .getSystemService(Context.LOCATION_SERVICE);

        // try to get the latest
        Location tmpLocation =
                locationManager
                        .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (tmpLocation != null) {

            Log.e(TAG, "getNetworkCoordinates: " + tmpLocation.getAccuracy());
            Log.e(TAG, "getNetworkCoordinates: "
                    + new Date(tmpLocation.getTime()));
            Log.d(TAG, t0 + "[ms]");

            // dont return default 0,0 values sometimes seen
            if (tmpLocation.getLatitude() != 0f
                    && tmpLocation.getLongitude() != 0) {
                return tmpLocation.getLatitude() + ", "
                        + tmpLocation.getLongitude();
            } else {
                return null;
            }
        } else {
            Log.d(TAG, t0 + "[ms]");
            return null;
        }
    }


    @SuppressWarnings("unchecked")
    public static String getNetworkType(final Context appContext) {

        long t0 = System.currentTimeMillis();
        Log.v(TAG, "getNetworkType()");
        if (cacheType.get("last modified time") != null
                && ((Long) cacheType.get("last modified time")) > System
                        .currentTimeMillis()
                        - EXPIRE_MS) {
            Log.d(TAG, (String) cacheType.get("networkType"));

            t0 = System.currentTimeMillis() - t0;
            Log.d(TAG, t0 + "[ms]");

            return (String) cacheType.get("networkType");
        }
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) appContext
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo =
                    connectivityManager.getActiveNetworkInfo();

            String networkInfoTypeName = activeNetworkInfo.getTypeName();
            if (networkInfoTypeName.equalsIgnoreCase("wifi")) {
                networkInfoTypeName = "Wi-Fi";
            } else {
                networkInfoTypeName = networkInfoTypeName.toLowerCase();
            }
            cacheType.put("networkType", networkInfoTypeName);
            cacheType.put("last modified time", System.currentTimeMillis());
            Log.d(TAG, (String) cacheType.get("networkType"));

            t0 = System.currentTimeMillis() - t0;
            Log.d(TAG, t0 + "[ms]");
            return (String) cacheType.get("networkType");
        } catch (SecurityException ise) {
            Log.w(TAG, ise);
        }
        cacheType.put("networkType", "no network");
        cacheType.put("last modified time", System.currentTimeMillis());
        Log.d(TAG, (String) cacheType.get("networkType"));

        t0 = System.currentTimeMillis() - t0;
        Log.d(TAG, t0 + "[ms]");
        return (String) cacheType.get("networkType");
    }

    public static String getPlatform() {
        return "Android";
    }

    public static String getPlatformVersion() {
        long t0 = System.currentTimeMillis();
        String release = android.os.Build.VERSION.RELEASE;
        t0 = System.currentTimeMillis() - t0;
        Log.d(TAG, t0 + "[ms]");
        return release;
    }

    public static String getScreenHeight(final Context appContext) {

        long t0 = System.currentTimeMillis();
        Log.v(TAG, "getScreenHeight()");
        // http://stackoverflow.com/questions/1016896/android-how-to-get-screen-dimensions
        WindowManager windowManager =
                (WindowManager) appContext
                        .getSystemService(Context.WINDOW_SERVICE);
        // int width = windowManager.getDefaultDisplay().getWidth();
        int height = windowManager.getDefaultDisplay().getHeight();

        t0 = System.currentTimeMillis() - t0;
        Log.d(TAG, t0 + "[ms]");

        return "" + height;
    }

    public static String getScreenWidth(final Context appContext) {

        long t0 = System.currentTimeMillis();
        Log.v(TAG, "getScreenWidth()");
        // http://stackoverflow.com/questions/1016896/android-how-to-get-screen-dimensions
        WindowManager windowManager =
                (WindowManager) appContext
                        .getSystemService(Context.WINDOW_SERVICE);
        int width = windowManager.getDefaultDisplay().getWidth();
        // int height = windowManager.getDefaultDisplay().getHeight();

        t0 = System.currentTimeMillis() - t0;
        Log.d(TAG, t0 + "[ms]");

        return "" + width;
    }

    public static String getUserAgent(final Context appContext) {

        long t0 = System.currentTimeMillis();
        Log.v(TAG, "getUserAgent()");
        // getting the user agent
        // http://developer.android.com/reference/android/webkit/WebSettings.html#getUserAgentString()
        // http://stackoverflow.com/questions/5638749/get-user-agent-in-my-app-which-doesnt-contain-a-webview
        WebView view = new WebView(appContext);

        t0 = System.currentTimeMillis() - t0;
        Log.d(TAG, t0 + "[ms]");

        return view.getSettings().getUserAgentString();
    }

    public static String getWifiInfo(final Context appContext) {

        long t0 = System.currentTimeMillis();
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

        t0 = System.currentTimeMillis() - t0;
        Log.d(TAG, t0 + "[ms]");

        return "unknown";
    }

    public static boolean isNetworkAvailable(final Context appContext) {

        long t0 = System.currentTimeMillis();
        ConnectivityManager cm =
                (ConnectivityManager) appContext
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();

        t0 = System.currentTimeMillis() - t0;
        Log.d(TAG, t0 + "[ms]");

        return (info != null);
    }

}