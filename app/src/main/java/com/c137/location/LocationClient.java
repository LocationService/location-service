package com.c137.location;

import android.content.Context;
import android.location.Location;
import android.provider.Settings.Secure;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class LocationClient {
    private Context context;
    private RequestQueue queue;

    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final String HMAC_KEY = "ea86ec783c52d9e26607d11a1247485a";

    private static final String API_URL = "http://192.168.31.216:8083/";
    private static final String API_KEY = "f5ee5dee5f9ded00a624ff4bf34eb3d3";

    private final String LOG_TAG = "Location Request";

    public LocationClient(Context context) {
        this.context = context;
        this.queue = Volley.newRequestQueue(context);
    }

    public void makeLocation(Location location) {
        if (location == null) {
            return;
        }

        final String provider = location.getProvider();
        final Double lat = location.getLatitude();
        final Double lng = location.getLongitude();
        String androidID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("device_id", androidID);
            jsonObject.put("provider", provider);
            jsonObject.put("lat", lat);
            jsonObject.put("lng", lng);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.toString());
        }
        makePost(Request.Method.POST, "receiver/location", jsonObject);
    }

    private void makePost(int method, String path, final JSONObject jsonObject) {
        String url = API_URL + path;
        StringRequest stringRequest = new StringRequest(method, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();

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

    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();

        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return formatter.toString();
    }

    private String makeSign(String data) throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec signingKey = new SecretKeySpec(HMAC_KEY.getBytes(), HMAC_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(signingKey);
        return toHexString(mac.doFinal(data.getBytes()));
    }

    private String signData(String data) throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        String sign = makeSign(data);
        return sign + "." + data;
    }
}
