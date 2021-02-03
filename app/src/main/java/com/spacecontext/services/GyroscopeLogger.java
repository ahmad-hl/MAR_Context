package com.spacecontext.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.spacecontext.Constants;
import com.spacecontext.MainActivity;
import com.spacecontext.R;
import com.spacecontext.providers.Gyroscope_Provider.Gyroscope_Data;
import com.spacecontext.providers.Gyroscope_Provider;

public class GyroscopeLogger extends Service implements SensorEventListener {
    public static String TAG = "VSpaceContext::GyroscopeLogger";

    // System sensor manager instance.
    private SensorManager mSensorManager;
    // Gyroscope and magnetometer sensors, as retrieved from sensor manager.
    private Sensor mSensorGyroscope;

    private static final float SHAKE_THRESHOLD = 15.00f; // m/S**2
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECS = 1000;
    private long mLastShakeTime;


    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            long curTime = System.currentTimeMillis();
            if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECS) {

                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];

                double gyroscope = Math.sqrt(Math.pow(x, 2) +
                        Math.pow(y, 2) +
                        Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH;
                Log.d("mySensor", " Gyroscope is " + gyroscope + "m/s^2");

                if (gyroscope > SHAKE_THRESHOLD) {
                    mLastShakeTime = curTime;
                    Toast.makeText(getApplicationContext(), "FALL DETECTED",
                            Toast.LENGTH_LONG).show();
                }

                //Insert Gyroscope Data into SQLite DB
                ContentValues values = new ContentValues();
                values.put(Gyroscope_Data.COLUMN_X, sensorEvent.values[0]);
                values.put(Gyroscope_Data.COLUMN_Y, sensorEvent.values[1]);
                values.put(Gyroscope_Data.COLUMN_Z, sensorEvent.values[2]);
                values.put(Gyroscope_Data.COLUMN_ACCURACY, sensorEvent.accuracy);

                // Insert the new row, returning the primary key value of the new row
                Uri gyroDataUri = getContentResolver().insert(Gyroscope_Data.CONTENT_URI,values );
                Log.d("mySensor", "Gyroscope is saved  to " + gyroDataUri.toString());
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Start Detecting", Toast.LENGTH_LONG).show();

        // Get gyroscope sensors from the sensor manager.
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, mSensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        //here u should make your service foreground so it will keep working even if app closed
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle("Gyroscope Service")
                .setSmallIcon(R.drawable.ic_android)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        return Service.START_STICKY;
    }


}
