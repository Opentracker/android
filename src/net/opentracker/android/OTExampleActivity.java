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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

/**
 * 
 * This class is an example of how to extend an activity that will use
 * Opentracker's logging/ analytics engines.
 * 
 * Android's activity class is focused on things that the user can do. Almost
 * all activities interact with the user, so the Activity class takes care of
 * creating a window for you in which you can place your UI.
 * 
 * You can use Opentracker's logging services to log the users/ or systems
 * activity.
 * 
 * Declare Internet permissions in your apps manifest by adding the following
 * line to AndroidManifest.xml. This allows your application to use any Internet
 * connections and for the application to know what network type is being used
 * (Wireless/ mobile)
 * 
 * <uses-permission android:name="android.permission.INTERNET" />
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 * 
 * Note if you see: DEBUG/SntpClient(69): request time failed:
 * java.net.SocketException: Address family not supported by protocol, then this
 * is android that failed to contact the internet time server, and is normal.
 */
public class OTExampleActivity extends Activity {

    /**
     * A Context is Android's interface to global information about an
     * application environment. This is an abstract class whose implementation
     * is provided by the Android system. It allows access to
     * application-specific resources and classes, as well as up-calls for
     * application-level operations such as launching activities, broadcasting
     * and receiving intents, etc.
     */
    public Context appContext;

    private static final String TAG = OTExampleActivity.class.getName();

    /**
     * Called when Android's activity is starting. This is where most
     * initialization should go.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "onCreate()");

        // Returns the context of the Activity to access its resources
        appContext = this.getApplicationContext();

        // initiate opentracker's logging service, with the context and
        // application's name
        OTLogService.onCreate(appContext, "test-app-name");

        // record an event with the title "Activity started"
        OTLogService.sendEvent("Activity started");
        setContentView(R.layout.main);
    }

    /**
     * Called when Android's system is about to start resuming a previous
     * activity. This is typically used to commit unsaved changes to persistent
     * data, stop animations and other things that may be consuming CPU, etc.
     * Implementations of this method must be very quick because the next
     * activity will not be resumed until this method returns.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause()");
        OTLogService.onPause();
    }

    /**
     * Called when Android's activity will start interacting with the user. At
     * this point your activity is at the top of the activity stack, with user
     * input going to it. Always followed by onPause().
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()");
        OTLogService.sendEvent("Session resuming");
    }

    public void clickEventOnButton(View v) {
        Log.v(TAG, "clickEventOnButton()");
        // RadioButton button1 = (RadioButton) findViewById(R.id.button1);
        OTLogService.sendEvent("button clickEventOnButton");
        Log.v(TAG, OTDataSockets.getAppVersion(appContext));
        Log.v(TAG, OTDataSockets.getIpAddress());
        Log.v(TAG, OTDataSockets.getScreenHeight(appContext));
        Log.v(TAG, OTDataSockets.getScreenWidth(appContext));
        Log.v(TAG, OTDataSockets.getUserAgent(appContext));
        Log.v(TAG, OTDataSockets.getNetworkType(appContext));
        Log.v(TAG, OTDataSockets.getNetwork(appContext));
        Log.v(TAG, OTDataSockets.getWifiInfo(appContext));
    }

    public void onClickCheckBox(View v) {
        Log.v(TAG, "clickEventOnButton()");
        // RadioButton button1 = (RadioButton) findViewById(R.id.button1);
        OTLogService.sendEvent("button onClickCheckBox");
    }

}