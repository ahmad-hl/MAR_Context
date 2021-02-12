package com.spacecontext.services;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import android.widget.Toast;
import android.util.Log;

import com.spacecontext.MainActivity;
import com.spacecontext.R;
import com.spacecontext.providers.Location_Provider.Location_Data;

public class LocationLogger extends Service implements LocationListener {
    public static String TAG = "VSpaceContext::LocationLogger";

    private static LocationManager locationManager = null;
    private final static int REQUEST_CHECK_GOOGLE_SETTINGS = 0x99;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Toast.makeText(this, "Location Service has started..",Toast.LENGTH_LONG).show();

        String input = intent.getStringExtra("inputExtra");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, NotificationChannelApp.CHANNEL_ID)
                .setContentTitle("Example Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_android)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        return START_NOT_STICKY;
    }

    /**
     * Checks whether two providers are the same
     */
    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    /**
     * Determines whether one Location reading is better than the current Location fix
     *
     * @param newLocation  The new Location that you want to evaluate
     * @param lastLocation The last location fix, to which you want to compare the new one
     */
    private boolean isBetterLocation(Location newLocation, Location lastLocation) {
        if (newLocation == null && lastLocation == null) return false;
        if (newLocation != null && lastLocation == null) return true;
        if (newLocation == null) return false;

        long timeDelta = newLocation.getTime() - lastLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > 1000 * 300;
        boolean isSignificantlyOlder = timeDelta < -1000 * 300;
        boolean isNewer = timeDelta > 0;
        isNewer = true;
//        if (isSignificantlyNewer) {
//            return true;
//        } else if (isSignificantlyOlder) {
//            return false;
//        }

        int accuracyDelta = (int) (newLocation.getAccuracy() - lastLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;
        boolean isFromSameProvider = isSameProvider(newLocation.getProvider(), lastLocation.getProvider());

        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     *
     */
    private void askForLocationDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Allow access your location ?");
        builder.setMessage("If you want to disable location permission for \\\"Compass\\\", access to your application settings and disable it.");
        builder.setPositiveButton(
                "Allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(null, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CHECK_GOOGLE_SETTINGS);
                    }
                }
        );
        builder.setNegativeButton(
                "Don\\'t allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }
        );
        builder.show();
    }

    @Override
    public void onLocationChanged(Location newLocation) {

        Location bestLocation = null;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            askForLocationDialog();
        }
        Location lastGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (isBetterLocation(lastNetwork, lastGPS)) {
            if (isBetterLocation(newLocation, lastNetwork)) {
                bestLocation = newLocation;
            } else {
                bestLocation = lastNetwork;
            }
        } else {
            if (isBetterLocation(newLocation, lastGPS)) {
                bestLocation = newLocation;
            } else {
                bestLocation = lastGPS;
            }
        }

        Log.d("mySensor", "Location is " + bestLocation);

        Uri locUri  = saveLocation(bestLocation);
        Toast.makeText(this, locUri.toString(),Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Save a location, handling geofencing.
     *
     * @param bestLocation Location to save
     */
    public Uri saveLocation(Location bestLocation) {
        Uri idURI = null;
        if (bestLocation == null) return idURI; //no location available

        ContentValues rowData = new ContentValues();
        rowData.put(Location_Data.TIMESTAMP, System.currentTimeMillis());
//        rowData.put(Location_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
        rowData.put(Location_Data.PROVIDER, bestLocation.getProvider());
        rowData.put(Location_Data.LATITUDE, bestLocation.getLatitude());
        rowData.put(Location_Data.LONGITUDE, bestLocation.getLongitude());
        rowData.put(Location_Data.BEARING, bestLocation.getBearing());
        rowData.put(Location_Data.SPEED, bestLocation.getSpeed());
        rowData.put(Location_Data.ALTITUDE, bestLocation.getAltitude());
        rowData.put(Location_Data.ACCURACY, bestLocation.getAccuracy());
        try {
            idURI = getContentResolver().insert(Location_Data.CONTENT_URI, rowData);
            Log.d("mySensor", "Location is saved to " + idURI.toString());
//            if (awareSensor != null) awareSensor.onLocationChanged(rowData);

        } catch (SQLiteException e) {
//            if (Aware.DEBUG) Log.d(TAG, e.getMessage());
            Toast.makeText(this, "Exeption.........",Toast.LENGTH_LONG).show();
        } catch (SQLException e) {
//            if (Aware.DEBUG) Log.d(TAG, e.getMessage());
            Toast.makeText(this, "Exeption.........",Toast.LENGTH_LONG).show();
        }
        return idURI;
    }
}
