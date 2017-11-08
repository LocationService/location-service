package com.c137.location;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import com.android.volley.toolbox.*;
import com.android.volley.*;

import java.util.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Formatter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;


public class LocationService extends Service {
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final String HMAC_KEY = "ea86ec783c52d9e26607d11a1247485a";

//    private static final String API_URL = "http://137.74.197.251:8083/";
    private static final String API_URL = "http://192.168.31.216:8083/";
    private static final String API_KEY = "f5ee5dee5f9ded00a624ff4bf34eb3d3";

    final String LOG_TAG = "Location";
    private LocationManager locationManager;
    private TelephonyManager telephonyManager;
    private String imei;

    private LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            sendLocation(location);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(LOG_TAG, provider + " status changed " + String.valueOf(status));
        }

        public void onProviderEnabled(String provider) {
            Log.d(LOG_TAG, provider + " enabled");
            Location location = locationManager.getLastKnownLocation(provider);
            sendLocation(location);
        }

        public void onProviderDisabled(String provider) {
            Log.d(LOG_TAG, provider + " disabled");
        }
    };

    private static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();

        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return formatter.toString();
    }

    public static String makeSign(String data) throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec signingKey = new SecretKeySpec(HMAC_KEY.getBytes(), HMAC_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(signingKey);
        return toHexString(mac.doFinal(data.getBytes()));
    }

    public static String signData(String data) throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        String sign = makeSign(data);
        return sign + "." + data;
    }

    public void onCreate() {
        Log.d(LOG_TAG, "start service");
        telephonyManager = this.getBaseContext().getSystemService(TelephonyManager.class);
        imei = telephonyManager.getImei();
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000 * 60, 100, locationListener);
        // locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000 * 60, 100, locationListener);
        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        Log.d(LOG_TAG, "stop service");
        locationManager.removeUpdates(locationListener);
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendLocation(Location location) {
        if (location == null)
            return;
        final String provider = location.getProvider();
        final Double lat = location.getLatitude();
        final Double lng = location.getLongitude();

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, API_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(LOG_TAG, response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(LOG_TAG, error.toString());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                JSONObject jsonObject = new JSONObject();

                try {
                    jsonObject.put("imei", imei);
                    jsonObject.put("provider", provider);
                    jsonObject.put("lat", lat);
                    jsonObject.put("lng", lng);
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.toString());
                }

                try {
                    String json = jsonObject.toString();
                    String signedBody = signData(json);
                    params.put("signed_body", signedBody);
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.toString());
                }

                Log.d(LOG_TAG, params.toString());
                return params;
            }
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + API_KEY);
                return headers;
            }
        };
        queue.add(stringRequest);
    }
}
