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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.spacecontext.util.DatabaseHelper;

public class Gyroscope_Provider extends ContentProvider {
    public static String AUTHORITY = "com.spacecontext.provider.gyroscope";
    private DatabaseHelper dbHelper;
    private static SQLiteDatabase database;
    public static String DATABASE_NAME = "gyroscope.db";
    public static final int DATABASE_VERSION = 2;

    private UriMatcher sUriMatcher;
    public final static  String[] DATABASE_TABLES = {"gyroscope"};
    public final static String[] TABLES_FIELDS= {Gyroscope_Data._ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+ Gyroscope_Data.COLUMN_X+" REAL NOT NULL,"+ Gyroscope_Data.COLUMN_Y+" REAL NOT NULL,"+ Gyroscope_Data.COLUMN_Z+" REAL NOT NULL, "+ Gyroscope_Data.COLUMN_ACCURACY+" REAL NOT NULL, "+ Gyroscope_Data.COLUMN_TIMESTAMP+" TIMESTAMP DEFAULT CURRENT_TIMESTAMP"};

    // code for query paths
    private final int GYROSCOPE_DATA = 1;
    private final int GYROSCOPE_DATA_ID = 2;

    @Override
    public boolean onCreate() {
        AUTHORITY = "com.spacecontext.provider.gyroscope";
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Gyroscope_Provider.AUTHORITY, DATABASE_TABLES[0], GYROSCOPE_DATA);
        sUriMatcher.addURI(Gyroscope_Provider.AUTHORITY, DATABASE_TABLES[0] + "/#", GYROSCOPE_DATA_ID);

        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        initialiseDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setStrict(true);
        switch (sUriMatcher.match(uri)) {
            case GYROSCOPE_DATA:
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
            case GYROSCOPE_DATA:
                return Gyroscope_Data.CONTENT_TYPE;
            case GYROSCOPE_DATA_ID:
                return Gyroscope_Data.CONTENT_ITEM_TYPE;
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
            case GYROSCOPE_DATA:
                long gyroData_id = database.insertWithOnConflict(DATABASE_TABLES[0], null, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (gyroData_id > 0) {
                    Uri gyroDataUri = ContentUris.withAppendedId(Gyroscope_Data.CONTENT_URI, gyroData_id);
                    getContext().getContentResolver().notifyChange(gyroDataUri, null, false);
                    database.setTransactionSuccessful();
                    database.endTransaction();
                    return gyroDataUri;
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
            case GYROSCOPE_DATA:
                count =  database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null, false);

        return count;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {
        initialiseDatabase();
        database.beginTransaction();
        int count;
        switch (sUriMatcher.match(uri)) {
            case GYROSCOPE_DATA:
                count = database.update(DATABASE_TABLES[0], contentValues, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        getContext().getContentResolver().notifyChange(uri, null, false);

        return count;
    }

    public static final class Gyroscope_Data implements BaseColumns {
        // make the constructor private.
        private Gyroscope_Data() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://"
                + Gyroscope_Provider.AUTHORITY + "/gyroscope");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.spacecontext.gyroscope.data";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.spacecontext.gyroscope.data";

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

