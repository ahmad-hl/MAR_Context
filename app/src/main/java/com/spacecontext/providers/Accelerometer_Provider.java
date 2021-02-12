package com.spacecontext.providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spacecontext.util.DatabaseHelper;

import static android.content.ContentResolver.NOTIFY_DELETE;
import static android.content.ContentResolver.NOTIFY_INSERT;
import static android.content.ContentResolver.NOTIFY_UPDATE;

public class Accelerometer_Provider extends ContentProvider {
    public static String TAG = "VSpaceContext::Accelerometer_Provider";

    public static String AUTHORITY = "com.spacecontext.provider.accelerometer";
    private DatabaseHelper dbHelper;
    private static SQLiteDatabase database;
    public static String DATABASE_NAME = "accelerometer.db";
    public static final int DATABASE_VERSION = 2;

    private UriMatcher sUriMatcher;
    public final static  String[] DATABASE_TABLES = {"accelerometer"};
    public final static String[] TABLES_FIELDS= {Accelerometer_Data._ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+ Accelerometer_Data.COLUMN_X+" REAL NOT NULL,"+ Accelerometer_Data.COLUMN_Y+" REAL NOT NULL,"+ Accelerometer_Data.COLUMN_Z+" REAL NOT NULL, "+ Accelerometer_Data.COLUMN_ACCURACY+" REAL NOT NULL, "+ Accelerometer_Data.COLUMN_TIMESTAMP+" TIMESTAMP DEFAULT CURRENT_TIMESTAMP"};

    // code for query paths
    private final int ACCEL_DATA = 1;
    private final int ACCEL_DATA_ID = 2;

    @Override
    public boolean onCreate() {
        AUTHORITY = "com.spacecontext.provider.accelerometer";
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Accelerometer_Provider.AUTHORITY, DATABASE_TABLES[0], ACCEL_DATA);
        sUriMatcher.addURI(Accelerometer_Provider.AUTHORITY, DATABASE_TABLES[0] + "/#", ACCEL_DATA_ID);

        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        initialiseDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setStrict(true);
        switch (sUriMatcher.match(uri)) {
            case ACCEL_DATA:
                qb.setTables(DATABASE_TABLES[0]);
//                qb.setProjectionMap(accelDataMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs, null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
//            if (Aware.DEBUG) Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case ACCEL_DATA:
                return Accelerometer_Data.CONTENT_TYPE;
            case ACCEL_DATA_ID:
                return Accelerometer_Data.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        initialiseDatabase();
        ContentValues values = (initialValues != null) ? new ContentValues(initialValues) : new ContentValues();
        database.beginTransaction();

        switch (sUriMatcher.match(uri)) {
            case ACCEL_DATA:
                long accelData_id = database.insertWithOnConflict(DATABASE_TABLES[0], null, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (accelData_id > 0) {
                    Uri accelDataUri = ContentUris.withAppendedId(Accelerometer_Data.CONTENT_URI, accelData_id);
                    getContext().getContentResolver().notifyChange(accelDataUri, null,  NOTIFY_INSERT);
                    database.setTransactionSuccessful();
                    database.endTransaction();
                    return accelDataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        initialiseDatabase();
        database.beginTransaction();
        int count;
        switch (sUriMatcher.match(uri)) {
            case ACCEL_DATA:
                count =  database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null, NOTIFY_DELETE);

        return count;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {
        initialiseDatabase();
        database.beginTransaction();
        int count;
        switch (sUriMatcher.match(uri)) {
            case ACCEL_DATA:
                count = database.update(DATABASE_TABLES[0], contentValues, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        getContext().getContentResolver().notifyChange(uri, null, NOTIFY_UPDATE);

        return count;
    }

    public static final class Accelerometer_Data implements BaseColumns {
        // make the constructor private.
        private Accelerometer_Data() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://"
                + Accelerometer_Provider.AUTHORITY + "/accelerometer");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.spacecontext.accelerometer.data";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.spacecontext.accelerometer.data";

        public static final String COLUMN_X= "x";
        public static final String COLUMN_Y= "y";
        public static final String COLUMN_Z = "z";
        public static final String COLUMN_ACCURACY = "accuracy";
        public static final String COLUMN_TIMESTAMP = "timestamp";
    }

    private void initialiseDatabase() {
        if (dbHelper == null)
            dbHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        if (database == null)
            database = dbHelper.getWritableDatabase();
    }
}

