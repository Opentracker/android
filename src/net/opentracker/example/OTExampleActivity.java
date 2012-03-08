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

import java.util.HashMap;

//note that this example needs to import net.opentracker.android.OTLogService;
import net.opentracker.android.OTLogService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

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
 * 
 * Note we implement SeekBar.OnSeekBarChangeListener to work with seek bar
 * events.
 * 
 * @author $Author: eddie $ (latest svn author)
 * @version $Id: OTExampleActivity.java 14170 2012-03-08 15:25:02Z eddie $
 */
public class OTExampleActivity extends Activity {

    private static final String TAG = OTExampleActivity.class.getName();

    private static final String DEMO_URL =
            "http://preview.opentracker.net/en/other/login.jsp";

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
    public void clickExampleButton(View v) {

        HashMap<String, String> values = new HashMap<String, String>();

        EditText mEdit = (EditText) findViewById(R.id.exampleEditText);

        values.put("exampleEditText", mEdit.getText().toString());

        // send a custom url to be rendered in the opentracker user interface
        values.put("url", "http://yahoo.com");

        Log.v(TAG, "clickExampleButton(): " + values);

        // Record an event with the title "button clickExampleButton", you
        // can call it anything you want, and attributes defined in hashmap
        // values
        OTLogService.sendEvent("button clickExampleButton", values);

    }

    /**
     * Called in the example android project to simulate a event when a check
     * box is pressed.
     */
    public void clickExampleCheckBox(View v) {
        Log.v(TAG, "clickExampleCheckBox()");
        // Record an event with the title "button onClickCheckBox", but you
        // can call it anything you want.
        OTLogService.sendEvent("button clickExampleCheckBox");
    }

    /**
     * A callback that notifies clients when the progress level has been
     * changed. This includes changes that were initiated by the user through a
     * touch gesture or arrow key/trackball as well as changes that were
     * initiated programmatically.
     * 
     * Implemented the exampleSeekBar as a private variable, so we don't need to
     * worry about having this example activity object implement SeekBar
     * 
     * http://android-er.blogspot.com
     * /2009/08/change-background-color-by-seekbar.html cf.
     * 
     * http://www.android10.org
     * /index.php/forums/43-view-layout-a-resource/959-example
     * -seekbar-to-control-the-volume-of-video-player
     * 
     * http://developer.android.com/reference/android/widget/SeekBar.
     * OnSeekBarChangeListener.html
     */
    private SeekBar.OnSeekBarChangeListener exampleSeekBarChangeListener =
            new SeekBar.OnSeekBarChangeListener() {

                int lastSeekBarValue = 0;

                boolean lastFromUser = false;

                /**
                 * Notice that we do not log anything in the onProgressChange()
                 * Doing so would sent too many events to the logging server,
                 * which should be avoided.
                 * 
                 * (non-Javadoc)
                 * 
                 * @see android.widget.SeekBar.OnSeekBarChangeListener#onProgressChanged(android.widget.SeekBar,
                 *      int, boolean)
                 */
                public void onProgressChanged(SeekBar seekBar, int progress,
                        boolean fromUser) {
                    TextView mProgressText =
                            (TextView) findViewById(R.id.exampleSeekBarValue);

                    mProgressText.setText("Slider value: " + progress);
                    lastSeekBarValue = progress;
                    lastFromUser = fromUser;
                }

                public void onStartTrackingTouch(SeekBar seekBar) {
                    // dont do anything wrt logging, no point
                }

                public void onStopTrackingTouch(SeekBar seekBar) {
                    // log the value (only when things have stopped)
                    HashMap<String, String> values =
                            new HashMap<String, String>();

                    values.put("exampleSeekBarValue", "" + lastSeekBarValue);

                    // adding a second value just for fun
                    values.put("exampleSeekBarFromUser", "" + lastFromUser);
                    Log.v(TAG, "clickExampleButton(): " + values);
                    OTLogService.sendEvent("onStopTrackingTouch() called",
                            values);

                }
            };

    /**
     * Called in the example android project to simulate a event when a check
     * box is pressed, and open a new browser window.
     */
    public void clickExampleViewBehaviorButton(View v) {
        Log.v(TAG, "clickExampleViewBehaviorButton()");

        OTLogService.sendEvent("button clickExampleViewBehaviorButton");

        // http://stackoverflow.com/questions/2201917/how-can-i-open-a-url-in-androids-web-browser-from-my-application
        Intent browserIntent =
                new Intent(Intent.ACTION_VIEW, Uri.parse(DEMO_URL));
        startActivity(browserIntent);

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

        // uncomment to reset session/ user data for opentracker.
        // OTLogService.reset();

        // to test things real-time always send data directly to logging service
        // make sure to comment this out if you are not testing
        // OTLogService.setDirectSend(true);

        // Record an event with the title "onCreate() called", but you can call
        // it anything you want
        OTLogService.sendEvent("onCreate() called");

        setContentView(R.layout.main);

        // register this object to capture exampleSeekBar events
        ((SeekBar) findViewById(R.id.exampleSeekBar))
                .setOnSeekBarChangeListener(exampleSeekBarChangeListener);

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
        // uploads the file containing the logged events. The onPause method is
        // guaranteed to be called in the life cycle of an Android App, so we
        // are guaranteed the events log file are uploaded
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
        // No real need to log this event, but you can if you need to, want to
        // Log.v(TAG, "onResume()");
        // OTLogService.sendEvent("onResume() called");
    }

}