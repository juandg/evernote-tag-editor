package com.evernote.android.sample.tageditor.data;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.evernote.edam.type.Tag;

/**
 * This class encapsulates all Database operations.
 * <p> it needs to be a singleton so that we can call it both from our ListFragment
 * and our service, avoiding any threading issues</p>
 * 
 * @author Juan Gomez
 * @version 1.0.0
 * @since December 2, 2012
 * 
 */

// This is the recomended way to create a singleton in Java 1.5 and above
public enum TagsDb {
	INSTANCE;

	public static final String EMPTY_TAG_LIST_ITEM = "(none)";

	// TAG for logging
	public static final String TAG = "TagsDb";

	// Database fields
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;

	// These are the Tag rows that we will retrieve
	static final String[] PROJECTION = { DatabaseHelper.COLUMN_ID,
		DatabaseHelper.COLUMN_GUID, DatabaseHelper.COLUMN_NAME,
		DatabaseHelper.COLUMN_PARENT_GUID,
		DatabaseHelper.COLUMN_UPDATE_SEQ_NUM };

	// This is the select criteria for all the top level tags (childless tags)
	static final String SELECTION_TOP_LEVEL = "(" + DatabaseHelper.COLUMN_PARENT_GUID + " IS NULL)";

	// This is the select criteria for an specific tag
	static final String SELECTION = "(" + DatabaseHelper.COLUMN_GUID + " = ?)";

	// This is the select criteria to obtain all the children of a tag
	static final String SELECTION_HAS_CHILDREN = "(" + DatabaseHelper.COLUMN_PARENT_GUID + " = ?)";

	// This is the sorting criteria
	static final String sortOrder = DatabaseHelper.COLUMN_NAME + " COLLATE LOCALIZED ASC";

	/**
	 * Opens the database connection to the Local Tags DB.
	 * 
	 */
	public void open(Context context) throws SQLException {
		if (dbHelper == null)
			dbHelper = new DatabaseHelper(context);
		database = dbHelper.getWritableDatabase();
	}

	/**
	 * Calls {@link DatabaseHelper#close()} which in turn closes the database connection.
	 * 
	 */
	public void close() {
		if (dbHelper != null)
			dbHelper.close();
	}

	/**
	 * Obtain a cursor with all the top level (childless) tags.
	 * @returns {@link Cursor}
	 * 
	 */
	public Cursor getTopLevelTags() {
		//  This method is used by the ListFragment to display the first list of tags
		Cursor topLevel = database.query(DatabaseHelper.TABLE_TAGS, PROJECTION,
				SELECTION_TOP_LEVEL, null, null, null, sortOrder);

		return topLevel;

	}

	/**
	 * Obtain a cursor with all the children of a specific Parent tag.
	 * @param	guid	A string representing the Guid of the Parent Tag.
	 * @returns {@link Cursor}
	 * 
	 */
	public Cursor getChildTags(String guid) {
		// Get a cursor with all the children of a tag. This is used to determine the 
		// row type and to display a tag's children
		Cursor topLevel = database.query(DatabaseHelper.TABLE_TAGS, PROJECTION,
				SELECTION_HAS_CHILDREN, new String[] { guid }, null, null, sortOrder);

		return topLevel;

	}

	/**
	 * Obtain a {@link Tag} by its Guid.
	 * @param	guid	A string representing the Guid of the requested Tag.
	 * @returns {@link Tag}
	 * 
	 */
	public Tag getTagByGuid(String guid) {
		// Get an specific tag by Guid
		Tag tag = null;
		Cursor cursor = database.query(DatabaseHelper.TABLE_TAGS, PROJECTION,
				SELECTION, new String[] { guid }, null, null, null);
		if (cursor != null && cursor.moveToNext()) {
			tag = new Tag();
			tag.setName(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME)));
			tag.setParentGuid(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PARENT_GUID)));
			tag.setGuid(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_GUID)));
		}
		return tag;
	}

	/**
	 * Obtain a list of all the possible parent Tags to display on the spinner widget of the Add/Edit Fragment
	 * @param	guid	A string representing the Guid of the Tag being modified.
	 * @param	parentGuid	A string representing the Guid of the current parent Tag 
	 * (This tag gets returned first on the list).
	 * @returns A {@link List} of {@link Tag} objects representing possible parent tags.
	 * 
	 */
	public List<Tag> getTagListForSpinner(String guid, String parentGuid) {

		List<Tag> all = new ArrayList<Tag>();
		String temp_guid = "";
		Tag temp_tag = new Tag();
		Tag emptyTag = new Tag();
		emptyTag.setName(EMPTY_TAG_LIST_ITEM);
		Tag parentTag = new Tag();


		Cursor cursor = database.query(DatabaseHelper.TABLE_TAGS, PROJECTION,
				null /* null on the select criteria to get all the tags*/, 
				null, null, null, sortOrder);

		// We turn the cursor into a list of all the tags, but with the parent tag on 
		// top (for the spinner) and minus the calling tag to avoid a circular reference
		while (cursor.moveToNext()) {
			temp_tag = new Tag();
			temp_guid = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_GUID));
			// If the tag is the calling tag we don't include it
			if(temp_guid.equalsIgnoreCase(guid))
				continue;
			temp_tag.setGuid(temp_guid);
			temp_tag.setName(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME)));
			temp_tag.setParentGuid(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PARENT_GUID)));
			//if the tag is the parent tag, we hold a reference to it but don't include it yet
			if (parentGuid != null && parentGuid.equalsIgnoreCase(temp_guid))
				parentTag = temp_tag;
			else
				all.add(temp_tag);
		}

		// if a parent guid was specified we add that tag first so that it 
		// displays first on the spinner
		if ("".equalsIgnoreCase(parentGuid)) {
			all.add(0, emptyTag);
		} else {
			all.add(0, parentTag);
			all.add(1, emptyTag);
		}
		// Close the cursor and return the List
		cursor.close();
		return all;
	}

	/**
	 * Store a list of tags obtained form the Evernote server, on the local database.
	 * @param	tags	A {@link List} of {@link Tag} objects to be inserted on the local database. 
	 * @returns {@link true} if the operation succeeded.
	 * 
	 */
	public boolean storeRemoteTags(List<Tag> tags) {

		boolean success = true;
		if (database.isOpen()) {
			database.beginTransaction();
			try {
				// It's easier to just clean the database and write everything, than 
				// trying to perform a "true" sync.
				cleanDb();
				for (Tag tag : tags) {
					ContentValues values = new ContentValues();
					values.put(DatabaseHelper.COLUMN_GUID, tag.getGuid());
					values.put(DatabaseHelper.COLUMN_NAME, tag.getName());
					values.put(DatabaseHelper.COLUMN_PARENT_GUID, tag.getParentGuid());
					values.put(DatabaseHelper.COLUMN_UPDATE_SEQ_NUM, tag.getUpdateSequenceNum());
					database.insertOrThrow(DatabaseHelper.TABLE_TAGS, null,values);
				}
				database.setTransactionSuccessful();

			} catch (Exception e) {
				Log.e(TAG, "Couldn't create Tag database", e);
				success = false;
			} finally {
				database.endTransaction();
			}
		}
		return success;
	}

	/**
	 * Insert a newly created Tag, after we got a Guid from the Evernote service
	 * @param	tag	The {@link Tag} object to be inserted on the local database. 
	 * 
	 */
	public void insertTag(Tag tag) {
		// 
		database.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.COLUMN_GUID, tag.getGuid());
			values.put(DatabaseHelper.COLUMN_NAME, tag.getName());
			values.put(DatabaseHelper.COLUMN_PARENT_GUID, tag.getParentGuid());
			values.put(DatabaseHelper.COLUMN_UPDATE_SEQ_NUM,
					tag.getUpdateSequenceNum());
			database.insertOrThrow(DatabaseHelper.TABLE_TAGS, null, values);

			database.setTransactionSuccessful();

		} catch (Exception e) {
			Log.e(TAG, "Couldn't insert Tag", e);
		} finally {
			database.endTransaction();
		}
	}

	/**
	 * Delete a tag from the local DB.
	 * @param	guid	a string representing the Guid of the Tag to be deleted.
	 * 
	 */
	public void deleteTag(String guid) {
		// 
		database.beginTransaction();
		try {
			database.delete(DatabaseHelper.TABLE_TAGS, SELECTION, new String[] { guid });
			database.setTransactionSuccessful();
		} catch (Exception e) {
			Log.e(TAG, "Couldn't delete Tag", e);
		} finally {
			database.endTransaction();
		}
	}

	/**
	 * Update a tag in the local db, after we received an UpdateSequenceNum from the Evernote server
	 * @param	tag	a {@link Tag} object containing the information to be updated 
	 * 
	 */
	public void updateTag(Tag tag) {
		database.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.COLUMN_GUID, tag.getGuid());
			values.put(DatabaseHelper.COLUMN_NAME, tag.getName());
			values.put(DatabaseHelper.COLUMN_PARENT_GUID, tag.getParentGuid());
			values.put(DatabaseHelper.COLUMN_UPDATE_SEQ_NUM, tag.getUpdateSequenceNum());
			database.update(DatabaseHelper.TABLE_TAGS, values, SELECTION, new String[] { tag.getGuid() });
			database.setTransactionSuccessful();

		} catch (Exception e) {
			Log.e(TAG, "Couldn't update Tag", e);
		} finally {
			database.endTransaction();
		}
	}

	/**
	 * Wipes out the entire Tags table.
	 * 
	 */
	public void cleanDb() {
		database.beginTransaction();
		try {
			database.delete(DatabaseHelper.TABLE_TAGS, 
					null /* null on the where criteria to delete all rows*/, 
					null);
			database.setTransactionSuccessful();
		} catch (Exception e) {
			Log.e(TAG, "Failed to clean the Tags table", e);
		} finally {
			database.endTransaction();
		}
	}
}
