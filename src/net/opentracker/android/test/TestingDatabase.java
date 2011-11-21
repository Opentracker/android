package net.opentracker.android.test;

import java.util.Locale;

import net.opentracker.example.R;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

public class TestingDatabase extends Activity {
    /** Called when the activity is first created. */
    SQLiteDatabase db;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        db =
                openOrCreateDatabase("TestingData.db",
                        SQLiteDatabase.CREATE_IF_NECESSARY, null);
        db.setVersion(2);
        db.setLocale(Locale.getDefault());
        db.setLockingEnabled(true);

        final String CREATE_TABLE_SESSION =
                "CREATE TABLE sessions ("
                        + "session_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "startUnixTimestamp TEXT"
                        + "totalViewCount INTEGER );";
        db.execSQL(CREATE_TABLE_SESSION);

    }

    public void insertRow() {
        ContentValues stateValues = new ContentValues();
        stateValues.put("startUnixTimestamp", Long.toString(System
                .currentTimeMillis()));
        stateValues.put("totalViewCount", 0);
        try {
            db.insertOrThrow("sessions", null, stateValues);
        } catch (Exception e) {
            // catch code
        }
    }

    public Cursor getLastInsertedRow() {
        String selectQuery = "select * from sessions limit 1";
        Cursor cursor = db.rawQuery(selectQuery, null);
        startManagingCursor(cursor);
        return cursor;
    }
}
