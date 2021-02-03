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

package com.spacecontext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.sqlite.SQLiteDatabase;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.spacecontext.providers.Orientation_Provider;
import com.spacecontext.services.AccelerationLogger;
import com.spacecontext.services.LocationLogger;
import com.spacecontext.services.MagnetLogger;

import com.spacecontext.bcastreceivers.OrientationBCastReceiver;

public class MainActivity extends AppCompatActivity {


    // TextViews to display current sensor values.
    private TextView mTextSensorAzimuth;
    private TextView mTextSensorPitch;
    private TextView mTextSensorRoll;

    private float[] mAccelerometerData = null;
    private float[] mMagnetometerData = null;

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

        IntentFilter filter = new IntentFilter(Constants.ACTION_CONTEXT_ORIENTATION);
        registerReceiver(msensorBCastReceiver, filter);

        IntentFilter orientFilter = new IntentFilter(Constants.ACTION_CONTEXT_ORIENTATION);
        registerReceiver(orientBCastReceiver, orientFilter);

        dbHelper = new DatabaseHelper(getApplicationContext(), Orientation_Provider.DATABASE_NAME, null, 1, Orientation_Provider.DATABASE_TABLES,Orientation_Provider.TABLES_FIELDS);


        Intent locServiceIntent = new Intent(this, LocationLogger.class);
        locServiceIntent.putExtra("inputExtra", "Location Service Started");
        ContextCompat.startForegroundService(this, locServiceIntent);

        Intent accelServiceIntent = new Intent(this, AccelerationLogger.class);
        accelServiceIntent.putExtra("inputExtra", "Acceleration Service Started");
        ContextCompat.startForegroundService(this, accelServiceIntent);

        Intent magnetServiceIntent = new Intent(this, MagnetLogger.class);
        magnetServiceIntent.putExtra("inputExtra", "Magnet Service Started");
        ContextCompat.startForegroundService(this, magnetServiceIntent);
    }

    /**
     * Listeners for the sensors are registered in this callback so that
     * they can be unregistered in onStop().
     */
    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Intent locNotiIntent = new Intent(this, LocationLogger.class);
        stopService(locNotiIntent);

        Intent accelNotiIntent = new Intent(this, AccelerationLogger.class);
        stopService(accelNotiIntent);

        Intent magnetNotiIntent = new Intent(this, MagnetLogger.class);
        stopService(magnetNotiIntent);


        unregisterReceiver(msensorBCastReceiver);
        unregisterReceiver(orientBCastReceiver);
    }

    private final OrientationBCastReceiver orientBCastReceiver = new OrientationBCastReceiver(mAccelerometerData,mMagnetometerData);

    private BroadcastReceiver msensorBCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle extras = intent.getExtras();
            String type = extras.getString("type");
            float[] data = null;
            if (type.equals(Constants.ACCEL_DATA)) {
                mAccelerometerData = extras.getFloatArray(Constants.ACCEL_FLOAT_DATA);
                mTextSensorAzimuth.setText(getResources().getString(
                        R.string.value_format, mAccelerometerData[0]));
                mTextSensorPitch.setText(getResources().getString(
                        R.string.value_format, mAccelerometerData[1]));
                mTextSensorRoll.setText(getResources().getString(
                        R.string.value_format, mAccelerometerData[2]));
            } else {
                mMagnetometerData = extras.getFloatArray(Constants.MAGNET_FLOAT_DATA);
                mTextSensorAzimuth.setText(getResources().getString(
                        R.string.value_format, mMagnetometerData[0]));
                mTextSensorPitch.setText(getResources().getString(
                        R.string.value_format, mMagnetometerData[1]));
                mTextSensorRoll.setText(getResources().getString(
                        R.string.value_format, mMagnetometerData[2]));

            }
        }
    };

}