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
import com.spacecontext.providers.Accelerometer_Provider;
import com.spacecontext.providers.Accelerometer_Provider.Accelerometer_Data;

public class AccelerationLogger extends Service implements SensorEventListener {
    public static String TAG = "VSpaceContext::AccelerationLogger";

    // System sensor manager instance.
    private SensorManager mSensorManager;
    // Accelerometer and magnetometer sensors, as retrieved from sensor manager.
    private Sensor mSensorAccelerometer;

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
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();
            if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECS) {

                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];

                double acceleration = Math.sqrt(Math.pow(x, 2) +
                        Math.pow(y, 2) +
                        Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH;
                Log.d("mySensor", "Acceleration is " + acceleration + "m/s^2");

                if (acceleration > SHAKE_THRESHOLD) {
                    mLastShakeTime = curTime;
                    Toast.makeText(getApplicationContext(), "FALL DETECTED",
                            Toast.LENGTH_LONG).show();
                }else{
                    float[] mAccelerometerData = sensorEvent.values.clone();

                    //Insert orientation Data into SQLite DB
                    ContentValues values = new ContentValues();
                    values.put(Accelerometer_Data.COLUMN_X, sensorEvent.values[0]);
                    values.put(Accelerometer_Data.COLUMN_Y, sensorEvent.values[1]);
                    values.put(Accelerometer_Data.COLUMN_Z, sensorEvent.values[2]);
                    values.put(Accelerometer_Data.COLUMN_ACCURACY, sensorEvent.accuracy);

                    // Insert the new row, returning the primary key value of the new row
                    Uri accelDataUri = getContentResolver().insert(Accelerometer_Provider.Accelerometer_Data.CONTENT_URI,values );
                    Log.d("mySensor", "Acceleration is saved  to " + accelDataUri.toString());

                    Intent accelData = new Intent(Constants.ACTION_CONTEXT_ORIENTATION);
                    accelData.putExtra("type",Constants.ACCEL_DATA);
                    accelData.putExtra(Constants.ACCEL_FLOAT_DATA,mAccelerometerData);
                    sendBroadcast(accelData);
                }
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
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        //here u should make your service foreground so it will keep working even if app closed
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle("Acceleration Service")
                .setSmallIcon(R.drawable.ic_android)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        return Service.START_STICKY;
    }


}
