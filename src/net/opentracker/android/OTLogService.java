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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

/**
 * OTLogService provides static utility methods to log activity to Opentracker's
 * logging/ analytics engines for an Android device.
 * 
 */
public class OTLogService {

    private static final String address = "http://log.opentracker.net/";

    private static Context appContext;

    private static String appName;

    // used for testing
    private static Boolean directSend = false;

    private static Handler handler = new Handler();

    private static OTFileUtils otFileUtil;

    // the time to lapse before creating a new session
    private static final int sessionLapseTimeMs = 30 * 1000; // m x s x ms

    private static final String TAG = OTLogService.class.getName();

    private static void appendDataToFile(HashMap<String, String> keyValuePairs)
            throws IOException {
        Log.v(TAG, "appendDataToFile()");

        // HashMap<String, String> map = otFileUtil.getFileNameDataPair();
        // Iterator<Entry<String, String>> itAdditionalMap =
        // additionalMap.entrySet().iterator();
        // while (itAdditionalMap.hasNext()) {
        // Map.Entry<String, String> pair =
        // (Map.Entry<String, String>) itAdditionalMap.next();
        // map.put(pair.getKey().toString(), pair.getValue().toString());
        // }
        // HttpPost post = new HttpPost(address);
        String urlQuery = "";

        keyValuePairs.put("t_ms", "" + System.currentTimeMillis());

        for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            urlQuery +=
                    URLEncoder.encode(key) + "=" + URLEncoder.encode(value)
                            + "&";
        }

        // Iterator<Entry<String, String>> it =
        // additionalMap.entrySet().iterator();
        // List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        // while (it.hasNext()) {
        // Map.Entry<String, String> pair =
        // (Map.Entry<String, String>) it.next();
        // System.out.println(pair.getKey() + " = " + pair.getValue());
        // pairs.add(new BasicNameValuePair(pair.getKey().toString(), pair
        // .getValue().toString()));
        // urlQuery +=
        // pair.getKey().toString() + "="
        // + URLEncoder.encode(pair.getValue().toString())
        // + "&";
        // }

        String url = address + "?" + urlQuery;
        Log.i(TAG, "appending url:" + url);
        try {
            otFileUtil.makeFile(OTFileUtils.UPLOAD_PATH, "fileToSend");
            otFileUtil.appendToFile(OTFileUtils.UPLOAD_PATH, "fileToSend", url);
        } catch (IOException e) {
            Log.i(TAG, "Exception while appending data to file: " + e);

            HashMap<String, String> logMap = new HashMap<String, String>();
            logMap.put("si", "errors"); // log to error appName
            logMap.put("message", getStackTrace(e));
            OTSend.send(logMap);
        }
    }

    private static void compressAndUploadData() {

        Log.v(TAG, "compressAndUploadData()");
        /*
         * String[] fileContents = otFileUtil.readFileByLine("OTDir",
         * "fileToSend"); for (int i = 0; i < fileContents.length; i++)
         * OTSend.send(fileContents[i]);
         */
        // String[] fileContents;
        try {
            // fileContents = otFileUtil.readFileLines("OTDir", "fileToSend");
            // Log.i(TAG, "Http requests in the file: " + fileContents.length);

            otFileUtil.compressFile(OTFileUtils.UPLOAD_PATH, "fileToSend");

            // HashMap<String, String> map = new HashMap<String, String>();
            // map.put("zip", otFileUtil.readFile(OTFileUtils.UPLOAD_PATH,
            // "fileToSend.gz"));

            long time1 = System.currentTimeMillis();
            boolean success =
                    OTSend.uploadFile(otFileUtil
                            .getInternalPath(OTFileUtils.UPLOAD_PATH),
                            "fileToSend.gz");

            long time2 = System.currentTimeMillis();
            Log.i(TAG, "Time taken to response:" + (time2 - time1));
            if (success) {
                otFileUtil.removeFile(OTFileUtils.UPLOAD_PATH, "fileToSend.gz");
                otFileUtil.emptyFile(OTFileUtils.UPLOAD_PATH, "fileToSend");
                Log.i(TAG, "cleared file");
            } else {
                otFileUtil.removeFile(OTFileUtils.UPLOAD_PATH, "fileToSend.gz");
                Log.i(TAG, "File did not empty!");
            }
        } catch (FileNotFoundException fnfe) {
            // nothing to do
            Log.i(TAG, "File not found!");
        } catch (IOException e) {
            Log.i(TAG, "IOException!");
            e.printStackTrace();
        }

    }

    public static void endSession() {
        Log.v(TAG, "endSession()");
    }

    /**
     * Get the app name being logged being logged by Opentracker's logging/
     * analytics engines for an Android device, this will be the app name you
     * have registered at Opentracker's website.
     * 
     * @return The app name
     */

    protected static String getAppName() {
        Log.v(TAG, "getAppName()");
        return appName;
    }

    /**
     * This class is useful for formatting the StackTrace as a string and before
     * passing it on to error collectors.
     * 
     * @param e
     *            The exception to get the stack trace from.
     * @return The string representation of the stack trace
     */
    private static String getStackTrace(Exception e) {
        Log.v(TAG, "getStackTrace()");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return (sw.toString());
    }

    /**
     * Initializes the Logging Service with the activities's Context object, and
     * the Opentracker's appName name received in the trail e-mail.
     */
    public static void onCreate(final Context appContext, final String appName) {
        Log.v(TAG, "onCreate()");

        OTLogService.appContext = appContext;
        OTLogService.appName = appName;
        otFileUtil = new OTFileUtils(appContext);
    }

    public static void onPause() {
        Log.e(TAG, "onPause()");

        compressAndUploadData();
    }

    // public static void sendEvent(HashMap<String, String> keyValuePairs) {
    // Log.v(TAG, "sendEvent()");
    // sendEvent(keyValuePairs, true);
    // }

    /**
     * Registers data related to this session and/ or user. Method ensures this
     * information is saved in the files otui and ots. Data is used to hold
     * session state variables; for instance the number of events in a session/
     * when the session started and ended, and/ or when the last session
     * happened.
     * 
     * For more information please see:
     * http://api.opentracker.net/api/inserts/insert_event.jsp
     * 
     * @return then number of events in current session
     */
    private static int registerSessionEvent() {
        Log.v(TAG, "registerSessionEvent()");

        // make the users data file
        try {
            otFileUtil.makeFile("otui");
        } catch (IOException e) {
            Log.e(TAG, "Can't make file otui");
            HashMap<String, String> logMap = new HashMap<String, String>();
            logMap.put("si", "errors"); // log to error appName
            logMap.put("message", "Can't make file otui");
            logMap.put("exception", getStackTrace(e));
            OTSend.send(logMap);
        }

        // same thing for session data
        try {
            otFileUtil.makeFile("ots");
        } catch (IOException e) {
            Log.v(TAG, "Can't make file ots");
            HashMap<String, String> logMap = new HashMap<String, String>();
            logMap.put("si", "errors"); // log to error appName
            logMap.put("message", "Can't make file ots");
            logMap.put("exception", getStackTrace(e));
            OTSend.send(logMap);
        }

        // read the users data file
        String otUserData = null;
        try {
            otUserData = otFileUtil.readFile("otui");
        } catch (IOException e) {
            Log.e(TAG, "Can't read file otui");
            HashMap<String, String> logMap = new HashMap<String, String>();
            logMap.put("si", "errors"); // log to error appName
            logMap.put("message", "Can't read file otui");
            logMap.put("exception", getStackTrace(e));
            OTSend.send(logMap);
        }
        Log.v(TAG, "otui read: " + otUserData);

        // read the session data
        String otSessionData = null;
        try {
            otSessionData = otFileUtil.readFile("ots");
        } catch (IOException e) {
            Log.e(TAG, "Can't read file ots");
            HashMap<String, String> logMap = new HashMap<String, String>();
            logMap.put("si", "errors"); // log to error appName
            logMap.put("message", "Can't read file ots");
            logMap.put("exception", getStackTrace(e));
            OTSend.send(logMap);
        }

        // create default/ initial user data
        int randomNumberClient = (int) (1000 * Math.random());

        long currentUnixTimestampMs = System.currentTimeMillis();

        long firstSessionStartUnixTimestamp = currentUnixTimestampMs;
        long previousSessionStartUnixTimestamp = currentUnixTimestampMs;
        long currentSessionStartUnixTimestamp = currentUnixTimestampMs;
        int sessionCount = 1;
        int lifeTimeEventCount = 1;

        if (otUserData != null) {

            // initialize the data
            String[] userData = otUserData.split("\\.");
            if (userData.length != 6) {

                // handle corruption use initialized values
                Log.i(TAG, "Data is corrupted length: " + userData.length
                        + ", userData:" + otUserData);

                HashMap<String, String> logMap = new HashMap<String, String>();
                logMap.put("si", "errors"); // log to error appName
                logMap.put("message", "Got corrupt otui, wrong length.");
                logMap.put("userData.length", "" + userData.length);
                logMap.put("otUserData", otUserData);
                OTSend.send(logMap);

            } else {

                // as per
                // http://api.opentracker.net/api/inserts/browser/reading_cookie.jsp

                // _otui <random number client site>. <first visit start unix
                // timestamp>. <previous visit start unix timestamp>. <current
                // visit start unix timestamp>. <session count>. <life time
                // event view count>

                try {
                    // parse the user data
                    randomNumberClient = Integer.parseInt(userData[0]);

                    firstSessionStartUnixTimestamp =
                            Long.parseLong(userData[1]);

                    previousSessionStartUnixTimestamp =
                            Long.parseLong(userData[2]);

                    currentSessionStartUnixTimestamp =
                            Long.parseLong(userData[3]);

                    sessionCount = Integer.parseInt(userData[4]);

                    lifeTimeEventCount = Integer.parseInt(userData[5]);

                    // update the event count
                    lifeTimeEventCount++;

                } catch (Exception e) {

                    Log.i(TAG, "otui has corrupted data: " + e);

                    HashMap<String, String> logMap =
                            new HashMap<String, String>();
                    logMap.put("si", "errors"); // log to error appName
                    logMap.put("message", getStackTrace(e));
                    OTSend.send(logMap);

                    // handle corruption: reinitialize everything
                    randomNumberClient = (int) (1000 * Math.random());
                    firstSessionStartUnixTimestamp = currentUnixTimestampMs;
                    previousSessionStartUnixTimestamp = currentUnixTimestampMs;
                    currentSessionStartUnixTimestamp = currentUnixTimestampMs;
                    sessionCount = 1;
                    lifeTimeEventCount = 1;

                }
            }
        }

        // Create data with initial parameters
        int sessionEventCount = 1;
        long previousEventUnixTimestamp = currentUnixTimestampMs;

        boolean isNewSession = true;
        if (otSessionData != null) {
            // initialize the data
            String[] sessionData = otSessionData.split("\\.");
            if (sessionData.length != 4) {

                Log.i(TAG, "Data is corrupted length: " + sessionData.length
                        + ", sessionData:" + otSessionData);

                HashMap<String, String> logMap = new HashMap<String, String>();
                logMap.put("si", "errors"); // log to error appName
                logMap.put("message", "Got corrupt ots, wrong length.");
                OTSend.send(logMap);

                // data is corrupted, using initialized data

            } else {

                try {

                    // as per
                    // http://api.opentracker.net/api/inserts/browser/reading_cookie.jsp

                    // _ots <session event view count>. <current visit start
                    // unix timestamp>. <previous event view unix timestamp>.
                    // <current event view unix timestamp>

                    previousEventUnixTimestamp = Long.parseLong(sessionData[2]);
                    long diff =
                            (currentUnixTimestampMs - previousEventUnixTimestamp);
                    Log.e(TAG, "Got: " + diff + "[ms]");
                    Log.e(TAG, "Got currentUnixTimestampMs: "
                            + currentUnixTimestampMs + "[ms]");
                    Log.e(TAG, "Got previousEventUnixTimestamp: "
                            + previousEventUnixTimestamp + "[ms]");
                    // make sure we have a ongoing session
                    if (diff < sessionLapseTimeMs) {

                        Log.e(TAG, "Continuing starting.");

                        // ongoing session, parse the session data
                        sessionEventCount = Integer.parseInt(sessionData[0]);

                        // currentSessionStartUnixTimestamp =
                        // Long.parseLong(sessionData[1]);

                        // do the work, to start a new event
                        sessionEventCount++;

                        // use initial session values
                        isNewSession = false;

                    }

                    previousEventUnixTimestamp = Long.parseLong(sessionData[3]);

                } catch (Exception e) {

                    Log.i(TAG, "ots has corrupted data: " + e);

                    HashMap<String, String> logMap =
                            new HashMap<String, String>();
                    logMap.put("si", "errors"); // log to error appName
                    logMap.put("message", getStackTrace(e));
                    OTSend.send(logMap);

                    // just reinitialize everything
                    sessionEventCount = 1;
                    previousEventUnixTimestamp = currentUnixTimestampMs;
                }
            }
        }

        // do the work, to register new session
        if (isNewSession) {
            Log.e(TAG, "Updating user data with new session.");
            previousSessionStartUnixTimestamp =
                    currentSessionStartUnixTimestamp;
            currentSessionStartUnixTimestamp = currentUnixTimestampMs;
            sessionCount++;
        }

        otSessionData =
                sessionEventCount + "." + currentSessionStartUnixTimestamp
                        + "." + previousEventUnixTimestamp + "."
                        + currentUnixTimestampMs;

        try {
            otFileUtil.writeFile("ots", otSessionData);
        } catch (IOException e) {

            Log.i(TAG, "Exception while writing to ots: " + e);

            HashMap<String, String> logMap = new HashMap<String, String>();
            logMap.put("si", "errors"); // log to error appName
            logMap.put("message", getStackTrace(e));
            OTSend.send(logMap);

        }
        Log.i(TAG, "ots write: " + otSessionData);

        // format the otUserData
        otUserData =
                randomNumberClient + "." + firstSessionStartUnixTimestamp + "."
                        + previousSessionStartUnixTimestamp + "."
                        + currentSessionStartUnixTimestamp + "." + sessionCount
                        + "." + lifeTimeEventCount;

        // write the otUserData
        try {
            otFileUtil.writeFile("otui", otUserData);
        } catch (IOException e) {
            Log.i(TAG, "Exception while writing to otui: " + e);

            HashMap<String, String> logMap = new HashMap<String, String>();
            logMap.put("si", "errors"); // log to error appName
            logMap.put("message", getStackTrace(e));
            OTSend.send(logMap);
        }
        Log.v(TAG, "otui write: " + otUserData);
        return sessionEventCount;
    }

    public static void sendEvent(String eventName) {
        sendEvent(eventName, true);
    }

    public static void sendEvent(String eventName,
            boolean appendSessionStateData) {
        sendEvent(eventName, null, appendSessionStateData);
    }

    public static void sendEvent(String eventName,
            HashMap<String, String> keyValuePairs) {
        sendEvent(eventName, keyValuePairs, true);
    }

    public static void sendEvent(String eventName,
            HashMap<String, String> keyValuePairs,
            boolean appendSessionStateData) {
        Log.v(TAG, "sendEvent(" + eventName + ",  " + appendSessionStateData
                + ", " + keyValuePairs + ")");

        // if we are on wifi then start separate thread otherwise
        // just log to disk (quick enough)
        if (OTDataSockets.getNetworkType(appContext).equalsIgnoreCase("wi-fi"))
            sendTask(eventName, keyValuePairs, appendSessionStateData);
        else
            processEvent(eventName, keyValuePairs, appendSessionStateData);
    }

    private static final void processEvent(String eventName,
            HashMap<String, String> keyValuePairs,
            boolean appendSessionStateData) {

        Log.v(TAG, "processEvent(" + eventName + ",  " + appendSessionStateData
                + ", " + keyValuePairs + ")");

        if (keyValuePairs == null)
            keyValuePairs = new HashMap<String, String>();

        // ti is default title tag
        if (eventName == null) {
            if (keyValuePairs.get("title") == null) {
                keyValuePairs.put("ti", "[No title]");
            } else {
                keyValuePairs.put("ti", keyValuePairs.get("title"));
                keyValuePairs.remove("title");
            }
        } else {
            keyValuePairs.put("ti", eventName);
        }
        // update the sessionData
        int eventCount = registerSessionEvent();
        Log.d(TAG, "eventCound: " + eventCount);

        keyValuePairs.put("si", appName);
        keyValuePairs.put("platform", OTDataSockets.getPlatform());
        keyValuePairs.put("platform version", OTDataSockets
                .getPlatformVersion());
        keyValuePairs.put("device", OTDataSockets.getDevice());
        keyValuePairs.put("connection", OTDataSockets
                .getNetworkType(appContext));
        keyValuePairs.put("sh", OTDataSockets.getScreenHeight(appContext));
        keyValuePairs.put("sw", OTDataSockets.getScreenWidth(appContext));
        keyValuePairs.put("app version", OTDataSockets
                .getAppVersion(appContext));
        keyValuePairs.put("lc", "http://app.opentracker.net/" + appName + "/"
                + eventName.replace('/', '.'));

        String location = OTDataSockets.getLastCoordinates(appContext);
        if (location != null)
            keyValuePairs.put("location", location);

        if (appendSessionStateData) {
            HashMap<String, String> dataFiles = null;
            try {
                dataFiles = otFileUtil.getSessionStateDataPairs();
            } catch (IOException e) {

                Log.i(TAG, "Exception while getting fileName data pairs");

                HashMap<String, String> logMap = new HashMap<String, String>();
                logMap.put("si", "errors"); // log to error appName
                logMap.put("message", getStackTrace(e));
                OTSend.send(logMap);

            }
            if (dataFiles != null)
                keyValuePairs.putAll(dataFiles);
        }

        // TODO: work out logic of appending data to file
        Log.v(TAG, "directSend: " + directSend + ", "
                + OTDataSockets.getNetworkType(appContext));
        try {
            if (OTDataSockets.getNetworkType(appContext).equalsIgnoreCase(
                    "wi-fi")) {
                Log.e(TAG, "sending: " + keyValuePairs);

                OTSend.send(keyValuePairs);
            } else if (directSend) {
                Log.e(TAG, "sending: " + keyValuePairs);

                OTSend.send(keyValuePairs);
            } else {
                Log.e(TAG, "appending: " + keyValuePairs);

                appendDataToFile(keyValuePairs);
            }

        } catch (UnknownHostException e) {
            try {
                appendDataToFile(keyValuePairs);
            } catch (IOException e1) {
                // TODO: ignore
            }
        } catch (Exception e) {
            Log.i(TAG, "Exception while appending data to file: " + e);

            HashMap<String, String> logMap = new HashMap<String, String>();
            logMap.put("si", "errors"); // log to error appName
            logMap.put("message", getStackTrace(e));
            OTSend.send(logMap);
        }
    }

    private static void sendTask(final String event,
            final HashMap<String, String> keyValuePairs,
            final boolean appendSessionStateData) {
        // Do something long
        Runnable runnable = new Runnable() {
            public void run() {

                try {
                    processEvent(event, keyValuePairs, appendSessionStateData);
                    handler.post(new Runnable() {
                        public void run() {
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
        new Thread(runnable).start();
    }

    /**
     * Sets if the data is sent directly to the log service (directSend = true).
     * 
     * This overrides the default behavior for testing purposes.
     * 
     * The default behavior is to send the event data directly if the device is
     * connected to the Internet via WiFi (larger bandwidth). If the device is
     * not connected via WiFi the data will be sent to a file which is then sent
     * to the log service at a later time. This helps save bandwidth and helps
     * with network performance.
     * 
     * @param directSend
     *            If log service should sent event data directly, indifferent of
     *            the connection.
     */
    public static void setDirectSend(boolean directSend) {
        // OTLogService.directSend = directSend;
    }

    // public static void uploadData(HashMap<String, String> keyValuePairs)
    // throws IOException {
    // Log.v(TAG, "uploadData()");
    // otFileUtil.removeFile("testFileNamePavi");
    // otFileUtil.removeFile("testFile");
    // otFileUtil.removeFile("otFile");
    // HashMap<String, String> map = otFileUtil.getFileNameDataPair();
    // Iterator<Entry<String, String>> it =
    // keyValuePairs.entrySet().iterator();
    // while (it.hasNext()) {
    // Map.Entry<String, String> pair =
    // (Map.Entry<String, String>) it.next();
    // map.put(pair.getKey().toString(), pair.getValue().toString());
    // }

    // String success = OTSend.send(keyValuePairs);
    // if (success == null) {
    // appendDataToFile(keyValuePairs);
    // }
    // }

    /**
     * Can not create object from out side of the class, utility static methods.
     */
    private OTLogService() {
        Log.v(TAG, "OTLogService()");
    }

}
