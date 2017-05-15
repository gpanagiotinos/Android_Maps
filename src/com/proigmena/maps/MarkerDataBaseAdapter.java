package com.proigmena.maps;

import com.google.android.gms.maps.model.LatLng;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

public class MarkerDataBaseAdapter {

	static final String DATABASE_NAME = "markers.db";
    static final int DATABASE_VERSION = 1;
    public static final int NAME_COLUMN = 1;
    // TODO: Create public field for each column in your table.
    // SQL Statement to create a new database.
    static final String DATABASE_CREATE = "create table "+"MARKER"+
                                 "( " +"ID"+" integer primary key autoincrement,"+ "TITLE  VARCHAR,DESC text,CAT VARCHAR,LAT REAL NOT NULL,LNG REAL NOT NULL); ";
    // Variable to hold the database instance
    public  SQLiteDatabase db;
    // Context of the application using the database.
    private final Context context;
    // Database open/upgrade helper
    private DataBaseHelper dbHelper;
    /*
     * Constructor
     */
    public  MarkerDataBaseAdapter(Context _context) 
    {
        context = _context;
        dbHelper = new DataBaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    /*
     * Open, Close and get instance
     */
    public  MarkerDataBaseAdapter open() throws SQLException 
    {
        db = dbHelper.getWritableDatabase();
        return this;
    }
    public void close() 
    {
        db.close();
    }
    public  SQLiteDatabase getDatabaseInstance()
    {
        return db;
    }
    
    public void insertMarker(String markerTitle,String markerDesc,String markerCat, LatLng point)
    {
       ContentValues newValues = new ContentValues();
        // Assign values for each row.
        newValues.put("TITLE", markerTitle);
        newValues.put("DESC",markerDesc);
        newValues.put("CAT",markerCat);
        newValues.put("LAT",point.latitude);
        newValues.put("LNG",point.longitude);

        // Insert the row into your table
        db.insert("MARKER", null, newValues);
        Toast.makeText(context, R.string.toast_marker_saved, Toast.LENGTH_LONG).show();
    }
    public Cursor getAllMarkers()
    {
    	String sql = "SELECT * FROM MARKER";
        Cursor cursor = db.rawQuery(sql, null);
        
        return cursor;
    }
    public int deleteMarker(LatLng point) {
    	String where = "LAT = ?" +
    				   "AND LNG = ?";
    	int numberOFEntriesDeleted = db.delete("MARKER", where, new String[]{String.valueOf(point.latitude), String.valueOf(point.longitude)});
        return numberOFEntriesDeleted;
    }
    public int updateMarker(String markerTitle,String markerDesc,String markerCat, LatLng point)
    {
    	ContentValues newValues = new ContentValues();
        // Assign values for each row.
        newValues.put("TITLE", markerTitle);
        newValues.put("DESC", markerDesc);
        newValues.put("CAT", markerCat);

        String where = "LAT = ?" +
				   "AND LNG = ?";
        int numberOFEntriesUpdated = db.update("MARKER", newValues, where, new String[]{String.valueOf(point.latitude), String.valueOf(point.longitude)});    
        return numberOFEntriesUpdated;
    } 
}
