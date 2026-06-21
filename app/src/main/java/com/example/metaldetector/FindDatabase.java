package com.example.metaldetector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class FindDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "find_places.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "find_places";

    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_LAT = "latitude";
    private static final String COL_LON = "longitude";
    private static final String COL_TIME = "timestamp";
    private static final String COL_AMP = "amplitude_db";
    private static final String COL_PHASE = "phase";
    private static final String COL_I = "i_value";
    private static final String COL_Q = "q_value";
    private static final String COL_RX = "rx_level";

    public FindDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_LAT + " REAL, " +
                COL_LON + " REAL, " +
                COL_TIME + " INTEGER, " +
                COL_AMP + " REAL, " +
                COL_PHASE + " REAL, " +
                COL_I + " REAL, " +
                COL_Q + " REAL, " +
                COL_RX + " REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public long addPlace(FindPlace place) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, place.getTitle());
        cv.put(COL_LAT, place.getLatitude());
        cv.put(COL_LON, place.getLongitude());
        cv.put(COL_TIME, place.getTimestamp());
        cv.put(COL_AMP, place.getAmplitudeDb());
        cv.put(COL_PHASE, place.getPhase());
        cv.put(COL_I, place.getIValue());
        cv.put(COL_Q, place.getQValue());
        cv.put(COL_RX, place.getRxLevel());
        return db.insert(TABLE, null, cv);
    }

    public List<FindPlace> getAllPlaces() {
        List<FindPlace> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE, null, null, null, null, null, COL_TIME + " DESC");
        if (c.moveToFirst()) {
            do {
                FindPlace p = new FindPlace();
                p.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
                p.setTitle(c.getString(c.getColumnIndexOrThrow(COL_TITLE)));
                p.setLatitude(c.getDouble(c.getColumnIndexOrThrow(COL_LAT)));
                p.setLongitude(c.getDouble(c.getColumnIndexOrThrow(COL_LON)));
                p.setTimestamp(c.getLong(c.getColumnIndexOrThrow(COL_TIME)));
                p.setAmplitudeDb(c.getFloat(c.getColumnIndexOrThrow(COL_AMP)));
                p.setPhase(c.getFloat(c.getColumnIndexOrThrow(COL_PHASE)));
                p.setIValue(c.getFloat(c.getColumnIndexOrThrow(COL_I)));
                p.setQValue(c.getFloat(c.getColumnIndexOrThrow(COL_Q)));
                p.setRxLevel(c.getFloat(c.getColumnIndexOrThrow(COL_RX)));
                list.add(p);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public void deletePlace(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }
}
