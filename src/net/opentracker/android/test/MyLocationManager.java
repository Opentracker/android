package net.opentracker.android.test;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Config;
import android.util.Log;

public class MyLocationManager {

    private static final String TAG = "MyLocationManager ";

    private Context mContext = null;

    private LocationManager mLocationManager = null;

    public MyLocationManager(Context context) {
        this.mContext = context;

        if (this.mContext != null) {
            this.mLocationManager =
                    (LocationManager) this.mContext
                            .getSystemService(Context.LOCATION_SERVICE);
        }
    }

    LocationListener[] mLocationListeners =
            new LocationListener[] {
                    new LocationListener(LocationManager.GPS_PROVIDER),
                    new LocationListener(LocationManager.NETWORK_PROVIDER) };

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        boolean mValid = false;

        String mProvider;

        public LocationListener(String provider) {
            mProvider = provider;
            mLastLocation = new Location(mProvider);
        }

        public void onLocationChanged(Location newLocation) {
            if (newLocation.getLatitude() == 0.0
                    && newLocation.getLongitude() == 0.0) {
                // Hack to filter out 0.0,0.0 locations
                return;
            }
            if (newLocation != null) {
                // /if(newLocation.getTime() == 0)
                // newLocation.setTime(System.currentTimeMillis());
                newLocation.setTime(System.currentTimeMillis());

                if (Config.DEBUG) {
                    Log.i(TAG, "onLocationChanged in loc mgnr");
                }
            }
            mLastLocation.set(newLocation);
            mValid = true;
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
            mValid = false;
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (status == LocationProvider.OUT_OF_SERVICE) {
                mValid = false;
            }
        }

        public Location current() {
            return mValid ? mLastLocation : null;
        }
    };

    public void startLocationReceiving() {
        if (this.mLocationManager != null) {
            try {
                this.mLocationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 1000, 0F,
                        this.mLocationListeners[1]);
            } catch (java.lang.SecurityException ex) {
                if (Config.DEBUG) {
                    Log.e(TAG, "SecurityException " + ex.getMessage());
                }
            } catch (IllegalArgumentException ex) {
                // Log.e(TAG, "provider does not exist " + ex.getMessage());
            }
            try {
                this.mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 1000, 0F,
                        this.mLocationListeners[0]);
            } catch (java.lang.SecurityException ex) {
                if (Config.DEBUG) {
                    Log.e(TAG, "SecurityException " + ex.getMessage());
                }
            } catch (IllegalArgumentException ex) {
                // Log.e(TAG, "provider does not exist " + ex.getMessage());
            }
        }
    }

    public void stopLocationReceiving() {
        if (this.mLocationManager != null) {
            for (int i = 0; i < this.mLocationListeners.length; i++) {
                try {
                    this.mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    // ok
                }
            }
        }
    }

    public Location getCurrentLocation() {
        Location l = null;

        // go in best to worst order
        for (int i = 0; i < this.mLocationListeners.length; i++) {
            l = this.mLocationListeners[i].current();
            if (l != null)
                break;
        }

        return l;
    }
}