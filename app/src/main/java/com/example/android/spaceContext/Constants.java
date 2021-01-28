package com.example.android.spaceContext;

public class Constants {

    final static String ACTION_CONTEXT_ORIENTATION= "com.example.android.ACTION_CONTEXT_ORIENT";
    final static String ACCEL_FLOAT_DATA= "com.example.android.ACCEL_FLOAT_DATA";
    final static String MAGNET_FLOAT_DATA= "com.example.android.MAGNET_FLOAT_DATA";

    //Orientation DB
    public static final String DATABASE_NAME = "orientation.db";
    public static final int DATABASE_VERSION = 1;
    final static  String[] database_tables = {"orientation"};
    final static String[] table_fields= {Orientation._ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+Orientation.COLUMN_AZIMUTH+" REAL NOT NULL,"+Orientation.COLUMN_PITCH+" REAL NOT NULL,"+Orientation.COLUMN_ROLL+" REAL NOT NULL, "+Orientation.COLUMN_TIMESTAMP+" TIMESTAMP DEFAULT CURRENT_TIMESTAMP"};
}
