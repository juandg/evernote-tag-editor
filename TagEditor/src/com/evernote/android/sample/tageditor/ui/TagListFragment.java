package com.evernote.android.sample.tageditor.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.evernote.android.sample.tageditor.R;
import com.evernote.android.sample.tageditor.data.DatabaseHelper;
import com.evernote.android.sample.tageditor.data.TagListAdapter;
import com.evernote.android.sample.tageditor.data.TagsDb;
import com.evernote.android.sample.tageditor.service.TagSyncService;
import com.evernote.edam.type.Tag;

/**
 * List Fragment that displays the list of Tags
 * 
 * <p>We extend a ListFragment (the ActionBarSherlock version) that gives us 
 * all the functionality we need to interact with the listview and its 
 * corresponding adapter, for free</p>
 * 
 * @author Juan Gomez
 * @version 1.0.0
 * @since December 3, 2012
 * 
 */
public class TagListFragment extends ListFragment {

	private Cursor cursor;
	private String currentTagGuid;
	private boolean isTopLevel;
	private LinearLayout header;

	@Override
	public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_tag_list, null);
		header = (LinearLayout) view.findViewById(R.id.header);
		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// When the fragment gets created we load the list of tags
		isTopLevel = true;
		new TagLoaderTask().execute();
	}

	@Override
	public void onPause() {
		if(cursor != null && !cursor.isClosed())
			cursor.close();
		super.onPause();
	}

	public String getCurrentTagGuid() {
		return currentTagGuid;
	}

	public boolean isTopLevelList() {
		return isTopLevel;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		cursor.moveToPosition(position);
		final String selectedTagGuid = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_GUID));
		final Cursor childCursor = TagsDb.INSTANCE.getChildTags(selectedTagGuid);
		boolean hasChildren = false;
		if(childCursor != null) {
			if(childCursor.getCount() > 0) {
				hasChildren = true;
			}
		}
		DialogFragment newFragment = new ModifyTagDialogFragment();
		Bundle argBundle = new Bundle();
		argBundle.putString(AddEditTagDialogFragment.NAME_BUNDLE_KEY, cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME)));
		argBundle.putString(AddEditTagDialogFragment.GUID_BUNDLE_KEY, selectedTagGuid);
		argBundle.putString(AddEditTagDialogFragment.PARENT_GUID_BUNDLE_KEY, cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PARENT_GUID)));
		argBundle.putBoolean(ModifyTagDialogFragment.HAS_CHILDREN_BUNDLE_KEY, hasChildren);

		newFragment.setArguments(argBundle);
		newFragment.show(getFragmentManager(), "TagAction");
	}

	public void notifyDataSetChanged() {
		new TagLoaderTask().execute();
	}

	public void onHeaderClick() {
		Tag currentTag = TagsDb.INSTANCE.getTagByGuid(currentTagGuid);
		TextView title = (TextView) header.findViewById(R.id.name_title);
		if(currentTag.getParentGuid() != null) {
			currentTagGuid = currentTag.getParentGuid();
			Tag parentTag = TagsDb.INSTANCE.getTagByGuid(currentTagGuid);
			title.setText("Tags under " + parentTag.getName());
		} else {
			isTopLevel = true;
			currentTagGuid =  "";
			header.setVisibility(View.GONE);
			header.setOnClickListener(null);
			title.setText("");
		}
		new TagLoaderTask().execute();
	}

	private class TagLoaderTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			setListAdapter(null);
		}

		@Override
		protected Void doInBackground(Void... v) {
			if(cursor != null) {
				cursor.close();
				cursor = null;
			}
			TagsDb.INSTANCE.open(getActivity().getApplicationContext());
			if(isTopLevel) {
				cursor = TagsDb.INSTANCE.getTopLevelTags();
			} else {
				cursor = TagsDb.INSTANCE.getChildTags(TagListFragment.this.currentTagGuid);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void v) {
			if(cursor != null && !cursor.isClosed() && cursor.getCount() > 0) {
				setListAdapter(new TagListAdapter(TagListFragment.this.getActivity(), cursor));
			}
		}
	}

	public static class ModifyTagDialogFragment extends DialogFragment {

		public static final String HAS_CHILDREN_BUNDLE_KEY = "hasChildren";

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			final Bundle argBundle = getArguments();
			StringBuilder options = new StringBuilder();
			options.append("Edit Tag");
			options.append(",Delete Tag");
			if(argBundle.getBoolean(HAS_CHILDREN_BUNDLE_KEY))
				options.append(",View Tags");
			builder.setTitle("Select an action")
			.setItems(options.toString().split(","), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {

					switch(which) {
					case 0:
						AddEditTagDialogFragment newFragment = new AddEditTagDialogFragment();
						argBundle.putBoolean(AddEditTagDialogFragment.EDIT_BUNDLE_KEY, true);
						newFragment.setArguments(argBundle);
						dismiss();
						newFragment.show(getFragmentManager(), "add_edit_tag");
						break;
					case 1:
						dismiss();
						Intent i = new Intent(getActivity().getApplicationContext(), TagSyncService.class);
						i.putExtra(TagSyncService.EXTRA_CURRENT_TASK, TagSyncService.Task.DELETE);
						i.putExtra(TagSyncService.EXTRA_TAG_GUID, argBundle.getString(AddEditTagDialogFragment.GUID_BUNDLE_KEY));
						getActivity().startService(i);
						break;
					case 2:
						final TagListFragment listFragment = (TagListFragment) getFragmentManager().findFragmentByTag(TagEditorActivity.LIST_FRAGMENT_TAG);
						listFragment.isTopLevel = false;
						listFragment.currentTagGuid = argBundle.getString(AddEditTagDialogFragment.GUID_BUNDLE_KEY);
						listFragment.header.setVisibility(View.VISIBLE);
						listFragment.header.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								listFragment.onHeaderClick();
							}
						});
						TextView title = (TextView) listFragment.header.findViewById(R.id.name_title);
						title.setText("Tags under " + argBundle.getString(AddEditTagDialogFragment.NAME_BUNDLE_KEY));
						listFragment.notifyDataSetChanged();
					}
				}
			});
			return builder.create();
		}
	}
}