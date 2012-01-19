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
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

/**
 * OTLogService provides static utility methods to log activity to Opentracker's
 * logging/ analytics engines for an Android device.
 * 
 * @author $Author: eddie $ (latest svn author)
 * @version $Id: OTLogService.java 13909 2012-01-19 11:30:44Z eddie $
 */
public class OTLogService {

    private static final String address = "http://LogWrapper.opentracker.net/";

    private static Context appContext;

    private static String appName;

    // used for testing
    private static Boolean directSend = false;

    private static Handler handler = new Handler();

    private static final Object lock = new Object();

    // flag the first wifi event, to upload compressed data
    private static boolean isFirstWiFiEvent = true;

    // the time to wait after uploading the file to ensure that everything gets
    // processed before sending the next request
    private static final int GRACE_PERIOD_UPLOAD_MS = 10000;

    private static OTFileUtils otFileUtil;

    // the time to lapse before creating a new session
    private static final int sessionLapseTimeMs = 30 * 60 * 1000; // m x s x ms

    private static final String TAG = OTLogService.class.getName();

    private static void appendDataToFile(HashMap<String, String> keyValuePairs)
            throws IOException {
        LogWrapper.v(TAG, "appendDataToFile()");

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
        LogWrapper.i(TAG, "appending url:" + url);
        try {
            otFileUtil.makeFile(OTFileUtils.UPLOAD_PATH, "fileToSend");
            otFileUtil.appendToFile(OTFileUtils.UPLOAD_PATH, "fileToSend", url);
        } catch (IOException e) {
            LogWrapper.e(TAG, "Exception while appending data to file: " + e);

            HashMap<String, String> logMap = new HashMap<String, String>();
            logMap.put("si", "errors"); // log to error appName
            logMap.put("message", getStackTrace(e));
            OTSend.send(logMap);
        }
    }

    private static void compressAndUploadDataTask() {
        // Do something long
        Runnable runnable = new Runnable() {
            public void run() {

                try {
                    compressAndUploadData();
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

    /*
     * Uploads the data file to logging servers. If no data (file size is zero)
     * or networkType is "no network", then this method immediately returns,
     * doing nothing.
     * 
     * Method is synchronized with processEvent() through a private lock object.
     * This is to ensure that two threads will not process network/ information
     * events.
     */
    private static void compressAndUploadData() {

        synchronized (lock/* Get */) {

            long fileSizeBytes = 0;

            try {
                fileSizeBytes =
                        otFileUtil.getFileSize(OTFileUtils.UPLOAD_PATH,
                                "fileToSend");
            } catch (IOException e) {
                // ignore
            }

            if (fileSizeBytes != 0) {
                LogWrapper.v(TAG, "compressAndUploadData()");
            } else {
                LogWrapper.v(TAG,
                        "compressAndUploadData() escaping/ empty file");
                return;
            }

            if (OTDataSockets.getNetworkType(appContext).equals(
                    OTDataSockets.NO_NETWORK)) {
                LogWrapper.e(TAG,
                        "compressAndUploadData() escaping/ no network");
                // no network, can't upload
                return;
            }

            /*
             * String[] fileContents = otFileUtil.readFileByLine("OTDir",
             * "fileToSend"); for (int i = 0; i < fileContents.length; i++)
             * OTSend.send(fileContents[i]);
             */
            // String[] fileContents;

            LogWrapper.w(TAG, "compressAndUploadData() entering...");

            try {

                // remove any hanging files
                otFileUtil.removeFile(OTFileUtils.UPLOAD_PATH, "fileToSend.gz");

                // LogWrapper.e(TAG, "1 "
                // + otFileUtil.listFiles(OTFileUtils.UPLOAD_PATH).length);

                otFileUtil.compressFile(OTFileUtils.UPLOAD_PATH, "fileToSend");

                // LogWrapper.e(TAG, "2 "
                // + otFileUtil.listFiles(OTFileUtils.UPLOAD_PATH).length);

                long time1 = System.currentTimeMillis();
                boolean success =
                        OTSend.uploadFile(otFileUtil
                                .getInternalPath(OTFileUtils.UPLOAD_PATH),
                                "fileToSend.gz");

                long time2 = System.currentTimeMillis();
                Log
                        .v(TAG, "Time taken to upload: " + (time2 - time1)
                                + " [ms]");
                if (success) {
                    LogWrapper.v(TAG, "Clearing files");

                    otFileUtil.removeFile(OTFileUtils.UPLOAD_PATH,
                            "fileToSend.gz");
                    otFileUtil.emptyFile(OTFileUtils.UPLOAD_PATH, "fileToSend");

                    // wait 10 seconds for things to settle on the server side
                    // this will block threads using the lock object
                    try {
                        Thread.sleep(GRACE_PERIOD_UPLOAD_MS);
                    } catch (InterruptedException e) {
                        LogWrapper.e(TAG, "InterruptedException: " + e);
                    }

                } else {
                    LogWrapper.i(TAG,
                            "File upload did not succeed, re-appending!");
                    otFileUtil.removeFile(OTFileUtils.UPLOAD_PATH,
                            "fileToSend.gz");
                }
            } catch (FileNotFoundException fnfe) {
                // nothing to do
                LogWrapper.e(TAG, "File not found!");
            } catch (IOException e) {
                LogWrapper.e(TAG, "IOException!");
                // e.printStackTrace();
            }
            // LogWrapper.e(TAG, "3 " +
            // otFileUtil.listFiles(OTFileUtils.UPLOAD_PATH).length);
        }// lock
    }

    // public static void endSession() {
    // LogWrapper.v(TAG, "endSession()");
    // }

    /**
     * Get the app name being logged by Opentracker's logging/ analytics engines
     * for an Android device, this will be the app name you have registered at
     * Opentracker's website.
     * 
     * @return The app name
     */

    protected static String getAppName() {
        LogWrapper.v(TAG, "getAppName()");
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
        LogWrapper.v(TAG, "getStackTrace()");

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
        LogWrapper.v(TAG, "onCreate()");

        OTLogService.appContext = appContext;
        OTLogService.appName = appName;
        otFileUtil = new OTFileUtils(appContext);
    }

    public static void onPause() {
        LogWrapper.v(TAG, "onPause()");
        compressAndUploadDataTask();
    }

    // public static void sendEvent(HashMap<String, String> keyValuePairs) {
    // LogWrapper.v(TAG, "sendEvent()");
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
        LogWrapper.v(TAG, "registerSessionEvent()");

        // make the users data file
        try {
            otFileUtil.makeFile("otui");
        } catch (IOException e) {
            LogWrapper.e(TAG, "Can't make file otui");
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
            LogWrapper.e(TAG, "Can't make file ots");
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
            LogWrapper.e(TAG, "Can't read file otui");
            HashMap<String, String> logMap = new HashMap<String, String>();
            logMap.put("si", "errors"); // log to error appName
            logMap.put("message", "Can't read file otui");
            logMap.put("exception", getStackTrace(e));
            OTSend.send(logMap);
        }
        LogWrapper.v(TAG, "otui read: " + otUserData);

        // read the session data
        String otSessionData = null;
        try {
            otSessionData = otFileUtil.readFile("ots");
        } catch (IOException e) {
            LogWrapper.e(TAG, "Can't read file ots");
            HashMap<String, String> logMap = new HashMap<String, String>();
            logMap.put("si", "errors"); // log to error appName
            logMap.put("message", "Can't read file ots");
            logMap.put("exception", getStackTrace(e));
            OTSend.send(logMap);
        }

        // create default/ initial user data (six digits)
        int randomNumberClient = (int) (1000000 * Math.random());

        long currentUnixTimestampMs = System.currentTimeMillis();

        long firstSessionStartUnixTimestamp = currentUnixTimestampMs;
        long previousSessionStartUnixTimestamp = currentUnixTimestampMs;
        long currentSessionStartUnixTimestamp = currentUnixTimestampMs;
        int sessionCount = 0;
        int lifeTimeEventCount = 1;

        if (otUserData != null) {

            // initialize the data
            String[] userData = otUserData.split("\\.");
            if (userData.length != 6) {

                // handle corruption use initialized values
                LogWrapper.w(TAG, "Data is corrupted length: "
                        + userData.length + ", userData:" + otUserData);

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

                    LogWrapper.w(TAG, "otui has corrupted data: " + e);

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
                    sessionCount = 0;
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

                // data is corrupted, using initialized data

                LogWrapper
                        .i(TAG, "Data is corrupted length: "
                                + sessionData.length + ", sessionData:"
                                + otSessionData);

                HashMap<String, String> logMap = new HashMap<String, String>();
                logMap.put("si", "errors"); // log to error appName
                logMap.put("message", "Got corrupt ots, wrong length.");
                OTSend.send(logMap);

            } else {

                try {

                    // as per
                    // http://api.opentracker.net/api/inserts/browser/reading_cookie.jsp

                    // _ots <session event view count>. <current visit start
                    // unix timestamp>. <previous event view unix timestamp>.
                    // <current event view unix timestamp>

                    previousEventUnixTimestamp = Long.parseLong(sessionData[3]);
                    long diff =
                            (currentUnixTimestampMs - previousEventUnixTimestamp);

                    // LogWrapper.e(TAG, "Got: " + diff + "[ms]");
                    // LogWrapper.e(TAG, "Got currentUnixTimestampMs: "
                    // + currentUnixTimestampMs + "[ms]");
                    // LogWrapper.e(TAG, "Got previousEventUnixTimestamp: "
                    // + previousEventUnixTimestamp + "[ms]");

                    // make sure we have a ongoing session
                    if (diff < sessionLapseTimeMs) {

                        LogWrapper.d(TAG, "Continuing session.");

                        // ongoing session, parse the session data
                        sessionEventCount = Integer.parseInt(sessionData[0]);

                        // currentSessionStartUnixTimestamp =
                        // Long.parseLong(sessionData[1]);

                        // do the work, to start a new event
                        sessionEventCount++;

                        // use initial session values
                        isNewSession = false;

                    }

                } catch (Exception e) {

                    LogWrapper.w(TAG, "ots has corrupted data: " + e);

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
            LogWrapper.i(TAG, "Updating data with new session.");
            previousSessionStartUnixTimestamp =
                    currentSessionStartUnixTimestamp;
            currentSessionStartUnixTimestamp = currentUnixTimestampMs;
            sessionCount++;

            // empty the file
            try {
                otFileUtil.emptyFile("otpe");
            } catch (IOException e) {
                LogWrapper.e(TAG, "Empty file creation did not work:" + e);
            }
        }

        otSessionData =
                sessionEventCount + "." + currentSessionStartUnixTimestamp
                        + "." + previousEventUnixTimestamp + "."
                        + currentUnixTimestampMs;

        try {
            // LogWrapper.e(TAG, "Writing session: " + otSessionData);
            // LogWrapper.e(TAG, "Writing current: " + currentUnixTimestampMs);
            // LogWrapper.e(TAG, "Writing previous: " +
            // previousEventUnixTimestamp);
            // LogWrapper.e(TAG, "Writing current: " + new
            // Date(currentUnixTimestampMs));
            // LogWrapper.e(TAG, "Writing previous: "
            // + new Date(previousEventUnixTimestamp));

            otFileUtil.writeFile("ots", otSessionData);
        } catch (IOException e) {

            LogWrapper.w(TAG, "Exception while writing to ots: " + e);

            HashMap<String, String> logMap = new HashMap<String, String>();
            logMap.put("si", "errors"); // log to error appName
            logMap.put("message", getStackTrace(e));
            OTSend.send(logMap);

        }
        LogWrapper.v(TAG, "ots write: " + otSessionData);

        // format the otUserData
        otUserData =
                randomNumberClient + "." + firstSessionStartUnixTimestamp + "."
                        + previousSessionStartUnixTimestamp + "."
                        + currentSessionStartUnixTimestamp + "." + sessionCount
                        + "." + lifeTimeEventCount;

        // write the otUserData
        try {
            LogWrapper.v(TAG, "Writing user: " + otUserData);
            otFileUtil.writeFile("otui", otUserData);
        } catch (IOException e) {
            LogWrapper.w(TAG, "Exception while writing to otui: " + e);

            HashMap<String, String> logMap = new HashMap<String, String>();
            logMap.put("si", "errors"); // log to error appName
            logMap.put("message", getStackTrace(e));
            OTSend.send(logMap);
        }
        LogWrapper.v(TAG, "otui write: " + otUserData);
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
        long t0 = System.currentTimeMillis();
        LogWrapper.v(TAG, "sendEvent(" + eventName + ",  "
                + appendSessionStateData + ", " + keyValuePairs + ")");

        // if we are on wifi then start separate thread otherwise
        if (OTDataSockets.getNetworkType(appContext).equalsIgnoreCase(
                OTDataSockets.WIFI)) {

            // use this wifi event to upload the file
            if (isFirstWiFiEvent) {
                compressAndUploadDataTask();
                isFirstWiFiEvent = false;
            }

        } else {
            // make sure the next wifi event will trigger as a first wifi event
            isFirstWiFiEvent = true;

        }
        sendTask(eventName, keyValuePairs, appendSessionStateData);

        t0 = System.currentTimeMillis() - t0;
        LogWrapper.v(TAG, t0 + "[ms]");

    }

    /*
     * Method is synchronized with compressAndUploadData through a private lock
     * object. This is to ensure that two threads will not process network/
     * information events.
     */
    private static final void processEvent(String eventName,
            HashMap<String, String> keyValuePairs,
            boolean appendSessionStateData) {

        synchronized (lock/* Get */) {

            LogWrapper.d(TAG, "processEvent(" + eventName + ",  "
                    + appendSessionStateData + ", " + keyValuePairs + ")");

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
            // int eventCount =
            registerSessionEvent();
            // LogWrapper.v(TAG, "eventCount: " + eventCount);

            keyValuePairs.put("si", appName);
            keyValuePairs.put("connection", OTDataSockets
                    .getNetworkType(appContext));
            keyValuePairs.put("platform", OTDataSockets.getPlatform());
            keyValuePairs.put("platform version", OTDataSockets
                    .getPlatformVersion());
            keyValuePairs.put("device", OTDataSockets.getDevice());
            keyValuePairs.put("sh", OTDataSockets.getScreenHeight(appContext));
            keyValuePairs.put("sw", OTDataSockets.getScreenWidth(appContext));
            keyValuePairs.put("app version", OTDataSockets
                    .getAppVersion(appContext));

            String lc =
                    "http://app.opentracker.net/" + appName + "/"
                            + eventName.replace('/', '.');

            keyValuePairs.put("lc", lc);

            // add this location as a previous event. The otpe key is used to
            // keep track of the previous event. This event is needed to measure
            // the amount of time the previous event has taken. This is
            // calculated from getting the current event's timestamp and
            // substracking the previous event's timestamp on the
            // log.opentracker.net server"
            String otpe = "";
            try {
                otpe = otFileUtil.readFile("otpe");
            } catch (IOException e) {
                LogWrapper.i(TAG, "Could not read previous event: " + e);
            }
            if (otpe != null && otpe.length() > 0) {
                keyValuePairs.put("otpe", otpe);
            }

            try {
                otFileUtil.writeFile("otpe", lc);
            } catch (IOException e) {
                LogWrapper.i(TAG, "Could not write previous event: " + e);
            }

            keyValuePairs.put("revision", OTSvnVersion.getRevision());

            // debug
            // String[] userData = null;
            // try {
            // userData = otFileUtil.readFile("otui").split("\\.");
            // } catch (IOException e) {
            // }
            // int lifeTimeEventCount = Integer.parseInt(userData[5]);
            //
            // keyValuePairs
            // .put("ti", eventName + " (" + lifeTimeEventCount + ")");

            String location = OTDataSockets.getCoordinateLatitude(appContext);
            if (location != null) {
                keyValuePairs.put("latitude", location);
                keyValuePairs.put("longitude", OTDataSockets
                        .getCoordinateLongitude(appContext));
                keyValuePairs.put("coordinateAccuracy", OTDataSockets
                        .getCoordinateAccuracy(appContext));
                keyValuePairs.put("coordinateTime", OTDataSockets
                        .getCoordinateTime(appContext));
            }

            String locale = OTDataSockets.getLocale(appContext);
            if (locale != null) {
                keyValuePairs.put("locale", locale);
            }

            String carrier = OTDataSockets.getCarrier(appContext);
            if (carrier != null) {
                keyValuePairs.put("carrier", carrier);
            }

            if (appendSessionStateData) {
                HashMap<String, String> dataFiles = null;
                try {
                    dataFiles = otFileUtil.getSessionStateDataPairs();
                } catch (IOException e) {

                    LogWrapper.w(TAG,
                            "Exception while getting fileName data pairs");

                    HashMap<String, String> logMap =
                            new HashMap<String, String>();
                    logMap.put("si", "errors"); // log to error appName
                    logMap.put("message", getStackTrace(e));
                    OTSend.send(logMap);

                }
                if (dataFiles != null)
                    keyValuePairs.putAll(dataFiles);
            }

            // done: worked out logic of appending data to file
            // LogWrapper.v(TAG, "directSend: " + directSend + ", "
            // + OTDataSockets.getNetworkType(appContext));
            try {
                // use success to determine if information was really sent
                // null if not, otherwise the response string from server
                String success = null;
                if (OTDataSockets.getNetworkType(appContext).equalsIgnoreCase(
                        OTDataSockets.WIFI)) {
                    LogWrapper.i(TAG, "sending: " + keyValuePairs);

                    success = OTSend.send(keyValuePairs);
                } else if (directSend) {
                    LogWrapper.i(TAG, "sending: " + keyValuePairs);

                    success = OTSend.send(keyValuePairs);
                } else {
                    LogWrapper.i(TAG, "appending: " + keyValuePairs);
                    success = "yes";
                    appendDataToFile(keyValuePairs);
                }
                if (success == null) {
                    LogWrapper.w(TAG, "appending to file, network down?");
                    appendDataToFile(keyValuePairs);

                    // TODO try uploading file?
                }

            } catch (Exception e) {
                try {
                    appendDataToFile(keyValuePairs);
                } catch (IOException e1) {
                    LogWrapper.w(TAG, "appending to file, network errors?");
                }
            }
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
        OTLogService.directSend = directSend;
    }

    // public static void uploadData(HashMap<String, String> keyValuePairs)
    // throws IOException {
    // LogWrapper.v(TAG, "uploadData()");
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
        LogWrapper.v(TAG, "OTLogService()");
    }

}
