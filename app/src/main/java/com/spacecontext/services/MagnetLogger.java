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

import com.spacecontext.util.Constants;
import com.spacecontext.MainActivity;
import com.spacecontext.R;
import com.spacecontext.providers.Magnetometer_Provider.Magnetometer_Data;

public class MagnetLogger extends Service implements SensorEventListener {
    public static String TAG = "VSpaceContext::MagnetLogger";

    // System sensor manager instance.
    private SensorManager mSensorManager;
    // Accelerometer and magnetometer sensors, as retrieved from sensor manager.
    private Sensor mSensorMagnetometer;

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
        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            long curTime = System.currentTimeMillis();
            if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECS) {

                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];

                double magnet = Math.sqrt(Math.pow(x, 2) +
                        Math.pow(y, 2) +
                        Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH;
                Log.d("mySensor", "Magnet is " + magnet + "m/s^2");
                float[] mMagnetometerData  = sensorEvent.values.clone();

                //Insert orientation Data into SQLite DB
                ContentValues values = new ContentValues();
                values.put(Magnetometer_Data.COLUMN_X, sensorEvent.values[0]);
                values.put(Magnetometer_Data.COLUMN_Y, sensorEvent.values[1]);
                values.put(Magnetometer_Data.COLUMN_Z, sensorEvent.values[2]);
                values.put(Magnetometer_Data.COLUMN_ACCURACY, sensorEvent.accuracy);

                // Insert the new row, returning the primary key value of the new row
                Uri magnetDataUri = getContentResolver().insert(Magnetometer_Data.CONTENT_URI,values );
                Log.d("mySensor", "Magnet field is saved  to " + magnetDataUri.toString());

                Intent magnetData = new Intent(Constants.ACTION_CONTEXT_ORIENTATION);
                magnetData.putExtra("type",Constants.MAGNET_DATA);
                magnetData.putExtra(Constants.MAGNET_FLOAT_DATA,mMagnetometerData);
                sendBroadcast(magnetData);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Start Detecting", Toast.LENGTH_LONG).show();

        // Get accelerometer and magnetometer sensors from the sensor manager.
        // The getDefaultSensor() method returns null if the sensor is not available on the device.
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, mSensorMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);

        //here u should make your service foreground so it will keep working even if app closed
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, NotificationChannelApp.CHANNEL_ID)
                .setContentTitle("Magnetometer Service")
                .setSmallIcon(R.drawable.ic_android)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        return Service.START_STICKY;
    }
}
