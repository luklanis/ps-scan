/*
 * Copyright 2012 ZXing authors
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
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import ch.luklanis.esscan.CaptureActivity;
import ch.luklanis.esscan.Intents;
import ch.luklanis.esscan.R;
import ch.luklanis.esscan.paymentslip.DTAFileCreator;

import java.util.List;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

public final class HistoryActivity extends SherlockListActivity {
	private static final int SEND_DTA_ID = Menu.FIRST;
	private static final int SEND_ID = Menu.FIRST + 1;
	//  private static final int CLEAR_ID = Menu.FIRST + 2;

	private HistoryManager historyManager;
	private HistoryItemAdapter adapter;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		this.historyManager = new HistoryManager(this);  
		adapter = new HistoryItemAdapter(this);
		setListAdapter(adapter);
		ListView listview = getListView();
		registerForContextMenu(listview);
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
		//    super.onCreateOptionsMenu(menu);
		if (historyManager.hasHistoryItems()) {
			MenuInflater inflater = getSupportMenuInflater();
			inflater.inflate(R.menu.history_menu, menu);
			//      menu.add(0, SEND_ID, 0, R.string.history_send).setIcon(android.R.drawable.ic_menu_share);
			//      menu.add(0, CLEAR_ID, 0, R.string.history_clear_text).setIcon(android.R.drawable.ic_menu_delete);
			SubMenu exportMenu = menu.addSubMenu(R.string.history_export);
			exportMenu.add(0, R.id.history_menu_send_dta, 0, R.string.history_send_dta);
			exportMenu.add(0, R.id.history_menu_send_csv, 0, R.string.history_send);

	        MenuItem exportMenuItem = exportMenu.getItem();
	        exportMenuItem.setIcon(android.R.drawable.ic_menu_share);
	        exportMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.history_menu_send_csv:
			CharSequence history = historyManager.buildHistory();
			Uri historyFile = HistoryManager.saveHistory(history.toString());
			if (historyFile == null) {
				setOKAlert(R.string.msg_unmount_usb);
			} else {
				Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				String subject = getResources().getString(R.string.history_email_title);
				intent.putExtra(Intent.EXTRA_SUBJECT, subject);
				intent.putExtra(Intent.EXTRA_TEXT, subject);
				intent.putExtra(Intent.EXTRA_STREAM, historyFile);
				intent.setType("text/csv");
				startActivity(intent);
			}
			break;
		case R.id.history_menu_clear:
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
			break;
		case R.id.history_menu_send_dta:
			List<HistoryItem> historyItems = historyManager.buildHistoryItemsForDTA();
			DTAFileCreator dtaFileCreator = new DTAFileCreator(this);
			String error = dtaFileCreator.getFirstError(historyItems);

			if(error != ""){
				setOKAlert(error);
				break;
			}

			CharSequence dta = dtaFileCreator.buildDTA(historyItems);

			Uri dtaFile = DTAFileCreator.saveDTAFile(dta.toString());
			if (dtaFile == null) {
				setOKAlert(R.string.msg_unmount_usb);
			} else {
				String dtaFileName = dtaFile.getLastPathSegment();

				new HistoryExportUpdateAsyncTask(this.historyManager, dtaFileName)
				.execute(historyItems.toArray(new HistoryItem[historyItems.size()]));

				Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				String subject = getResources().getString(R.string.history_email_as_dta_title);
				intent.putExtra(Intent.EXTRA_SUBJECT, subject);
				intent.putExtra(Intent.EXTRA_TEXT, subject);
				intent.putExtra(Intent.EXTRA_STREAM, dtaFile);
				intent.setType("text/plain");
				startActivity(intent);
			}
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private void setOKAlert(String message){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.button_ok, null);
		builder.show();
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
