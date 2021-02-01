/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.spaceContext;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.spaceContext.Orientation_Provider.Orientation_Data;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    // System sensor manager instance.
    private SensorManager mSensorManager;
    // Accelerometer and magnetometer sensors, as retrieved from the
    // sensor manager.
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagnetometer;

    //Add member variables to hold copies of the accelerometer and magnetometer data
    float[] mAccelerometerData = new float[3];
    float[] mMagnetometerData = new float[3];

    // TextViews to display current sensor values.
    private TextView mTextSensorAzimuth;
    private TextView mTextSensorPitch;
    private TextView mTextSensorRoll;

    // Very small values for the accelerometer (on all three axes) should
    // be interpreted as 0. This value is the amount of acceptable
    // non-zero drift.
    private static final float VALUE_DRIFT = 0.05f;
    private DatabaseHelper dbHelper;
    private static SQLiteDatabase database = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lock the orientation to portrait (for now)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mTextSensorAzimuth = (TextView) findViewById(R.id.value_azimuth);
        mTextSensorPitch = (TextView) findViewById(R.id.value_pitch);
        mTextSensorRoll = (TextView) findViewById(R.id.value_roll);

        // Get accelerometer and magnetometer sensors from the sensor manager.
        // The getDefaultSensor() method returns null if the sensor
        // is not available on the device.
        mSensorManager = (SensorManager) getSystemService(
                Context.SENSOR_SERVICE);
        mSensorAccelerometer = mSensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
        mSensorMagnetometer = mSensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD);

        IntentFilter filter = new IntentFilter(Constants.ACTION_CONTEXT_ORIENTATION);
        registerReceiver(orientBCastReceiver, filter);
        dbHelper = new DatabaseHelper(getApplicationContext(), Orientation_Provider.DATABASE_NAME, null, 1, Orientation_Provider.DATABASE_TABLES,Orientation_Provider.TABLES_FIELDS);

    }

    /**
     * Listeners for the sensors are registered in this callback so that
     * they can be unregistered in onStop().
     */
    @Override
    protected void onStart() {
        super.onStart();

        // Listeners for the sensors are registered in this callback and
        // can be unregistered in onStop().
        if (mSensorAccelerometer != null) {
            mSensorManager.registerListener(this, mSensorAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mSensorMagnetometer != null) {
            mSensorManager.registerListener(this, mSensorMagnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(orientBCastReceiver);
//        unregisterReceiver(orientationBCastReceiver);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                mAccelerometerData = sensorEvent.values.clone();
                Intent accelData = new Intent(Constants.ACTION_CONTEXT_ORIENTATION);
                accelData.putExtra("type",Constants.ACCEL_FLOAT_DATA);
                accelData.putExtra(Constants.ACTION_CONTEXT_ORIENTATION,mAccelerometerData);
                sendBroadcast(accelData);

                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagnetometerData = sensorEvent.values.clone();
                Intent magnetData = new Intent(Constants.ACTION_CONTEXT_ORIENTATION);
                magnetData.putExtra("type",Constants.MAGNET_FLOAT_DATA);
                magnetData.putExtra(Constants.ACTION_CONTEXT_ORIENTATION,mMagnetometerData);
                sendBroadcast(magnetData);
                break;
            default:
                return;
        }
    }


    /**
     * Must be implemented to satisfy the SensorEventListener interface;
     * unused in this app.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }


    private BroadcastReceiver orientBCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

//            Bundle extras = getIntent().getExtras();
//            String type = intent.getStringExtra("type");
//            float[] data = extras.getFloatArray(Constants.ACTION_CONTEXT_ORIENTATION);
//            if(type.equals("ACCEL")){
//                mAccelerometerData = data;
//            }else{
//                mMagnetometerData = data;
//            }

            //Compute the orientation
            float[] rotationMatrix = new float[9];
            boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                    null, mAccelerometerData, mMagnetometerData);

            float orientationValues[] = new float[3];
            if (rotationOK) {
                SensorManager.getOrientation(rotationMatrix, orientationValues);

                float azimuth = orientationValues[0];
                float pitch = orientationValues[1];
                float roll = orientationValues[2];

                //Insert orientation Data into SQLite DB
                ContentValues values = new ContentValues();
                values.put(Orientation_Provider.Orientation_Data.COLUMN_AZIMUTH, azimuth);
                values.put(Orientation_Provider.Orientation_Data.COLUMN_PITCH, pitch);
                values.put(Orientation_Provider.Orientation_Data.COLUMN_ROLL, roll);

                // Insert the new row, returning the primary key value of the new row
                Uri orientDataUri = getContentResolver().insert(Orientation_Data.CONTENT_URI,values );
                Toast.makeText(context, orientDataUri.toString(),Toast.LENGTH_LONG).show();

                mTextSensorAzimuth.setText(getResources().getString(
                        R.string.value_format, azimuth));
                mTextSensorPitch.setText(getResources().getString(
                        R.string.value_format, pitch));
                mTextSensorRoll.setText(getResources().getString(
                        R.string.value_format, roll));
            }
        }
    };



}