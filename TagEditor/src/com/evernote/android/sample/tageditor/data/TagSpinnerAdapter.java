package com.evernote.android.sample.tageditor.data;


import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import com.evernote.edam.type.Tag;

/**
 * Custom ArrayAdapter for the Spinner widget on the Add/Edit Dialog Fragment
 * <p> We needed to create a custom a ArrayAdapter since we're using a list of 
 * Tag objects instead of plain strings. </p>
 * 
 * @author Juan Gomez
 * @version 1.0.0
 * @since December 4, 2012
 * 
 */
public class TagSpinnerAdapter extends ArrayAdapter<Tag> {

	public TagSpinnerAdapter(Context context, int textViewResourceId, List<Tag> objects) {
		super(context, textViewResourceId, objects);
	}

	/**
	 * We need to override {@link ArrayAdapter#getDropDownView(int,View,ViewGroup)} 
	 * for the Drop Down list to show correctly.
	 * 
	 */
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		// we get the View form the parent's implementation
		View v = super.getDropDownView(position, convertView, parent);
		// Look for the CheckedTextView on the view
		CheckedTextView tag_name = (CheckedTextView) v.findViewById(android.R.id.text1);
		// And make the text of the CheckedTextView be equal to the Tag name
		tag_name.setText(getItem(position).getName());
		// finally returned the modified view.
		return v;
	}

	/**
	 * We also need to override {@link ArrayAdapter#getView(int,View,ViewGroup)} 
	 * for the Spinner to display correctly when collapsed.
	 * 
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// same process as getDropDownView(...)
		View v = super.getView(position, convertView, parent);
		// Here we have a TextView instead of a CheckedTextView since the 
		// ArrayAdapter uses different layouts for the collapsed Spinner 
		// and the drop down list.
		TextView tag_name = (TextView) v.findViewById(android.R.id.text1);
		tag_name.setText(getItem(position).getName());
		return v;
	}
}