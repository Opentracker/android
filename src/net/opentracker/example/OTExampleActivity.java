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
package net.opentracker.example;

// note that this example is in the package itself so we do not need to import
// import net.opentracker.android;

import net.opentracker.android.OTLogService;
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

    private static final String TAG = OTExampleActivity.class.getName();

    /**
     * A Context is Android's interface to global information about an
     * application environment. This is an abstract class whose implementation
     * is provided by the Android system. It allows access to
     * application-specific resources and classes, as well as up-calls for
     * application-level operations such as launching activities, broadcasting
     * and receiving intents, etc.
     */
    public Context appContext;

    /**
     * Called in the example android project to simulate a event when a button
     * is pressed.
     */
    public void clickEventOnButton(View v) {
        Log.v(TAG, "clickEventOnButton()");

        // Record an event with the title "button clickEventOnButton", but you
        // can call it anything you want.
        OTLogService.sendEvent("button clickEventOnButton");

    }

    /**
     * Called in the example android project to simulate a event when a check
     * box is pressed.
     */
    public void onClickCheckBox(View v) {
        Log.v(TAG, "clickEventOnButton()");
        // Record an event with the title "button onClickCheckBox", but you
        // can call it anything you want.
        OTLogService.sendEvent("button onClickCheckBox");
    }

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

        // Initiate opentracker's logging service, with the Context and
        // your-registered-app-name
        OTLogService.onCreate(appContext, "your-registered-app-name");

        // to test things real-time always send data directly to logging service
        // make sure to comment this out if you are not testing
        OTLogService.setDirectSend(true);

        // Record an event with the title "Activity started", but you can call
        // it anything you want
        OTLogService.sendEvent("onCreate() called");

        setContentView(R.layout.main);
    }

    /**
     * Called when Android's system is about to start resuming a previous
     * activity, ie the back button is pressed. This is typically used to commit
     * unsaved changes to persistent data, stop animations and other things that
     * may be consuming CPU, etc. Implementations of this method must be very
     * quick because the next activity will not be resumed until this method
     * returns.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause()");
        OTLogService.sendEvent("onPause() called");
        // Close the session and upload the events. The onPause method is
        // guaranteed to be called in the life cycle of an Android App.
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
        OTLogService.sendEvent("onResume() called");
    }

}