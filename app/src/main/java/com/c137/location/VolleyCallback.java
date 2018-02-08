package com.c137.location;

import com.android.volley.VolleyError;

public interface VolleyCallback{
    void onSuccess(String result);
    void onError(VolleyError error);
}
