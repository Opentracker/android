package net.opentracker.android.test;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

// read: http://blog.doityourselfandroid.com/2010/12/25/understanding-locationlistener-android/
// on conserving battery, and timing constraints.
// read: http://stackoverflow.com/questions/2021176/how-can-i-check-the-current-status-of-the-gps-receiver
// on the status icon
// read: http://stackoverflow.com/questions/3145089/what-is-the-simplest-and-most-robust-way-to-get-the-users-current-location-in-a/3145655#3145655
// and used this code a main body of location services
// confirmed that is is best code by looking at
// http://stackoverflow.com/questions/1513485/how-do-i-get-the-current-gps-location-programmatically-in-android
// which has less control
public class OTLocationService {

    public abstract class LocationResult {
        public abstract Location getGPSLocation();

        public abstract Location getNetworkLocation();

        public abstract void setGPSLocation(Location location);

        public abstract void setNetworkLocation(Location location);

    }

    // public static String getLocation(final Context appContext) {
    // if (locationService == null) {
    // locationService = new OTLocationService(appContext);
    // } else {
    // locationService.runLocationService(appContext);
    // }
    // long t0 = System.currentTimeMillis();
    // try {
    // if (locationService.locationResult.getGPSLocation() != null)
    // Log.e(TAG, "getGpsCoordinates: "
    // + locationService.locationResult.getGPSLocation());
    // if (locationService.locationResult.getGPSLocation() != null)
    // Log.e(TAG, "getNetworkCoordinates: "
    // + locationService.locationResult.getNetworkLocation());
    // if (locationService.locationResult.getGPSLocation() != null)
    // Log.e(TAG, "getGpsCoordinates: "
    // + locationService.locationResult.getGPSLocation()
    // .getLatitude());
    // if (locationService.locationResult.getGPSLocation() != null)
    // Log.e(TAG, "getNetworkCoordinates: "
    // + locationService.locationResult.getNetworkLocation()
    // .getLatitude());
    // if (locationService.locationResult.getNetworkLocation() != null)
    // Log.e(TAG, "getNetworkCoordinates: "
    // + new Date(locationService.locationResult
    // .getNetworkLocation().getTime()));
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    //
    // t0 = System.currentTimeMillis() - t0;
    // Log.e(TAG, t0 + "[ms]");
    // return null;
    //
    // }
    // public static String getResolution() {
    // // display mangaer Obj gets the resolution of the screen
    // //
    // http://stackoverflow.com/questions/1016896/android-how-to-get-screen-dimensions
    // DisplayMetrics dm = new DisplayMetrics();
    // getWindowManager().getDefaultDisplay().getMetrics(dm);
    // return dm.widthPixels + " x " + dm.heightPixels;
    // }

    // TODO: return wifi, mobile or airplane.
    // public static String getNetwork(final Context appContext) {
    // Log.v(TAG, "getNetwork()");
    // try {
    // // TelephonyManager teleMan =
    // // (TelephonyManager) appContext
    // // .getSystemService(Context.TELEPHONY_SERVICE);
    // // int networkType = teleMan.getNetworkType();
    // ConnectivityManager connectivityManager =
    // (ConnectivityManager) appContext
    // .getSystemService(Context.CONNECTIVITY_SERVICE);
    // NetworkInfo activeNetworkInfo =
    // connectivityManager.getActiveNetworkInfo();
    // if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
    // return "wifi";
    // }
    // // switch (networkType) {
    // // case TelephonyManager.NETWORK_TYPE_CDMA:
    // // return "cdma";
    // // case TelephonyManager.NETWORK_TYPE_EDGE:
    // // return "edge";
    // // case TelephonyManager.NETWORK_TYPE_UMTS:
    // // return "umts";
    // // case TelephonyManager.NETWORK_TYPE_HSDPA:
    // // return "hsdpa";
    // // default:
    // // return "unknown";
    // // }
    // } catch (SecurityException ise) {
    // Log.w(TAG, ise);
    // }
    // return "no network";
    // }
    private class UpdateGPSLocation extends TimerTask {
        @Override
        public void run() {
            locationManager.removeUpdates(locationListenerGps);

            Log.e(TAG, "request gps update...");

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0 * 5 * 60 * 1000, 0 * 100,
                    locationListenerGps);
            Log.e(TAG, "done with gps update request...");

        }
    }

    private class UpdateNetworkLocation extends TimerTask {
        @Override
        public void run() {
            locationManager.removeUpdates(locationListenerNetwork);

            Log.e(TAG, "request network update...");

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 0 * 5 * 60 * 1000,
                    0 * 100, locationListenerNetwork);
            Log.e(TAG, "done with network update request...");

        }
    }

    // private static long delayGpsMs = 20 * 10000; // 20 x seconds * 1000ms
    //
    // private static long delayNetworkMs = 5 * 1000; // 20 x seconds * 1000ms

    /*
     * A facility for threads to schedule tasks for future execution in a
     * background thread. Tasks may be scheduled for one-time execution, or for
     * repeated execution at regular intervals.
     */
    private static Timer gpsTimer;

    private static boolean isGpsEnabled = false;

    private static boolean isMoved = true;

    private static boolean isNetworkEnabled = false;

    private static long lastRun = -1;

    private static LocationManager locationManager;

    // private static long MIN_DISTANCE = 100; // 100 m

    /*
     * Every 5 * 60 seconds our GPS kicks in and tries to retrieve the location.
     * It does so by providing the location listener with multiple location
     * updates that we can use to track the users location, before going idle.
     * 
     * When the GPS is trying to pinpoint your location, the GPS icon on the
     * phone will blink, you know that it’s consuming your battery power.
     * 
     * Notice how during these windows where its pinpointing a location, and
     * location updates are sent to the listeners, different accuracies are
     * provided.
     */
    private static long minTimeMs = 1 * 60 * 1000; // minutes x seconds * 1000ms

    private static Timer networkTimer;

    private static final String TAG = "OTLocationService";// .class.getName();

    private LocationListener locationListenerGps = new LocationListener() {

        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged called for gps " + location);

            if (location == null
                    || (location.getLatitude() == 0.0 && location
                            .getLongitude() == 0.0)) {
                // Hack to filter out 0.0,0.0 locations
                return;
            }
            location.setTime(System.currentTimeMillis());
            locationResult.setGPSLocation(location);
            gpsTimer.cancel();

            // locationManager.removeUpdates(this);
        }

        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled called");

        }

        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled called");

        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged called");

        }

    };

    private LocationListener locationListenerNetwork = new LocationListener() {

        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged called for network " + location);

            if (location == null
                    || (location.getLatitude() == 0.0 && location
                            .getLongitude() == 0.0)) {
                // Hack to filter out 0.0,0.0 locations
                return;
            }
            location.setTime(System.currentTimeMillis());
            locationResult.setNetworkLocation(location);
            // locationManager.removeUpdates(this);
            networkTimer.cancel();

        }

        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled called");

        }

        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled called");

        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged called");

        }

    };

    public final LocationResult locationResult = new LocationResult() {

        private Location gpsLocation;

        private Location networkLocation;

        @Override
        public Location getGPSLocation() {

            if (gpsLocation != null)
                return gpsLocation;

            if (locationManager == null)
                return null;

            // try to get the latest
            Location tmpLocation =
                    locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (tmpLocation != null)
                return tmpLocation;
            else
                return null;

        }

        @Override
        public Location getNetworkLocation() {
            if (networkLocation != null)
                return networkLocation;

            if (locationManager == null)
                return null;

            // try to get the latest
            Location tmpLocation =
                    locationManager
                            .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (tmpLocation != null)
                return tmpLocation;
            else
                return null;

        }

        public void setGPSLocation(Location location) {
            // Got the location!
            this.gpsLocation = location;
        }

        public void setNetworkLocation(Location location) {
            // Got the location!
            this.networkLocation = location;
        }

    };

    public OTLocationService(Context appContext) {
        Log.e(TAG, "constructor called");
        runLocationService(appContext);
    }

    /*
     * When called the GPS will try to pinpoint the location. The GPS icon on
     * the phone will blink and the GPS system will consume battery power.
     * 
     * The GPS location will be provided by satellites and will take some time,
     * the time needed is a function of the hardware and the system's position
     * on our planet. The variable delayGpsMs is the number of milliseconds we
     * wait before updating the last know location with GPS data. This should be
     * enough time for the GPS system to do its work.
     * 
     * This class has a safety valve defaulted to a minTimeMs of five minutes.
     * Ie you can not run two GPS pinpointing services separated by less that
     * minTimeMs milliseconds;
     */
    public void runLocationService(Context context) {

        Log.e(TAG, "runLocationService called, appContext: " + context);

        // Use LocationResult as a callback class to pass location value from
        // getLocation to user code.

        Log.e(TAG, "retreaving last run: "
                + lastRun
                + ", "
                + (lastRun != -1 ? (Math
                        .round((System.currentTimeMillis() - lastRun)
                                / (100 * 60)) / 10) : "unknown") + " [min]");

        // lastRun - now
        if (System.currentTimeMillis() - lastRun < minTimeMs) {
            return;
        }
        if (!isMoved)
            return;

        Log.e(TAG, "initializing timestamps and managers...");

        lastRun = System.currentTimeMillis();

        if (locationManager == null) {
            locationManager =
                    (LocationManager) context
                            .getSystemService(Context.LOCATION_SERVICE);
        }

        Log.e(TAG, "retreaving providers...");

        // exceptions will be thrown if provider is not permitted.
        try {
            isGpsEnabled =
                    locationManager
                            .isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            // e.printStackTrace();
        }
        try {
            isNetworkEnabled =
                    locationManager
                            .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            // e.printStackTrace();
        }

        // don't start listeners if no provider is enabled
        if (!isGpsEnabled && !isNetworkEnabled)
            return;

        Log.e(TAG, "scheduling pinpointing gps: " + isGpsEnabled
                + ", network: " + isNetworkEnabled);

        if (isNetworkEnabled) {

            Log.e(TAG, "added network update location request...");

            networkTimer = new Timer();
            networkTimer.schedule(new UpdateNetworkLocation(), 10);// delayGpsMs

            Log.e(TAG, "scheduling a timer to pick up results...");

        }
        if (isGpsEnabled) {
            Log.e(TAG, "request gps update...");

            Log.e(TAG, "added gps update location request...");

            gpsTimer = new Timer();
            gpsTimer.schedule(new UpdateGPSLocation(), 10);// delayNetworkMs

            Log.e(TAG, "scheduling a timer to pick up results...");

        }
    }

    /*
     * GPS systems take some time to pinpoint location data. The variable
     * delayGpsMs is the number of milliseconds we wait before updating the last
     * known location with GPS data. This should be enough time for the GPS
     * system to do its work.
     */
    public void setDelayGps(long minTimeMs) {
        // this.minTimeMs = minTimeMs;
    }

    /*
     * Sets the safety valve minTimeMs in milliseconds, ie to conserve battery
     * power you can not run two GPS pinpointing services separated by less that
     * minTimeMs milliseconds.
     */
    public void setMinTimeMs(long minTimeMs) {
        // this.minTimeMs = minTimeMs;
    }
}