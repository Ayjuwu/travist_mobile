package com.example.travist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TravelDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "travist.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE = "travel";
    public static final String ID = "id";
    public static final String TRAVEL_NAME = "travel_name";
    public static final String PEOPLE_NUMBER = "people_number";
    public static final String INDIVIDUAL_PRICE = "individual_price";
    public static final String TOTAL_PRICE = "total_price";
    public static final String START_DATE = "travel_start_date";
    public static final String END_DATE = "travel_end_date";
    public static final String USER_ID = "user_id";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE + " (" +
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    TRAVEL_NAME + " TEXT, " +
                    PEOPLE_NUMBER + " INTEGER, " +
                    INDIVIDUAL_PRICE + " REAL, " +
                    TOTAL_PRICE + " REAL, " +
                    START_DATE + " TEXT, " +
                    END_DATE + " TEXT, " +
                    USER_ID + " INTEGER" +
                    ");";

    public TravelDatabaseHelper(Context context) {
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

    public void insertTravel(String travel_name, int people_number, float i_price, float t_price,
                             String start_date, String end_date, int user_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TRAVEL_NAME, travel_name);
        values.put(PEOPLE_NUMBER, people_number);
        values.put(INDIVIDUAL_PRICE, i_price);
        values.put(TOTAL_PRICE, t_price);
        values.put(START_DATE, start_date);
        values.put(END_DATE, end_date);
        values.put(USER_ID, user_id);
        db.insert(TABLE, null, values);
        db.close();
    }

    public Cursor getAllTravels() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE, null);
    }
}

