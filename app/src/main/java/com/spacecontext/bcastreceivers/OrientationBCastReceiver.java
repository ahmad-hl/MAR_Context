package com.spacecontext.bcastreceivers;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;

import com.spacecontext.Constants;
import com.spacecontext.providers.Orientation_Provider;
import com.spacecontext.providers.Orientation_Provider.Orientation_Data;

import android.util.Log;
import android.widget.Toast;

public class OrientationBCastReceiver extends BroadcastReceiver {
    public static String TAG = "VSpaceContext::OrientationBCastReceiver";

    float[] mAccelerometerData;
    float[] mMagnetometerData;

    public OrientationBCastReceiver(float[] mAccelerometerData, float[] mMagnetometerData) {
        this.mAccelerometerData = mAccelerometerData;
        this.mMagnetometerData = mMagnetometerData;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();
        String type = extras.getString("type");
        float[] data = null;
        if (type.equals(Constants.ACCEL_DATA)) {
            this.mAccelerometerData = extras.getFloatArray(Constants.ACCEL_FLOAT_DATA);
        } else if (type.equals(Constants.MAGNET_DATA)) {
            this.mMagnetometerData = extras.getFloatArray(Constants.MAGNET_FLOAT_DATA);
        }

        if( mAccelerometerData ==null || mMagnetometerData==null)
            return;

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
            Uri orientDataUri = context.getContentResolver().insert(Orientation_Data.CONTENT_URI, values);
            Log.d(TAG, "Orientation is saved  to " + orientDataUri.toString());
        }
    }
}
