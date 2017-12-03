package com.c137.location;

import android.content.Context;
import android.location.Location;
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

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String API_URL = " https://api.c137-location.space/";

    private final String LOG_TAG = "Location Request";
    private String jwt;

    public LocationClient(Context context) {
        this.context = context;
        this.queue = Volley.newRequestQueue(context);
    }

    public void register(String deviceID, String manufacturer, String model, String androidRelease) {
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonAttributes = new JSONObject();
        try {
            jsonAttributes.put("device_id", deviceID);
            jsonAttributes.put("manufacturer", manufacturer);
            jsonAttributes.put("model", model);
            jsonAttributes.put("android_release", androidRelease);

            jsonObject.put("reg", jsonAttributes);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.toString());
        }
        makePost(Request.Method.POST, "devices/auth", jsonObject, false, new VolleyCallback(){
            @Override
            public void onSuccess(String result){
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    jwt = jsonObject.get("jwt").toString();
                } catch (JSONException e) {
                    Log.e(LOG_TAG, e.toString());
                }
            }
        });
    }

    public void makeLocation(Location location) {
        if (location == null) {
            return;
        }

        final String provider = location.getProvider();
        final Double lat = location.getLatitude();
        final Double lng = location.getLongitude();

        JSONObject jsonObject = new JSONObject();
        JSONObject jsonAttributes = new JSONObject();
        try {
            jsonAttributes.put("provider", provider);
            jsonAttributes.put("lat", lat);
            jsonAttributes.put("lng", lng);
            jsonObject.put("location", jsonAttributes);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.toString());
        }
        makePost(Request.Method.POST, "devices/locations", jsonObject, true, new VolleyCallback(){
            @Override
            public void onSuccess(String result){}
        });
    }

    private void makePost(int method, String path, final JSONObject jsonObject, final boolean auth, final VolleyCallback callback) {
        String url = API_URL + path;
        StringRequest stringRequest = new StringRequest(method, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        callback.onSuccess(response);
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
                if (auth) {
                    headers.put("Authorization", "Bearer " + jwt);
                }
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    private String signData(String data) throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        String sign = makeSign(data);
        return sign + "." + data;
    }

    private String makeSign(String data) throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        String hmacKey = context.getResources().getString(R.string.location_hmac_key);
        SecretKeySpec signingKey = new SecretKeySpec(hmacKey.getBytes(), HMAC_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(signingKey);
        return toHexString(mac.doFinal(data.getBytes()));
    }

    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();

        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return formatter.toString();
    }
}
