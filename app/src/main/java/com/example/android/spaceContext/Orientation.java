package com.example.android.spaceContext;

import android.provider.BaseColumns;

public final class Orientation  implements BaseColumns {
    // make the constructor private.
    private Orientation() {}

    public static final String TABLE_NAME = "orientation";
    public static final String COLUMN_AZIMUTH = "azimuth";
    public static final String COLUMN_PITCH = "pitch";
    public static final String COLUMN_ROLL = "roll";
    public static final String COLUMN_TIMESTAMP = "timestamp";
}