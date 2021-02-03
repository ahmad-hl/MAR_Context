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
import com.spacecontext.providers.Accelerometer_Provider.Accelerometer_Data;

public class EnvironmentLogger extends Service implements SensorEventListener {
    public static String TAG = "VSpaceContext::EnvironmentLogger";

    // System sensor manager instance.
    private SensorManager mSensorManager;
    // Accelerometer and magnetometer sensors, as retrieved from sensor manager.
    private Sensor mSensorPressure;
    private Sensor mSensorTemp;
    private Sensor mSensorHumidity;
    private Sensor mSensorLight;

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

        if (sensorEvent.sensor.getType() == Sensor.TYPE_PRESSURE) {
            float pressure = sensorEvent.values[0];
            Intent pressureData = new Intent(Constants.ACTION_CONTEXT_ENVIRONMENT);
            pressureData.putExtra("type", Constants.PRESSURE_DATA);
            pressureData.putExtra(Constants.PRESSURE_FLOAT_DATA, pressure);
            sendBroadcast(pressureData);
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            float ambientTemp = sensorEvent.values[0];
            Intent tempData = new Intent(Constants.ACTION_CONTEXT_ENVIRONMENT);
            tempData.putExtra("type", Constants.TEMP_DATA);
            tempData.putExtra(Constants.TEMP_FLOAT_DATA, ambientTemp);
            sendBroadcast(tempData);
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT) {
            float illuminance = sensorEvent.values[0];
            Intent lightData = new Intent(Constants.ACTION_CONTEXT_ENVIRONMENT);
            lightData.putExtra("type", Constants.ILLUM_DATA);
            lightData.putExtra(Constants.ILLUM_FLOAT_DATA, illuminance);
            sendBroadcast(lightData);
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
            float humidity = sensorEvent.values[0];
            Intent humidityData = new Intent(Constants.ACTION_CONTEXT_ENVIRONMENT);
            humidityData.putExtra("type", Constants.HUMIDITY_DATA);
            humidityData.putExtra(Constants.HUMIDITY_FLOAT_DATA, humidity);
            sendBroadcast(humidityData);
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
        mSensorPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mSensorManager.registerListener(this, mSensorPressure, SensorManager.SENSOR_DELAY_NORMAL);

        mSensorTemp = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mSensorManager.registerListener(this, mSensorTemp, SensorManager.SENSOR_DELAY_NORMAL);

        mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(this, mSensorLight, SensorManager.SENSOR_DELAY_NORMAL);

        mSensorHumidity = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        mSensorManager.registerListener(this, mSensorHumidity, SensorManager.SENSOR_DELAY_NORMAL);

        //here u should make your service foreground so it will keep working even if app closed
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle("Environment Service")
                .setSmallIcon(R.drawable.ic_android)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        return Service.START_STICKY;
    }


}
