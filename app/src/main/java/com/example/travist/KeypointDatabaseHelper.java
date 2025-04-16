package com.example.travist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class KeypointDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "travist.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE = "keypoints";
    public static final String ID = "id";
    public static final String KP_NAME = "keypoint_name";
    public static final String KP_PRICE = "key_point_price";
    public static final String START_DATE = "key_point_start_date";
    public static final String END_DATE = "key_point_end_date";
    public static final String KP_COVER = "key_point_cover";
    public static final String KP_X = "key_point_gps_x";
    public static final String KP_Y = "key_point_gps_y";
    public static final String IS_ALTERED = "is_altered_keypoint";
    public static final String CITY_ID = "city_id";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE + " (" +
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KP_NAME + " TEXT, " +
                    KP_PRICE + " REAL, " +
                    START_DATE + " TEXT, " +
                    END_DATE + " TEXT, " +
                    KP_COVER + " LONGTEXT, " +
                    KP_X + " REAL, " +
                    KP_Y + " REAL, " +
                    IS_ALTERED + " BOOLEAN, " +
                    CITY_ID + " INTEGER" +
                    ");";

    public KeypointDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public void insertKeypoint(String kp_name, float kp_price, String start_date, String end_date, String kp_cover,
                               float kp_x, float kp_y, boolean is_altered, int city_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KP_NAME, kp_name);
        values.put(KP_PRICE, kp_price);
        values.put(START_DATE, start_date);
        values.put(END_DATE, end_date);
        values.put(KP_COVER, kp_cover);
        values.put(KP_X, kp_x);
        values.put(KP_Y, kp_y);
        values.put(IS_ALTERED, is_altered);
        values.put(CITY_ID, city_id);
        db.insert(TABLE, null, values);
        db.close();
    }

    public Cursor getAllKeypoints() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE, null);
    }
}

