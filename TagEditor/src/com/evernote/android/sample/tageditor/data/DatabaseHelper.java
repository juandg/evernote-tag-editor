package com.evernote.android.sample.tageditor.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Database helper class
 * 
 * @author Juan Gomez
 * @version 1.0.0
 * @since December 2, 2012
 * 
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	public static final String TAG = "DatabaseHelper";

	public static final String TABLE_TAGS = "tags";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_GUID = "guid";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_PARENT_GUID = "parentGuid";
	public static final String COLUMN_UPDATE_SEQ_NUM = "updateSequenceNum";

	private static final String DATABASE_NAME = "tags.db";
	private static final int DATABASE_VERSION = 1;

	// Database creation sql statement
	private static final String DATABASE_CREATE = "create table "
			+ TABLE_TAGS + "(" + COLUMN_ID
			+ " integer primary key autoincrement, " + COLUMN_GUID
			+ " text not null, " + COLUMN_NAME
			+ " text not null, " + COLUMN_PARENT_GUID
			+ " text, " + COLUMN_UPDATE_SEQ_NUM
			+ " integer not null);";

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}


	@Override
	public void onCreate(SQLiteDatabase database) {
		// on creation of the helper, we create our table
		database.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG,
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_TAGS);
		onCreate(db);
	}
}
