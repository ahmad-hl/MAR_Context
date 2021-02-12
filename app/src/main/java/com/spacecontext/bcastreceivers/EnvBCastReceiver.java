package com.spacecontext.bcastreceivers;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.spacecontext.util.Constants;
import com.spacecontext.providers.Environment_Provider.Environment_Data;

public class EnvBCastReceiver extends BroadcastReceiver {
    public static String TAG = "VSpaceContext::EnvBCastReceiver";

    private float ambientAirTemp_c;
    private float illuminance_lx;
    private float ambientAirPressure;
    private float ambientAirHumidity;

    public EnvBCastReceiver(float ambientAirTemp_c, float illuminance_lx, float ambientAirPressure, float ambientAirHumidity) {
        this.ambientAirTemp_c = ambientAirTemp_c;
        this.illuminance_lx = illuminance_lx;
        this.ambientAirPressure = ambientAirPressure;
        this.ambientAirHumidity = ambientAirHumidity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();
        String type = extras.getString("type");
        float[] data = null;
        if (type.equals(Constants.TEMP_DATA)) {
            this.ambientAirTemp_c = extras.getFloat(Constants.TEMP_FLOAT_DATA);
        } else if (type.equals(Constants.ILLUM_DATA)) {
            this.illuminance_lx = extras.getFloat(Constants.ILLUM_FLOAT_DATA);
        } else if (type.equals(Constants.PRESSURE_DATA)) {
            this.ambientAirPressure = extras.getFloat(Constants.PRESSURE_FLOAT_DATA);
        } else if (type.equals(Constants.HUMIDITY_DATA)) {
            this.ambientAirHumidity = extras.getFloat(Constants.HUMIDITY_FLOAT_DATA);
        }

//        Check if there is change and satisfy threshold
//        if( ambientAirTemp_c ==0 || illuminance_lx==0 )
//            return;

        //Insert environment Data into SQLite DB
        ContentValues values = new ContentValues();
        values.put(Environment_Data.AMB_AIR_TEMPERATURE, ambientAirTemp_c);
        values.put(Environment_Data.AMB_ILLUMINANCE, illuminance_lx);
        values.put(Environment_Data.AMB_AIR_PRESSURE, ambientAirPressure);
        values.put(Environment_Data.AMB_AIR_HUMIDITY, ambientAirHumidity);

        // Insert the new row, returning the primary key value of the new row
        Uri envDataUri = context.getContentResolver().insert(Environment_Data.CONTENT_URI, values);
        //Log.d(TAG, "Environment is saved  to " + envDataUri.toString());
    }
}
