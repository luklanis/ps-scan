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

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
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
import ch.luklanis.esscan.codesend.ESRSender;
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

public final class HistoryActivity extends SherlockFragmentActivity implements HistoryFragment.HistoryCallbacks {

	public static final String ACTION_SHOW_RESULT = "action_show_result";

	public static final String EXTRA_CODE_ROW = "extra_code_row";

	private static final int DETAILS_REQUEST_CODE = 0;

	private boolean twoPane;

	private HistoryManager historyManager;
	private int lastAlertId;
	private DTAFileCreator dtaFileCreator;
	private CheckBox dontShowAgainCheckBox;

	private int[] tmpPositions;

	final private OnQueryTextListener queryListener = new OnQueryTextListener() {       

		@Override
		public boolean onQueryTextChange(String newText) {
			if (TextUtils.isEmpty(newText)) {
				getSupportActionBar().setSubtitle("History");
			} else {
				getSupportActionBar().setSubtitle("History - Searching for: " + newText);
			}

			HistoryItemAdapter adapter = historyFragment.getHistoryItemAdapter();

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

	private Intent serviceIntent;

	private boolean serviceIsBound;

	private ESRSender boundService;

	private ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (ESRSender.EXISTS) {
				boundService = ((ESRSender.LocalBinder)service).getService();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
		}
	};

	private HistoryFragment historyFragment;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.activity_history);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		this.historyFragment = ((HistoryFragment) getSupportFragmentManager()
				.findFragmentById(R.id.history));

		if (findViewById(R.id.ps_detail_container) != null) {
			twoPane = true;
			this.historyFragment.setActivateOnItemClick(true);
		}

		tmpPositions = new int[2];
		tmpPositions[0] = ListView.INVALID_POSITION;	// old position
		tmpPositions[1] = ListView.INVALID_POSITION;	// new position

		dtaFileCreator = new DTAFileCreator(this);
		historyManager = new HistoryManager(this);

		Intent intent = getIntent();

		if (intent.getAction() != null && intent.getAction().equals(ACTION_SHOW_RESULT)){
			String codeRow = intent.getStringExtra(EXTRA_CODE_ROW);
			PsResult psResult = PsResult.getCoderowType(codeRow).equals(EsResult.PS_TYPE_NAME) 
					? new EsResult(codeRow) : new EsrResult(codeRow);

					if (twoPane) {
						this.historyManager.addHistoryItem(psResult);
						setNewDetails(0);
						intent.setAction(null);
					} else {
						Intent detailIntent = new Intent(this, PsDetailActivity.class);
						detailIntent.putExtra(PsDetailFragment.ARG_POSITION, 0);
						startActivityForResult(detailIntent, DETAILS_REQUEST_CODE);
					}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (historyManager.hasHistoryItems()) {
			getSupportMenuInflater().inflate(R.menu.history_menu, menu);

			SearchView searchView = (SearchView) menu.findItem(R.id.history_menu_search).getActionView();
			searchView.setOnQueryTextListener(queryListener);

			MenuItem copyItem = menu.findItem(R.id.history_menu_copy_code_row);
			MenuItem sendItem = menu.findItem(R.id.history_menu_send_code_row);

			if (twoPane && this.historyFragment.getActivatedPosition() != ListView.INVALID_POSITION) {
				copyItem.setVisible(true);

				if (ESRSender.EXISTS) {
					sendItem.setVisible(true);
				}
			} else {
				copyItem.setVisible(false);
				sendItem.setVisible(false);
			}

			MenuItem item = menu.findItem(R.id.history_menu_send_dta);

			if(dtaFileCreator.getFirstErrorId() != 0) {
				item.setVisible(false);
			}

			return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.history_menu_send_dta_wlan: {
			Uri dtaFileUri = createDTAFile();
			if (dtaFileUri != null) {
			}
		}
		break;
		case R.id.history_menu_send_dta_email: {
			Uri dtaFileUri = createDTAFile();
			if (dtaFileUri != null) {
				try {
					startActivity(createMailIntent(dtaFileUri));
				} catch (Exception ex) {
					Toast toast = Toast.makeText(getApplicationContext(), R.string.msg_no_email_client, Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.BOTTOM, 0, 0);
					toast.show();
				}
			}
		}
		break;
		case R.id.history_menu_send_dta_other: {
			Uri dtaFileUri = createDTAFile();
			if (dtaFileUri != null) {
				startActivity(Intent.createChooser(createShareIntent(dtaFileUri), "Send with..."));
			}
		}
		break;
		case R.id.history_menu_send_dta_save: {
			createDTAFile();
		}
		break;
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
			PsDetailFragment fragment = (PsDetailFragment)getSupportFragmentManager()
					.findFragmentById(R.id.ps_detail_container);

			if (fragment != null) {
				String completeCode = fragment.getHistoryItem().getResult().getCompleteCode();

				ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				clipboardManager.setText(completeCode);

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
		case R.id.history_menu_send_code_row:
		{
			PsDetailFragment fragment = (PsDetailFragment)getSupportFragmentManager()
					.findFragmentById(R.id.ps_detail_container);

			if (fragment != null) {
				String completeCode = fragment.getHistoryItem().getResult().getCompleteCode();

				int msgId = 0;

				if (ESRSender.EXISTS) {
					if (boundService != null && boundService.isConnectedLocal()) {
						boolean sent = this.boundService.sendToListeners(completeCode);

						if (sent) {
							String msg = getResources().getString(R.string.history_item_sent);
							historyManager.updateHistoryItemFileName(completeCode, msg);

							int position = this.historyFragment.getActivatedPosition();

							HistoryItem historyItem = historyManager.buildHistoryItem(position);
							this.historyFragment.updatePosition(position, historyItem);

							msgId = R.string.msg_coderow_sent;
						} else {
							msgId = R.string.msg_coderow_not_sent;
						}
					} else if (boundService != null) { 
						msgId = R.string.msg_stream_mode_not_available;
					}

					if (msgId != 0) {
						Toast toast = Toast.makeText(getApplicationContext(), msgId, Toast.LENGTH_SHORT);
						toast.setGravity(Gravity.BOTTOM, 0, 0);
						toast.show();
					}
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
	public void selectTopInTwoPane() {
		if (twoPane) {
			onItemSelected(ListView.INVALID_POSITION, 0);
		}
	}

	@Override
	public void onItemSelected(int oldPosition, int newPosition) {

		if (twoPane) {
			tmpPositions[0] = ListView.INVALID_POSITION;
			tmpPositions[1] = ListView.INVALID_POSITION;
			PsDetailFragment oldFragment = (PsDetailFragment)getSupportFragmentManager().findFragmentById(R.id.ps_detail_container);
			if (oldPosition != ListView.INVALID_POSITION && oldFragment != null) {
				int error = oldFragment.save();

				HistoryItem item = historyManager.buildHistoryItem(oldPosition);
				this.historyFragment.updatePosition(oldPosition, item);

				if (error > 0) {
					tmpPositions[0] = oldPosition;
					tmpPositions[1] = newPosition;
					setCancelOkAlert(error, false);
					return;
				}
			} else {
				invalidateOptionsMenu();
			}

			setNewDetails(newPosition);

		} else {
			Intent detailIntent = new Intent(this, PsDetailActivity.class);
			detailIntent.putExtra(PsDetailFragment.ARG_POSITION, newPosition);
			startActivityForResult(detailIntent, DETAILS_REQUEST_CODE);
		}
	}

	@Override
	public int getPositionToActivate() {
		PsDetailFragment fragment = (PsDetailFragment)getSupportFragmentManager().findFragmentById(R.id.ps_detail_container);
		if (fragment != null && fragment.isInLayout()) {
			return fragment.getListPosition();
		}

		return -1;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(resultCode == RESULT_OK && requestCode == DETAILS_REQUEST_CODE){

			if (intent.hasExtra(Intents.History.ITEM_NUMBER)) {
				int position = intent.getIntExtra(Intents.History.ITEM_NUMBER, -1);

				HistoryItem item = historyManager.buildHistoryItem(position);

				this.historyFragment.updatePosition(position, item);
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

		if (ESRSender.EXISTS && twoPane) {
			serviceIntent =  new Intent(this, ESRSender.class);
			startService(serviceIntent);

			doBindService();
		}

		int error = dtaFileCreator.getFirstErrorId();

		if(error != 0){
			setOptionalOkAlert(error);
		} else {
		}
	}

	@Override
	protected void onPause() {

		doUnbindService();

		super.onPause();
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

	private void doBindService() {
		if (!serviceIsBound) {
			bindService(serviceIntent, serviceConnection, 0);
			serviceIsBound = true;
		}
	}

	private void doUnbindService() {
		if (serviceIsBound) {		
			unbindService(serviceConnection);
			serviceIsBound = false;
		}
	}

	private Intent createShareIntent(Uri dtaFileUri) {
		return createShareIntent("text/plain", dtaFileUri);
	}

	private Intent createMailIntent(Uri dtaFileUri) {		
		return createShareIntent("message/rfc822", dtaFileUri);
	}

	private Intent createShareIntent(String mime, Uri dtaFileUri) {
		String[] recipients = new String[]{PreferenceManager.getDefaultSharedPreferences(this)
				.getString(PreferencesActivity.KEY_EMAIL_ADDRESS, "")};
		String subject = getResources().getString(R.string.history_share_as_dta_title);
		String text = String.format(getResources().getString(R.string.history_share_as_dta_summary), 
				dtaFileUri.getPath());

		Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		intent.setType(mime);

		intent.putExtra(Intent.EXTRA_EMAIL, recipients);
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_TEXT, text);

		intent.putExtra(Intent.EXTRA_STREAM, dtaFileUri);

		return intent;
	}

	private void setOkAlert(int id){
		setOkAlert(getResources().getString(id));
	}

	private void setOkAlert(String message){
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
				historyFragment.setActivatedPosition(tmpPositions[0]);

				tmpPositions[0] = ListView.INVALID_POSITION;
				tmpPositions[1] = ListView.INVALID_POSITION;
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
					setNewDetails(tmpPositions[1]);

					tmpPositions[0] = ListView.INVALID_POSITION;
					tmpPositions[1] = ListView.INVALID_POSITION;
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

	private Uri createDTAFile() {
		List<HistoryItem> historyItems = historyManager.buildHistoryItemsForDTA();
		String error = dtaFileCreator.getFirstError(historyItems);

		if(error != ""){
			setOkAlert(error);
			return null;
		}

		CharSequence dta = dtaFileCreator.buildDTA(historyItems);

		if (!dtaFileCreator.saveDTAFile(dta.toString())) {
			setOkAlert(R.string.msg_unmount_usb);
			return null;
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

			return dtaFileUri;
		}
	}
}
