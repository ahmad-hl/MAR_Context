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

public class Environment_Provider extends ContentProvider {
    public static String AUTHORITY = "com.spacecontext.provider.env";
    private DatabaseHelper dbHelper;
    private static SQLiteDatabase database;
    public static String DATABASE_NAME = "env.db";
    public static final int DATABASE_VERSION = 2;

    private UriMatcher sUriMatcher;
    public final static  String[] DATABASE_TABLES = {"env"};

    public final static String[] TABLES_FIELDS= {Environment_Data._ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+ Environment_Data.AMB_AIR_TEMPERATURE+" REAL NOT NULL,"+ Environment_Data.AMB_ILLUMINANCE+" REAL NOT NULL,"+ Environment_Data.AMB_AIR_PRESSURE+" REAL NOT NULL, "+ Environment_Data.AMB_AIR_HUMIDITY+" REAL NOT NULL, "+ Environment_Data.COLUMN_TIMESTAMP+" TIMESTAMP DEFAULT CURRENT_TIMESTAMP"};

    // code for query paths
    private final int ENV_DATA = 1;
    private final int ENV_DATA_ID = 2;

    @Override
    public boolean onCreate() {
        AUTHORITY = "com.spacecontext.provider.env";
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Environment_Provider.AUTHORITY, DATABASE_TABLES[0], ENV_DATA);
        sUriMatcher.addURI(Environment_Provider.AUTHORITY, DATABASE_TABLES[0] + "/#",ENV_DATA_ID);

        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        initialiseDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setStrict(true);
        switch (sUriMatcher.match(uri)) {
            case ENV_DATA:
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
            case ENV_DATA:
                return Environment_Data.CONTENT_TYPE;
            case ENV_DATA_ID:
                return Environment_Data.CONTENT_ITEM_TYPE;
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
            case ENV_DATA:
                long envData_id = database.insertWithOnConflict(DATABASE_TABLES[0], null, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (envData_id > 0) {
                    Uri envDataUri = ContentUris.withAppendedId(Environment_Data.CONTENT_URI, envData_id);
                    getContext().getContentResolver().notifyChange(envDataUri, null, NOTIFY_INSERT);
                    database.setTransactionSuccessful();
                    database.endTransaction();
                    return envDataUri;
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
            case ENV_DATA:
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
            case ENV_DATA:
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

    public static final class Environment_Data implements BaseColumns {
        // make the constructor private.
        private Environment_Data() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://"
                + Environment_Provider.AUTHORITY + "/env");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.spacecontext.env.data";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.spacecontext.env.data";

        public static final String AMB_AIR_TEMPERATURE= "ambientAirTemp";
        public static final String AMB_ILLUMINANCE= "illuminance";
        public static final String AMB_AIR_PRESSURE = "ambientAirPressure";
        public static final String AMB_AIR_HUMIDITY = "ambientAirHumidity";
        public static final String COLUMN_TIMESTAMP = "timestamp";
    }

    private void initialiseDatabase() {
        if (dbHelper == null)
            dbHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        if (database == null)
            database = dbHelper.getWritableDatabase();
    }
}

