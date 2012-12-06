package com.evernote.android.sample.tageditor.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.evernote.android.sample.tageditor.R;
import com.evernote.android.sample.tageditor.data.TagsDb;
import com.evernote.android.sample.tageditor.service.TagSyncService;
import com.evernote.android.sample.tageditor.service.TagSyncService.Task;
import com.evernote.android.sample.tageditor.utils.TagEditorUtil;
import com.evernote.client.oauth.android.EvernoteSession;

/**
 * Main activity of the Evernote Tag Editor app
 * 
 * <p>We're using a library called ActionBarSherlock and the Android 
 * Compatibility Library v4 to be able to use the Action Bar and Fragments, but 
 * still be compatible with devices running Android 2.2 and 2.3 which represent 
 * the bulk (<60%) of Android devices currently on the market</p>
 * 
 * @author Juan Gomez
 * @version 1.0.0
 * @since December 2, 2012
 * 
 */
public class TagEditorActivity extends Activity implements
ActionBar.OnNavigationListener {

	// Name of this application, for logging
	private static final String TAG = "TagEditorActivity";

	// Used to interact with the Evernote web service
	private EvernoteSession mEvernoteSession;

	private BroadcastReceiver actionReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			TagListFragment fragment = (TagListFragment) getFragmentManager().findFragmentByTag(LIST_FRAGMENT_TAG);
			if(fragment == null) {
				startListFragment();
			} else {
				fragment.notifyDataSetChanged();
			}
			Task currentTask = (Task) intent.getExtras().get(TagSyncService.EXTRA_CURRENT_TASK);
			String action = intent.getAction();
			String toastText = "";
			switch(currentTask) {
			case CREATE:
				toastText  = (action.equalsIgnoreCase(TagSyncService.ACTION_COMPLETED)) ? 
						"Tag created successfully" : "Tag creation failed";
				break;
			case DELETE:
				toastText  = (action.equalsIgnoreCase(TagSyncService.ACTION_COMPLETED)) ? 
						"Tag deleted successfully" : "Tag deletion failed";
				break;
			case SYNC:
				setProgressBarIndeterminateVisibility(false); 
				break;
			case UPDATE:
				toastText  = (action.equalsIgnoreCase(TagSyncService.ACTION_COMPLETED)) ? 
						"Tag updated successfully" : "Tag update failed";
				break;
			default:
				break;

			}
			if(!toastText.equalsIgnoreCase(""))
				Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show();
		}
	};

	public static final String LIST_FRAGMENT_TAG = "tagList";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_tag_editor);
		setProgressBarIndeterminateVisibility(false); 
		setupSession();	
	}

	private void startListFragment() {
		Fragment fragment = new TagListFragment();
		getFragmentManager().beginTransaction()
		.replace(R.id.container, fragment, LIST_FRAGMENT_TAG ).commit();
	}

	@Override
	public void onResume() {
		super.onResume();
		TagsDb.INSTANCE.open(getApplicationContext());
		registerReceivers();
		if (!mEvernoteSession.isLoggedIn()) {
			startAuth();
		} else {
			startListFragment();
			updateUi();
		}	
	}

	private void registerReceivers() {
		IntentFilter if_completed = new IntentFilter(TagSyncService.ACTION_COMPLETED);
		IntentFilter if_failed = new IntentFilter(TagSyncService.ACTION_FAILED);
		registerReceiver(actionReceiver, if_completed);
		registerReceiver(actionReceiver, if_failed);
	}


	@Override
	protected void onPause() {
		TagsDb.INSTANCE.close();
		unregisterReceiver(actionReceiver);
		super.onPause();
	}

	/**
	 * Setup the EvernoteSession used to access the Evernote API.
	 */
	private void setupSession() {
		// Retrieve persisted authentication information
		mEvernoteSession = EvernoteSession.init(getApplicationContext(), TagEditorUtil.CONSUMER_KEY, TagEditorUtil.CONSUMER_SECRET, TagEditorUtil.EVERNOTE_HOST, null);
	}

	/**
	 * Update the UI based on Evernote authentication state.
	 */
	private void updateUi() {
		if (mEvernoteSession.isLoggedIn()) {
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			String saved_username = "";
			if(sp.contains("username")) {
				saved_username  = sp.getString("username", "");
			}

			if (saved_username.equalsIgnoreCase("")) {
				new EvernoteSyncUser().execute();
			} else {
				initializeActionBar(saved_username);
			}
		}
	}

	private void initializeActionBar(String saved_username) {
		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
				// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(actionBar.getThemedContext(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, new String[] {
					saved_username,
				"Sign Out"}), TagEditorActivity.this);
	}

	/**
	 * Called when the user taps the "Log in to Evernote" button.
	 * Initiates the Evernote OAuth process, or logs out if the user is already
	 * logged in.
	 */
	public void startAuth() {
		if (mEvernoteSession.isLoggedIn())
			logOut();
		mEvernoteSession.authenticate(this.getApplicationContext());
	}

	public void logOut() {
		mEvernoteSession.logOut(getApplicationContext());
		Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
		edit.putString("username", "");
		edit.commit();	
	}

	/**
	 * Called when the control returns from an activity that we launched.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
		//Update UI when oauth activity returns result
		case EvernoteSession.REQUEST_CODE_OAUTH:
			if(resultCode == Activity.RESULT_OK) {
				updateUi();
			}
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_tag_editor, menu);
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			TagListFragment listFragment = (TagListFragment) getFragmentManager().findFragmentByTag(TagEditorActivity.LIST_FRAGMENT_TAG);
			if(!listFragment.isTopLevelList()) {
				listFragment.onHeaderClick();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.addTag:
			AddEditTagDialogFragment newFragment = new AddEditTagDialogFragment();
			Bundle argBundle = new Bundle();
			argBundle.putBoolean(AddEditTagDialogFragment.EDIT_BUNDLE_KEY, false);
			TagListFragment listFragment = (TagListFragment) getFragmentManager().findFragmentByTag(TagEditorActivity.LIST_FRAGMENT_TAG);
			if(!listFragment.isTopLevelList())
				argBundle.putString(AddEditTagDialogFragment.PARENT_GUID_BUNDLE_KEY, listFragment.getCurrentTagGuid());
			newFragment.setArguments(argBundle);
			newFragment.show(getFragmentManager(), "add_edit_tag");
			break;
		case R.id.update:
			Intent i = new Intent(TagEditorActivity.this, TagSyncService.class);
			i.putExtra(TagSyncService.EXTRA_CURRENT_TASK, TagSyncService.Task.SYNC);
			setProgressBarIndeterminateVisibility(true); 
			startService(i);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private class EvernoteSyncUser extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true); 
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			String username = "";
			String saved_username = "";
			try {

				username = mEvernoteSession.createUserStore().getUser(mEvernoteSession.getAuthToken()).getUsername();
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				if(sp.contains("username")) {
					saved_username = sp.getString("username", "");
					if(saved_username.equalsIgnoreCase(username))
						return false;
				}

				Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
				edit.putString("username", username);
				edit.commit();	

				TagsDb.INSTANCE.cleanDb();

				Intent i = new Intent(TagEditorActivity.this, TagSyncService.class);
				i.putExtra(TagSyncService.EXTRA_CURRENT_TASK, TagSyncService.Task.SYNC);
				startService(i);

			} catch(Exception e) {
				Log.e(TAG, "Can't get username", e);
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean triggeredSync) {
			setProgressBarIndeterminateVisibility(false); 
			if(triggeredSync)
				setProgressBarIndeterminateVisibility(true); 
			updateUi();
		}
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		if(position == 1) {
			if (mEvernoteSession.isLoggedIn()) {
				logOut();
			}
			startAuth();
		}
		return true;
	}
}
