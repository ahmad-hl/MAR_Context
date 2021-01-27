package com.example.android.spaceContext;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class DummyTask extends AsyncTask<SensorEvent, Integer, float []> {
    float orientationValues[];

    private Context mContext;

    public DummyTask (Context context) throws IOException {
        mContext = context;
    }

    @Override
    protected float[] doInBackground(SensorEvent... params) {
        //Add member variables to hold copies of the accelerometer and magnetometer data
        float[] mAccelerometerData = new float[3];
        float[] mMagnetometerData = new float[3];
        try {
            SensorEvent sensorEvent = params[0];
            int sensorType = sensorEvent.sensor.getType();
            switch (sensorType) {
                case Sensor.TYPE_ACCELEROMETER:
                    mAccelerometerData = sensorEvent.values.clone();
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mMagnetometerData = sensorEvent.values.clone();
                    break;
                default:
                    break;
            }

            float[] rotationMatrix = new float[9];
            boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                    null, mAccelerometerData, mMagnetometerData);

            orientationValues = new float[3];
            if (rotationOK) {
                SensorManager.getOrientation(rotationMatrix, orientationValues);
            }

            return orientationValues;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPreExecute() {

    }


    @Override
    protected void onProgressUpdate(Integer... percent) {

    }

    @Override
    protected void onCancelled() {

    }

    @Override
    protected void onPostExecute(float[] orientationValues) {
        float azimuth = orientationValues[0];
        float pitch = orientationValues[1];
        float roll = orientationValues[2];

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("azimuth", azimuth);
            jsonObject.put("pitch", pitch);
            jsonObject.put("roll", roll);

            String userString = jsonObject.toString();
            Log.d(mContext.toString(),userString);

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }
}
