package com.c137.location;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

public class LocationService extends Service {
    final String LOG_TAG = "Location";

    private LocationManager locationManager;
    private LocationClient locationClient;

    private LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            locationClient.makeLocation(location);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(LOG_TAG, provider + " status changed " + String.valueOf(status));
        }

        public void onProviderEnabled(String provider) {
            Log.d(LOG_TAG, provider + " enabled");
            Location location = locationManager.getLastKnownLocation(provider);
            locationClient.makeLocation(location);
        }

        public void onProviderDisabled(String provider) {
            Log.d(LOG_TAG, provider + " disabled");
        }
    };

    public void onCreate() {
        locationClient = new LocationClient(
                this,
                Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID),
                Build.MANUFACTURER,
                Build.MODEL,
                Build.VERSION.RELEASE
        );
        locationClient.register();

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000 * 60, 100, locationListener);
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000 * 60, 100, locationListener);
        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        locationManager.removeUpdates(locationListener);
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        return null;
    }
}