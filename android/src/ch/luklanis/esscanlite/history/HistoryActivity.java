/*
 * Copyright 2012 ZXing authors
 * Copyright 2012 Lukas Landis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.luklanis.esscanlite.history;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;
import ch.luklanis.esscanlite.R;
import ch.luklanis.esscan.paymentslip.DTAFileCreator;
import ch.luklanis.esscanlite.CaptureActivity;
import ch.luklanis.esscanlite.Intents;
import ch.luklanis.esscanlite.PreferencesActivity;

import java.util.List;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.actionbarsherlock.widget.ShareActionProvider.OnShareTargetSelectedListener;

public final class HistoryActivity extends SherlockListActivity {

	private HistoryManager historyManager;
	private HistoryItemAdapter adapter;
	private ShareActionProvider mShareActionProvider;
	private int lastAlertId;
	private CheckBox dontShowAgainCheckBox;
	private DTAFileCreator dtaFileCreator;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// Hide Icon in ActionBar
		getSupportActionBar().setDisplayShowHomeEnabled(false);

		this.historyManager = new HistoryManager(this);  
		adapter = new HistoryItemAdapter(this);
		setListAdapter(adapter);
		ListView listview = getListView();
		registerForContextMenu(listview);

		this.dtaFileCreator = new DTAFileCreator(getApplicationContext());
	}

	@Override
	protected void onResume() {
		super.onResume();
		List<HistoryItem> items = historyManager.buildHistoryItemsForCSV();
		adapter.clear();
		for (HistoryItem item : items) {
			adapter.add(item);
		}
		if (adapter.isEmpty()) {
			adapter.add(new HistoryItem(null));
		}

		int error = dtaFileCreator.getFirstErrorId();

		if(error != 0){
			setOptionalOKAlert(error);
		} else {
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (adapter.getItem(position).getResult() != null) {
			Intent intent = new Intent(this, CaptureActivity.class);
			intent.putExtra(Intents.History.ITEM_NUMBER, position);
			setResult(Activity.RESULT_OK, intent);
			finish();
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu,
			View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
		menu.add(Menu.NONE, position, position, R.string.history_clear_one_history_text);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		int position = item.getItemId();
		historyManager.deleteHistoryItem(position);
		reload();
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (historyManager.hasHistoryItems()) {
			getSupportMenuInflater().inflate(R.menu.history_menu, menu);

			// Locate MenuItem with ShareActionProvider
			MenuItem item = menu.findItem(R.id.history_menu_send_dta);

			if(dtaFileCreator.getFirstErrorId() == 0) {
				// Fetch and store ShareActionProvider
				mShareActionProvider = (ShareActionProvider) item.getActionProvider();
				
				mShareActionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);

				mShareActionProvider.setOnShareTargetSelectedListener(new OnShareTargetSelectedListener() {

					@Override
					public boolean onShareTargetSelected(ShareActionProvider source,
							Intent intent) {						
						if(createDTAFile()) {
							return false;
						} else {
							// Do nothing so we have to return true that says we handled the intent
							return true;
						}
					}
				});

				setShareIntent(createShareIntent());
			} else {
				item.setVisible(false);
			}

			return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.history_menu_send_csv: {
			CharSequence history = historyManager.buildHistory();
			Uri historyFile = HistoryManager.saveHistory(history.toString());

			String[] recipients = new String[]{PreferenceManager.getDefaultSharedPreferences(this)
					.getString(PreferencesActivity.KEY_EMAIL_ADDRESS, "")};

			if (historyFile == null) {
				setOKAlert(R.string.msg_unmount_usb);
			} else {
				Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				intent.putExtra(Intent.EXTRA_EMAIL, recipients);
				String subject = getResources().getString(R.string.history_email_title);
				intent.putExtra(Intent.EXTRA_SUBJECT, subject);
				intent.putExtra(Intent.EXTRA_TEXT, subject);
				intent.putExtra(Intent.EXTRA_STREAM, historyFile);
				intent.setType("text/csv");
				startActivity(intent);
			}
		}
		break;
		case R.id.history_menu_clear: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.msg_sure);
			builder.setCancelable(true);
			builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int i2) {
					historyManager.clearHistory();
					dialog.dismiss();
					finish();
				}
			});
			builder.setNegativeButton(R.string.button_cancel, null);
			builder.show();
		}
		break;
		//		case R.id.history_menu_send_dta: {
		//			Uri dtaFile = getDTAFileUri();
		//			if (dtaFile != null) {
		//				Intent intent = createShareIntent();
		//				intent.putExtra(Intent.EXTRA_STREAM, dtaFile);
		//				startActivity(intent);
		//			}
		//		}
		//		break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private boolean createDTAFile() {
		List<HistoryItem> historyItems = historyManager.buildHistoryItemsForDTA();
		String error = dtaFileCreator.getFirstError(historyItems);

		if(error != ""){
			setOKAlert(error);
			return false;
		}

		CharSequence dta = dtaFileCreator.buildDTA(historyItems);

		if (!dtaFileCreator.saveDTAFile(dta.toString())) {
			setOKAlert(R.string.msg_unmount_usb);
			return false;
		} else {
			Uri dtaFileUri = dtaFileCreator.getDTAFileUri();
			String dtaFileName = dtaFileUri.getLastPathSegment();

			new HistoryExportUpdateAsyncTask(historyManager, dtaFileName)
			.execute(historyItems.toArray(new HistoryItem[historyItems.size()]));
			
			this.dtaFileCreator = new DTAFileCreator(getApplicationContext());
			
			Toast toast = Toast.makeText(this, 
					getResources().getString(R.string.msg_dta_saved, 
							dtaFileUri.getPath()), 
							Toast.LENGTH_LONG);
			toast.setGravity(Gravity.BOTTOM, 0, 0);
			toast.show();

			return true;
		}
	}

	private Intent createShareIntent() {
		Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		intent.setType("text/plain");
		String[] recipients = new String[]{PreferenceManager.getDefaultSharedPreferences(this)
				.getString(PreferencesActivity.KEY_EMAIL_ADDRESS, "")};
		intent.putExtra(Intent.EXTRA_EMAIL, recipients);
		String subject = getResources().getString(R.string.history_email_as_dta_title);
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_TEXT, subject);

		intent.putExtra(Intent.EXTRA_STREAM, this.dtaFileCreator.getDTAFileUri());

		return intent;
	}

	// Call to update the share intent
	private void setShareIntent(Intent shareIntent) {
		if (mShareActionProvider != null) {
			mShareActionProvider.setShareIntent(shareIntent);
		}
	}

	private void setOKAlert(String message){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.button_ok, null);
		builder.show();
	}

	private void setOptionalOKAlert(int id) {
		int dontShow = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(PreferencesActivity.KEY_NOT_SHOW_ALERT + String.valueOf(id), 0);

		if (dontShow == 0) {
			lastAlertId = id;

			LayoutInflater inflater = getLayoutInflater();
			final View checkboxLayout = inflater.inflate(R.layout.dont_show_again, null);
			dontShowAgainCheckBox = (CheckBox)checkboxLayout.findViewById(R.id.dont_show_again_checkbox);

			AlertDialog.Builder builder = new AlertDialog.Builder(this)
			.setTitle(R.string.alert_title_information)
			.setMessage(lastAlertId)
			.setView(checkboxLayout)
			.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (dontShowAgainCheckBox.isChecked()){
						PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
						.edit()
						.putInt(PreferencesActivity.KEY_NOT_SHOW_ALERT + String.valueOf(lastAlertId), 1)
						.apply();
					}
				}
			});

			builder.show();
		}
	}

	private void setOKAlert(int id){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(id);
		builder.setPositiveButton(R.string.button_ok, null);
		builder.show();
	}

	private void reload(){
		startActivity(getIntent()); 
		finish();
	}
}
