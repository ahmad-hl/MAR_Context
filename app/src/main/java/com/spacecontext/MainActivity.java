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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;

import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.spacecontext.bcastreceivers.EnvBCastReceiver;
import com.spacecontext.providers.Orientation_Provider;
import com.spacecontext.services.AccelerationLogger;
import com.spacecontext.services.EnvironmentLogger;
import com.spacecontext.services.GyroscopeLogger;
import com.spacecontext.services.LocationLogger;
import com.spacecontext.services.MagnetLogger;

import com.spacecontext.bcastreceivers.OrientationBCastReceiver;
import com.spacecontext.services.PassiveLocationLogger;
import com.spacecontext.util.Constants;
import com.spacecontext.util.DatabaseHelper;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "VSpaceContext::MainActivity";

    // TextViews to display current sensor values.
    private TextView mTextSensorAzimuth;
    private TextView mTextSensorPitch;
    private TextView mTextSensorRoll;

    //Motion data
    private float[] mAccelerometerData = null;
    private float[] mMagnetometerData = null;

    //Environment data
    private float ambientAirTemp_c;
    private float illuminance_lx;
    private float ambientAirPressure;
    private float ambientAirHumidity;

    // Very small values for the accelerometer (on all three axes) should be interpreted as 0. This value is the amount of acceptable non-zero drift.
    private static final float VALUE_DRIFT = 0.05f;
    private DatabaseHelper dbHelper;

    // To invoke the bound service, first make sure that this value
    // is not null.
    private PassiveLocationLogger locationLogger;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            locationLogger = ((PassiveLocationLogger.LocalBinder)service).getService();
        }
        public void onServiceDisconnected(ComponentName className) {
            locationLogger = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lock the orientation to portrait (for now)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        dbHelper = new DatabaseHelper(getApplicationContext(), Orientation_Provider.DATABASE_NAME, null, 1, Orientation_Provider.DATABASE_TABLES,Orientation_Provider.TABLES_FIELDS);

        mTextSensorAzimuth = (TextView) findViewById(R.id.value_azimuth);
        mTextSensorPitch = (TextView) findViewById(R.id.value_pitch);
        mTextSensorRoll = (TextView) findViewById(R.id.value_roll);

        //Start the sensor service
//        Intent locServiceIntent = new Intent(this, LocationLogger.class);
//        locServiceIntent.putExtra("inputExtra", "Location Service Started");
//        startService(locServiceIntent)

        Intent passiveLocServiceIntent = new Intent(this, PassiveLocationLogger.class);
        passiveLocServiceIntent.putExtra("inputExtra", "Passive Location Service Started");
        boolean boundStatus = bindService(passiveLocServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
        if (boundStatus) {
            startService(passiveLocServiceIntent);
        } else {
            Log.e("MY_APP_TAG", "Error: The requested service doesn't " + "exist, or this client isn't allowed access to it.");
        }
        
        Intent accelServiceIntent = new Intent(this, AccelerationLogger.class);
        accelServiceIntent.putExtra("inputExtra", "Acceleration Service Started");
        startService(accelServiceIntent);
        Intent magnetServiceIntent = new Intent(this, MagnetLogger.class);
        magnetServiceIntent.putExtra("inputExtra", "Magnet Service Started");
        startService(magnetServiceIntent);
        Intent gyroscopeServiceIntent = new Intent(this, GyroscopeLogger.class);
        gyroscopeServiceIntent.putExtra("inputExtra", "Gyroscope Service Started");
        startService(gyroscopeServiceIntent);
        Intent envServiceIntent = new Intent(this, EnvironmentLogger.class);
        envServiceIntent.putExtra("inputExtra", "Environment Service Started");
        startService(envServiceIntent);

        //Register Broadcast Receivers
        IntentFilter filter = new IntentFilter(Constants.ACTION_CONTEXT_ORIENTATION);
        registerReceiver(msensorBCastReceiver, filter);
        IntentFilter orientFilter = new IntentFilter(Constants.ACTION_CONTEXT_ORIENTATION);
        registerReceiver(orientBCastReceiver, orientFilter);
        IntentFilter envFilter = new IntentFilter(Constants.ACTION_CONTEXT_ENVIRONMENT);
        registerReceiver(envBCastReceiver, envFilter);
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

        //Stop the sensor service
        Intent locNotiIntent = new Intent(this, LocationLogger.class);
        stopService(locNotiIntent);
        Intent accelNotiIntent = new Intent(this, AccelerationLogger.class);
        stopService(accelNotiIntent);
        Intent magnetNotiIntent = new Intent(this, MagnetLogger.class);
        stopService(magnetNotiIntent);
        Intent gyroscopeNotiIntent = new Intent(this, GyroscopeLogger.class);
        stopService(gyroscopeNotiIntent);
        Intent envNotiIntent = new Intent(this, EnvironmentLogger.class);
        stopService(envNotiIntent);

        //Unregister Broadcast Receiver
        unregisterReceiver(msensorBCastReceiver);
        unregisterReceiver(orientBCastReceiver);
        unregisterReceiver(envBCastReceiver);
    }

    private final OrientationBCastReceiver orientBCastReceiver = new OrientationBCastReceiver(mAccelerometerData,mMagnetometerData);
    private final EnvBCastReceiver envBCastReceiver = new EnvBCastReceiver(ambientAirTemp_c,illuminance_lx, ambientAirPressure,ambientAirHumidity );

    private BroadcastReceiver msensorBCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle extras = intent.getExtras();
            String type = extras.getString("type");
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

    public void getPassiveLocation(View view) {
        locationLogger.getLatestLocation(MainActivity.this);
    }
}