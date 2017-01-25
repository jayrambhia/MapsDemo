package com.fenchtose.mapsdemo.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Jay Rambhia on 8/2/16.
 */
public class DatabaseHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "locations_v1.db";
    private static final int DATABASE_VERSION = 1;

    // Locations Table
    private static final String LOCATIONS_TABLE = "LOCATIONS";
    // Columns
    private static final String KEY_ID = "ID";
    private static final String KEY_LATITUDE = "LATITUDE";
    private static final String KEY_LONGITUDE = "LONGITUDE";

    private AbstractDao<LocationDbItem> locationDao;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        LocationDao.createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO do upgrade
    }

    public synchronized AbstractDao<LocationDbItem> getLocationDao() {
        if (locationDao == null) {
            locationDao = new LocationDao(getWritableDatabase());
        }

        return locationDao;
    }
}
