package com.spacecontext.bcastreceivers;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.spacecontext.providers.Location_Provider.Location_Data;
import com.spacecontext.util.Constants;

public class LocationBCastReceiver extends BroadcastReceiver {
    public static String TAG = "VSpaceContext::LocationBCastReceiver";

    private float lat;
    private float lon;
    private float alt;

    public LocationBCastReceiver(float lat, float lon, float alt) {
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();
        this.lat = extras.getFloat(Constants.TEMP_FLOAT_DATA);
        this.lon = extras.getFloat(Constants.ILLUM_FLOAT_DATA);
        this.alt = extras.getFloat(Constants.PRESSURE_FLOAT_DATA);

        //Insert location Data into SQLite DB
        ContentValues values = new ContentValues();
        values.put(Location_Data.LATITUDE, lat);
        values.put(Location_Data.LONGITUDE, lon);
        values.put(Location_Data.ALTITUDE, alt);

        // Insert the new row, returning the primary key value of the new row
        Uri locDataUri = context.getContentResolver().insert(Location_Data.CONTENT_URI, values);
        Log.d(TAG, "Passive Location is saved  to " + locDataUri.toString());
    }
}
