package com.spacecontext.services;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.spacecontext.MainActivity;
import com.spacecontext.R;
import com.spacecontext.util.Constants;

public class PassiveLocationLogger extends Service implements LocationListener{
    private final static String TAG = "SpaceContext:PassiveLocationLogger";
    private final static int REQUEST_CHECK_GOOGLE_SETTINGS = 0x99;

    private boolean isGPSEnable = false;
    private boolean isNetworkEnable = false;
    private LocationManager locationManager;
    private Location location;
    private Context mContext;

    public PassiveLocationLogger(Context mContext){
        this.mContext = mContext;
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
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
    public void onStatusChanged(String provider, int status, Bundle extras) {

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
    private void askForLocationDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
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

    public void getLatestLocation() {
        //Check if pemission granted and return otherwise
        if (ActivityCompat.checkSelfPermission((MainActivity)mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            askForLocationDialog();
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
                    Log.e("Loc", "Lat:"+location.getLongitude() + ",Lon:"+location.getLatitude());
                    sendBCastupdate(location);
                }
            }
        }

        if (isGPSEnable) {
            location = null;
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            if (locationManager != null) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    Log.e("Loc", "Lat:"+location.getLongitude() + ",Lon:"+location.getLatitude());
                    sendBCastupdate(location);
                }
            }
        }
    }

    private void sendBCastupdate(Location location){
        Intent intent = new Intent(Constants.ACTION_CONTEXT_LOCATION);
        intent.putExtra(Constants.LAT_FLOAT_DATA,location.getLatitude());
        intent.putExtra(Constants.LON_FLOAT_DATA,location.getLongitude());
        intent.putExtra(Constants.ALT_FLOAT_DATA,location.getAltitude());
        sendBroadcast(intent);
    }

}

