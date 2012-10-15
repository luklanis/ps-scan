/*
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

package ch.luklanis.esscan.history;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;
import ch.luklanis.esscan.CaptureActivity;
import ch.luklanis.esscan.Intents;
import ch.luklanis.esscan.PreferencesActivity;
import ch.luklanis.esscan.R;
import ch.luklanis.esscan.paymentslip.DTAFileCreator;
import ch.luklanis.esscan.paymentslip.EsResult;
import ch.luklanis.esscan.paymentslip.EsrResult;
import ch.luklanis.esscan.paymentslip.PsResult;

import java.util.List;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.actionbarsherlock.widget.ShareActionProvider.OnShareTargetSelectedListener;

public final class HistoryActivity extends SherlockFragmentActivity implements HistoryFragment.HistoryCallbacks {

	public static final String ACTION_SHOW_RESULT = "action_show_result";

	public static final String EXTRA_CODE_ROW = "extra_code_row";

	private static final int DETAILS_REQUEST_CODE = 0;

	private boolean twoPane;

	private HistoryManager historyManager;
	private ShareActionProvider shareActionProvider;
	private int lastAlertId;
	private DTAFileCreator dtaFileCreator;
	private CheckBox dontShowAgainCheckBox;

	private boolean streamModeEnabled;

	private int newPosition;

	private int oldPosition;

	final private OnQueryTextListener queryListener = new OnQueryTextListener() {       

		@Override
		public boolean onQueryTextChange(String newText) {
			if (TextUtils.isEmpty(newText)) {
				getSupportActionBar().setSubtitle("History");
			} else {
				getSupportActionBar().setSubtitle("History - Searching for: " + newText);
			}

			HistoryFragment historyFragment = ((HistoryFragment) getSupportFragmentManager()
					.findFragmentById(R.id.history));
			
			HistoryItemAdapter adapter = historyFragment.getAdapter();
			
			if (adapter != null) {
				adapter.getFilter().filter(newText); 
			}
			return true;
		}

		@Override
		public boolean onQueryTextSubmit(String query) {            
			Toast.makeText(getApplication(), "Searching for: " + query + "...", Toast.LENGTH_SHORT).show();
			return true;
		}
	};

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.activity_history);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		HistoryFragment historyFragment = ((HistoryFragment) getSupportFragmentManager()
				.findFragmentById(R.id.history));

		if (findViewById(R.id.ps_detail_container) != null) {
			twoPane = true;
			historyFragment.setActivateOnItemClick(true);
		}
		
		oldPosition = ListView.INVALID_POSITION;
		newPosition = ListView.INVALID_POSITION;

		dtaFileCreator = new DTAFileCreator(this);
		historyManager = new HistoryManager(this);

		Intent intent = getIntent();

		if (intent.getAction() != null && intent.getAction().equals(ACTION_SHOW_RESULT)){
			String codeRow = intent.getStringExtra(EXTRA_CODE_ROW);
			PsResult psResult = PsResult.getCoderowType(codeRow).equals(EsResult.PS_TYPE_NAME) 
					? new EsResult(codeRow) : new EsrResult(codeRow);

			historyManager.addHistoryItem(psResult);
			historyFragment.showPaymentSlipDetail();
			intent.setAction(null);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (historyManager.hasHistoryItems()) {
			getSupportMenuInflater().inflate(R.menu.history_menu, menu);
			
			SearchView searchView = (SearchView) menu.findItem(R.id.history_menu_search).getActionView();
			searchView.setOnQueryTextListener(queryListener);

			MenuItem item = menu.findItem(R.id.history_menu_copy_code_row);
			
			if (twoPane) {
				item.setVisible(true);
			} else {
				item.setVisible(false);
			}

			// Locate MenuItem with ShareActionProvider
			item = menu.findItem(R.id.history_menu_send_dta);

			if(dtaFileCreator.getFirstErrorId() == 0) {
				// Fetch and store ShareActionProvider
				shareActionProvider = (ShareActionProvider) item.getActionProvider();
				shareActionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);

				shareActionProvider.setOnShareTargetSelectedListener(new OnShareTargetSelectedListener() {

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
				setOkAlert(R.string.msg_unmount_usb);
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
		case android.R.id.home: {
            if (!PsDetailActivity.savePaymentSlip(this)) {
            	return true;
            }
            
            NavUtils.navigateUpTo(this, new Intent(this, CaptureActivity.class));
            return true;
        }
		case R.id.history_menu_copy_code_row:
		{
			HistoryFragment historyFragment = ((HistoryFragment) getSupportFragmentManager()
					.findFragmentById(R.id.history));

			int position = historyFragment == null ? ListView.INVALID_POSITION : historyFragment.getActivatedPosition();

			if (position != ListView.INVALID_POSITION) {
				PsResult result = historyFragment.getAdapter().getItem(position).getResult();

				ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				clipboardManager.setText(result.getCompleteCode());

				//        clipboardManager.setPrimaryClip(ClipData.newPlainText("ocrResult", ocrResultView.getText()));
				//      if (clipboardManager.hasPrimaryClip()) {
				if (clipboardManager.hasText()) {
					Toast toast = Toast.makeText(getApplicationContext(), R.string.msg_copied, Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.BOTTOM, 0, 0);
					toast.show();
				}
			}
		}
		break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public void onItemSelected(int oldPosition, int newPosition) {
		
		if (streamModeEnabled) {
			Intent intent = new Intent(this, CaptureActivity.class);
			intent.putExtra(Intents.History.ITEM_NUMBER, newPosition);
			setResult(Activity.RESULT_OK, intent);
			finish();
			return;
		}
		
		if (twoPane) {
			this.newPosition = ListView.INVALID_POSITION;
			this.oldPosition = ListView.INVALID_POSITION;
			PsDetailFragment oldFragment = (PsDetailFragment)getSupportFragmentManager().findFragmentById(R.id.ps_detail_container);
			if (oldFragment != null) {
				int error = oldFragment.save();

				HistoryItem item = historyManager.buildHistoryItem(oldPosition);
				HistoryFragment fragment = (HistoryFragment)getSupportFragmentManager().findFragmentById(R.id.history);
				fragment.updatePosition(oldPosition, item);

				if (error > 0) {
					this.newPosition = newPosition;
					this.oldPosition = oldPosition;
					setCancelOkAlert(error, false);
					return;
				}
			}

			setNewDetails(newPosition);

		} else {
			Intent detailIntent = new Intent(this, PsDetailActivity.class);
			detailIntent.putExtra(PsDetailFragment.ARG_POSITION, newPosition);
			startActivityForResult(detailIntent, DETAILS_REQUEST_CODE);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(resultCode == RESULT_OK && requestCode == DETAILS_REQUEST_CODE){

			if (intent.hasExtra(Intents.History.ITEM_NUMBER)) {
				int position = intent.getIntExtra(Intents.History.ITEM_NUMBER, -1);

				HistoryItem item = historyManager.buildHistoryItem(position);

				HistoryFragment fragment = (HistoryFragment)getSupportFragmentManager().findFragmentById(R.id.history);
				fragment.updatePosition(position, item);
			}
		}
	}

	private void setNewDetails(int position) {
		Bundle arguments = new Bundle();
		arguments.putInt(PsDetailFragment.ARG_POSITION, position);
		PsDetailFragment fragment = new PsDetailFragment();
		fragment.setArguments(arguments);

		getSupportFragmentManager()
		.beginTransaction()
		.replace(R.id.ps_detail_container, fragment)
		.commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		streamModeEnabled = PreferenceManager.getDefaultSharedPreferences(this)
				.getBoolean(PreferencesActivity.KEY_ENABLE_STREAM_MODE, false);

		int error = dtaFileCreator.getFirstErrorId();

		if(error != 0){
			setOptionalOkAlert(error);
		} else {
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			PsDetailFragment oldFragment = (PsDetailFragment)getSupportFragmentManager().findFragmentById(R.id.ps_detail_container);
			if (oldFragment != null) {
				int error = oldFragment.save();
				
				if (error > 0) {
					setCancelOkAlert(error, true);
					return true;
				}
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	// Call to update the share intent
	private void setShareIntent(Intent shareIntent) {
		if (shareActionProvider != null) {
			shareActionProvider.setShareIntent(shareIntent);
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

	private void setOkAlert(int id){
		setOKAlert(getResources().getString(id));
	}

	private void setOKAlert(String message){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.button_ok, null);
		builder.show();
	}

	private void setCancelOkAlert(int id, boolean finish){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		builder.setMessage(id)
		.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				HistoryFragment fragment = (HistoryFragment)getSupportFragmentManager().findFragmentById(R.id.history);
				fragment.setActivatedPosition(oldPosition);
				
				oldPosition = ListView.INVALID_POSITION;
				newPosition = ListView.INVALID_POSITION;
			}
		});

		if (finish) {
			builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
		} else {
			builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					setNewDetails(newPosition);
					
					newPosition = ListView.INVALID_POSITION;
					oldPosition = ListView.INVALID_POSITION;
				}
			});
		}
		
		builder.show();
	}

	public void setOptionalOkAlert(int id) {
		int dontShow = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
				.getInt(PreferencesActivity.KEY_NOT_SHOW_ALERT + String.valueOf(id), 0);

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
					if (dontShowAgainCheckBox.isChecked()) {
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

	private boolean createDTAFile() {
		List<HistoryItem> historyItems = historyManager.buildHistoryItemsForDTA();
		String error = dtaFileCreator.getFirstError(historyItems);

		if(error != ""){
			setOKAlert(error);
			return false;
		}

		CharSequence dta = dtaFileCreator.buildDTA(historyItems);

		if (!dtaFileCreator.saveDTAFile(dta.toString())) {
			setOkAlert(R.string.msg_unmount_usb);
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
}
