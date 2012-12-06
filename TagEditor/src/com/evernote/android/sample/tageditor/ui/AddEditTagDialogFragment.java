package com.evernote.android.sample.tageditor.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.Spinner;

import com.evernote.android.sample.tageditor.R;
import com.evernote.android.sample.tageditor.data.TagSpinnerAdapter;
import com.evernote.android.sample.tageditor.data.TagsDb;
import com.evernote.android.sample.tageditor.service.TagSyncService;
import com.evernote.edam.type.Tag;

/**
 * Dialog Fragment for the Add/Edit dialog
 * 
 * @author Juan Gomez
 * @version 1.0.0
 * @since December 4, 2012
 * 
 */
public class AddEditTagDialogFragment extends DialogFragment implements OnItemSelectedListener {

	// Constants to name the expected Fragment arguments
	public static final String NAME_BUNDLE_KEY = "name";
	public static final String GUID_BUNDLE_KEY = "guid";
	public static final String PARENT_GUID_BUNDLE_KEY = "parentGuid";
	public static final String EDIT_BUNDLE_KEY = "isEdit";

	// Fields to hold the info of the Edited tag
	private String name;
	private String guid;
	private String currentParentGuid;
	private boolean isEdit = false;
	private String newParentGuid = "";

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// When using fragments we need to use the setArguments/getArguments methods
		// instead of passing parameters on the constructor. Fragments need to have
		// just a default constructor.
		Bundle argBundle = getArguments();
		// check if we have a parent guid to set the Spinner to
		if(argBundle.containsKey(PARENT_GUID_BUNDLE_KEY))
			this.currentParentGuid = argBundle.getString(PARENT_GUID_BUNDLE_KEY);

		// check if we're editing a Tag, if we are, we get all the extra information from the arguments bundle
		if(argBundle.getBoolean(EDIT_BUNDLE_KEY)) {
			this.isEdit = argBundle.getBoolean(EDIT_BUNDLE_KEY);
			this.name = argBundle.getString(NAME_BUNDLE_KEY);
			this.guid = argBundle.getString(GUID_BUNDLE_KEY);
		}

		// Start building our AlertDialog
		AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();
		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout
		final View v = inflater.inflate(R.layout.add_edit, null);
		// get the spinner widget
		Spinner parent = (Spinner) v.findViewById(R.id.parent);
		// Determined if this tag has a parent tag
		String parent_guid = (this.currentParentGuid != null) ? this.currentParentGuid : "";
		// if we're editing we need to dipaly the name of the tag on the edit field
		// otherwise is balnk
		String tagName = (isEdit) ? name : "";
		EditText nameEditText = (EditText) v.findViewById(R.id.name);
		nameEditText.setText(tagName);

		// Create an ArrayAdapter using the a Tag list we get form the DB and 
		// a default spinner layout
		TagSpinnerAdapter adapter = new TagSpinnerAdapter(getActivity(), android.R.layout.simple_spinner_item, TagsDb.INSTANCE.getTagListForSpinner(guid , parent_guid));
		// Specify the layout to use when the drop down list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		parent.setAdapter(adapter);
		// register our click listener
		parent.setOnItemSelectedListener(this);

		int title = (isEdit) ? R.string.edit_title : R.string.add_title;
		builder.setTitle(title).setView(v)
		// set a listener for when the use saves changes
		.setPositiveButton(R.string.ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				EditText nameEditText = (EditText) v.findViewById(R.id.name);
				// we generate and intent to send to our sync service
				Intent i = new Intent(getActivity(), TagSyncService.class);
				if(AddEditTagDialogFragment.this.isEdit) {
					// set the current task to "update"
					i.putExtra(TagSyncService.EXTRA_CURRENT_TASK, TagSyncService.Task.UPDATE);
					// add the guid of the tag we're updating
					i.putExtra(TagSyncService.EXTRA_TAG_GUID, AddEditTagDialogFragment.this.guid);
				} else {
					// set the current task to "create"
					i.putExtra(TagSyncService.EXTRA_CURRENT_TASK, TagSyncService.Task.CREATE);
				}
				// add the tag name
				i.putExtra(TagSyncService.EXTRA_TAG_NAME, nameEditText.getText().toString());
				// add the tag parent guid
				i.putExtra(TagSyncService.EXTRA_TAG_PARENT, (AddEditTagDialogFragment.this.newParentGuid));

				// we ask our parent activity to call the sync service with the intent we created
				getActivity().startService(i);

			}
		})
		.setNegativeButton(R.string.cancel, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// if the user cancels, we simply dismiss the dialog
				dialog.dismiss();
			}
		});
		return builder.create();		
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, 
			int pos, long id) {
		// Determine if a parent tag was selected
		Tag t = (Tag) parent.getItemAtPosition(pos);
		if (t != null) {
			// if it was, set it as our new parent tag
			this.newParentGuid = (t.getName() != TagsDb.EMPTY_TAG_LIST_ITEM) ? t.getGuid(): t.getName();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// We're forced to override this method, but there's nothing to do here
	}
}