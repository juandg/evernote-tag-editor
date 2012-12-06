package com.evernote.android.sample.tageditor.data;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.evernote.android.sample.tageditor.R;

/**
 * Custom CursorAdapter for the main list of Tags
 * 
 * @author Juan Gomez
 * @version 1.0.0
 * @since December 3, 2012
 * 
 */
public class TagListAdapter extends CursorAdapter {

	private static final int TAG_WITH_CHILDREN = 1;
	private static final int TOP_LEVEL_TAG = 0;
	private LayoutInflater mInflater;

	public TagListAdapter(Context context, Cursor c) {
		super(context, c, false);
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public void bindView(View convertView, Context context, Cursor cursor) {
		// convertView contains an already inflated layout for the row
		TextView tag_name = (TextView) convertView.findViewById(R.id.tag_name);
		// There's a view in this layout that draws a separation line if there's
		// child tags
		View separator = (View) convertView.findViewById(R.id.separator);
		// We query the database for children of this tag
		Cursor childCursor = TagsDb.INSTANCE.getChildTags(cursor.getString(cursor
				.getColumnIndex(DatabaseHelper.COLUMN_GUID)));

		if(childCursor != null) {
			if(childCursor.getCount() > 0) {
				// If we find children we need to make the separator visible 
				separator.setVisibility(View.VISIBLE);
				// and modify the tag_children TextView to dispaly the number of children
				TextView tag_children = (TextView) convertView.findViewById(R.id.tag_children);
				// the text changes depending on the number of children
				switch(childCursor.getCount()) {
				case 1:
					// case for just one children
					childCursor.moveToFirst();
					tag_children.setText("1 Tag:" + childCursor.getString(cursor
							.getColumnIndex(DatabaseHelper.COLUMN_NAME)));
					break;
				default:
					// case for multiple children
					StringBuilder sb = new StringBuilder();
					sb.append(childCursor.getCount());
					sb.append(" Tags: ");
					while(childCursor.moveToNext()) {
						sb.append(childCursor.getString(cursor
								.getColumnIndex(DatabaseHelper.COLUMN_NAME)));
						// we need to add a comma to all elements but the last one
						if(!cursor.isLast())
							sb.append(", ");
					}
					tag_children.setText(sb.toString());
					break;
				}
			}
			// we don't need the childCursor anymore so it's safe to close it
			childCursor.close();
		}
		tag_name.setText(cursor.getString(cursor
				.getColumnIndex(DatabaseHelper.COLUMN_NAME)));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		// on each call of new view, we inflate the row layout.
		return mInflater.inflate(R.layout.tag_list_item, parent, false);
	}

	/**
	 * We need to override {@link CursorAdapter#getItemViewType(int)} 
	 * for the Drop Down list to display correctly.
	 * 
	 */
	@Override
	public int getItemViewType(int position) {
		Cursor cursor = getCursor();
		cursor.moveToPosition(position);
		Cursor childCursor = TagsDb.INSTANCE.getChildTags(cursor.getString(cursor
				.getColumnIndex(DatabaseHelper.COLUMN_GUID)));
		return (childCursor != null && childCursor.getCount() > 0) ? TAG_WITH_CHILDREN : TOP_LEVEL_TAG;
	}

	/**
	 * We need to override {@link CursorAdapter#getViewTypeCount()} 
	 * for the Drop Down list to display correctly.
	 * 
	 */
	@Override
	public int getViewTypeCount() {
		// we have only two types of rows. tags with children and tags without
		// so this method always returns 2
		return 2;
	}

}