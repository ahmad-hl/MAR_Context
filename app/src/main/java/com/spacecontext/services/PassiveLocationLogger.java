package com.spacecontext.services;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.spacecontext.MainActivity;
import com.spacecontext.R;
import com.spacecontext.providers.Location_Provider;

public class PassiveLocationLogger extends Service implements LocationListener{
    private static String TAG = "SpaceContext:PassiveLocationLogger";
    private final static int REQUEST_CHECK_GOOGLE_SETTINGS = 0x99;

    private boolean isGPSEnable = false;
    private boolean isNetworkEnable = false;
    private LocationManager locationManager;
    private Location location;

    // This is the object that receives interactions from clients.
    private final IBinder mBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public PassiveLocationLogger getService() {
            return PassiveLocationLogger.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Toast.makeText(this, "Passive Location Service has started..",Toast.LENGTH_LONG).show();
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

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    /**
     * Ask user for permission Allow/Don't Allow
     */
    private void askForLocationDialog(final Activity mainActivity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Allow access your location ?");
        builder.setMessage("If you want to disable location permission for 'Compass', access to your application settings and disable it.");
        builder.setPositiveButton(
                "Allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CHECK_GOOGLE_SETTINGS);
                    }
                }
        );
        builder.setNegativeButton(
                "Don't allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }
        );
        builder.show();
    }

    public void getLatestLocation(Activity mainActivity) {
        //Check if pemission granted and return otherwise
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            askForLocationDialog(mainActivity);
            return;
        }

        isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (isNetworkEnable) {
            location = null;
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
            if (locationManager != null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    Log.d("Loc", "Lat:"+location.getLongitude() + ",Lon:"+location.getLatitude());

                    //Insert location Data into SQLite DB
                    saveLocation(this, location);
                }
            }
        }

        if (isGPSEnable) {
            location = null;
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            if (locationManager != null) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    Log.d("Loc", "Lat:"+location.getLongitude() + ",Lon:"+location.getLatitude());

                    //Insert location Data into SQLite DB
                    saveLocation(this, location);
                }
            }
        }
    }

    private void saveLocation(Context mContext, Location location){
        //Insert location Data into SQLite DB
        ContentValues values = new ContentValues();
        values.put(Location_Provider.Location_Data.LATITUDE, location.getLatitude());
        values.put(Location_Provider.Location_Data.LONGITUDE, location.getLongitude());
        values.put(Location_Provider.Location_Data.ALTITUDE, location.getAltitude());

        // Insert the new row, returning the primary key value of the new row
        Uri locDataUri = mContext.getContentResolver().insert(Location_Provider.Location_Data.CONTENT_URI, values);
        Log.d(TAG, "Passive Location is saved  to " + locDataUri.toString());
    }

}