package com.fenchtose.mapsdemo.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jay Rambhia on 8/2/16.
 */
public class LocationDao implements AbstractDao<LocationDbItem> {

    public static final String TABLENAME = "LOCATIONS";

    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_LATITUDE = "LATITUDE";
    public static final String COLUMN_LONGITUDE = "LONGITUDE";

    private final SQLiteDatabase db;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLENAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY NOT NULL , " +
                COLUMN_LATITUDE + " DOUBLE NOT NULL , " +
                COLUMN_LONGITUDE + " DOUBLE NOT NULL);"

        );
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLENAME);
    }

    public LocationDao(SQLiteDatabase db) {
        this.db = db;
    }

    @Override
    public int add(@NonNull LocationDbItem item) {
        ContentValues value = new ContentValues();

        value.put(COLUMN_ID, item.getId());
        value.put(COLUMN_LATITUDE, item.getLatitude());
        value.put(COLUMN_LONGITUDE, item.getLongitude());

        db.insert(TABLENAME, null, value);
        return 1;
    }

    @Nullable
    @Override
    public LocationDbItem get(int id) {
        Cursor cursor = db.query(TABLENAME, new String[]{COLUMN_ID, COLUMN_LATITUDE, COLUMN_LONGITUDE},
                COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            LocationDbItem item = new LocationDbItem(Integer.parseInt(cursor.getString(0)), cursor.getDouble(1), cursor.getDouble(2));
            cursor.close();
            return item;
        }

        return null;
    }

    @Override
    public List<LocationDbItem> getAll() {
        List<LocationDbItem> items = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLENAME, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {

                    items.add(new LocationDbItem(Integer.parseInt(cursor.getString(0)), cursor.getDouble(1), cursor.getDouble(2)));

                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return items;
    }

    @Override
    public int getCount() {
        return (int) DatabaseUtils.queryNumEntries(db, TABLENAME);
    }

    @Override
    public int update(@NonNull LocationDbItem item) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_LATITUDE, item.getLatitude());
        values.put(COLUMN_LONGITUDE, item.getLongitude());

        return db.update(TABLENAME, values, COLUMN_ID + " =? ", new String[]{String.valueOf(item.getId())});
    }

    @Override
    public int delete(@NonNull LocationDbItem item) {
        return db.delete(TABLENAME, COLUMN_ID + " =? " , new String[] {String.valueOf(item.getId())});
    }
}
