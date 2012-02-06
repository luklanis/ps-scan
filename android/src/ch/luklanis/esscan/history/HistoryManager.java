/*
 * Copyright (C) 2009 ZXing authors
 * Copyright (C) 2012 Lukas Landis
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

import ch.luklanis.esscan.paymentslip.EsrResult;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p>Manages functionality related to scan history.</p>
 *
 * @author Sean Owen
 */
public final class HistoryManager {

	private static final String TAG = HistoryManager.class.getSimpleName();

	private static final int MAX_ITEMS = 500;

	private static final String[] HISTORY_COLUMNS = {
		DBHelper.HISTORY_CODE_ROW_COL,
		DBHelper.HISTORY_TIMESTAMP_COL,
		DBHelper.HISTORY_ADDRESS_COL,
		DBHelper.HISTORY_AMOUNT_COL,
		DBHelper.HISTORY_FILE_NAME_COL
	};

	private static final String[] ADDRESS_COLUMNS = {
		DBHelper.ADDRESS_ACCOUNT_COL,
		DBHelper.ADDRESS_ADDRESS_COL
	};

	private static final String[] COUNT_COLUMN = { "COUNT(1)" };

	private static final String[] ID_COL_PROJECTION = { DBHelper.ID_COL };
	private static final String[] ID_ADDRESS_COL_PROJECTION = { DBHelper.ID_COL, DBHelper.HISTORY_ADDRESS_COL };
	private static final String[] ID_AMOUNT_COL_PROJECTION = { DBHelper.ID_COL, DBHelper.HISTORY_AMOUNT_COL };
	private static final String[] ID_FILE_NAME_COL_PROJECTION = { DBHelper.ID_COL, DBHelper.HISTORY_FILE_NAME_COL };
	private static final DateFormat EXPORT_DATE_TIME_FORMAT = DateFormat.getDateTimeInstance();

	private final Activity activity;

	public HistoryManager(Activity activity) {
		this.activity = activity;
	}

	public boolean hasHistoryItems() {
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = helper.getReadableDatabase();
			cursor = db.query(DBHelper.HISTORY_TABLE_NAME, COUNT_COLUMN, null, null, null, null, null);
			cursor.moveToFirst();
			return cursor.getInt(0) > 0;
		} finally {
			close(cursor, db);
		}
	}

	public List<HistoryItem> buildHistoryItems() {
		SQLiteOpenHelper helper = new DBHelper(activity);
		List<HistoryItem> items = new ArrayList<HistoryItem>();
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = helper.getReadableDatabase();
			cursor = db.query(DBHelper.HISTORY_TABLE_NAME, HISTORY_COLUMNS, null, null, null, null, DBHelper.HISTORY_TIMESTAMP_COL + " DESC");
			while (cursor.moveToNext()) {
				String code_row = cursor.getString(0);
				long timestamp = cursor.getLong(1);
				String address = cursor.getString(2);
				String amount = cursor.getString(3);
				EsrResult result = new EsrResult(code_row, timestamp);
				items.add(new HistoryItem(result, amount, address));
			}
		} finally {
			close(cursor, db);
		}
		return items;
	}

	public HistoryItem buildHistoryItem(int number) {
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = helper.getReadableDatabase();
			cursor = db.query(DBHelper.HISTORY_TABLE_NAME, HISTORY_COLUMNS, null, null, null, null, DBHelper.HISTORY_TIMESTAMP_COL + " DESC");
			cursor.move(number + 1);
			String text = cursor.getString(0);
			long timestamp = cursor.getLong(1);
			String address = cursor.getString(2);
			String amount = cursor.getString(3);
			EsrResult result = new EsrResult(text, timestamp);
			return new HistoryItem(result, amount, address);
		} finally {
			close(cursor, db);
		}
	}

	public void deleteHistoryItem(int number) {
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = helper.getWritableDatabase();      
			cursor = db.query(DBHelper.HISTORY_TABLE_NAME,
					ID_COL_PROJECTION,
					null, null, null, null,
					DBHelper.HISTORY_TIMESTAMP_COL + " DESC");
			cursor.move(number + 1);
			db.delete(DBHelper.HISTORY_TABLE_NAME, DBHelper.ID_COL + '=' + cursor.getString(0), null);
		} finally {
			close(cursor, db);
		}
	}

	public void addHistoryItem(EsrResult result, String amount, String address) {
		// Do not save this item to the history if the preference is turned off, or the contents are
		// considered secure.
		//    if (!activity.getIntent().getBooleanExtra(Intents.Scan.SAVE_HISTORY, true)) {
		//      return;
		//    }

		//	  SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		//	  if (!prefs.getBoolean(PreferencesActivity.KEY_REMEMBER_DUPLICATES, false)) {
		//		  deletePrevious(result.getText());
		//	  }
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;    
		Cursor cursor = null;
		try {
			db = helper.getWritableDatabase();
			cursor = db.query(DBHelper.HISTORY_TABLE_NAME,
					HISTORY_COLUMNS,
					DBHelper.HISTORY_CODE_ROW_COL + "=?",
					new String[] { result.getCompleteCode() },
					null,
					null,
					DBHelper.HISTORY_TIMESTAMP_COL + " DESC",
					"1");
			String oldID = null;
			if (cursor.moveToNext()) {
				oldID = cursor.getString(0);
				ContentValues values = new ContentValues();
				values.put(DBHelper.HISTORY_AMOUNT_COL, amount);
				values.put(DBHelper.HISTORY_ADDRESS_COL, address);
				db.update(DBHelper.HISTORY_TABLE_NAME, values, DBHelper.ID_COL + "=?", new String[] { oldID });
			}
			else{
				ContentValues values = new ContentValues();
				values.put(DBHelper.HISTORY_CODE_ROW_COL, result.getCompleteCode());
				values.put(DBHelper.HISTORY_TIMESTAMP_COL, result.getTimestamp());
				values.put(DBHelper.HISTORY_AMOUNT_COL, amount);
				values.put(DBHelper.HISTORY_ADDRESS_COL, address);

				// Insert the new entry into the DB.
				db.insert(DBHelper.HISTORY_TABLE_NAME, DBHelper.HISTORY_TIMESTAMP_COL, values);
			}
		}
		finally {
			close(null, db);
		}
	}

	public void updateHistoryItemAddress(String code_row, String itemAddress) {
		updateHistoryItem(ID_ADDRESS_COL_PROJECTION, DBHelper.HISTORY_ADDRESS_COL, code_row, itemAddress);
	}

	public void updateHistoryItemAmount(String code_row, String itemAmount) {
		updateHistoryItem(ID_AMOUNT_COL_PROJECTION, DBHelper.HISTORY_AMOUNT_COL, code_row, itemAmount);
	}

	public void updateHistoryItemFileName(String code_row, String itemFileName) {
		updateHistoryItem(ID_FILE_NAME_COL_PROJECTION, DBHelper.HISTORY_FILE_NAME_COL, code_row, itemFileName);
	}

	private void updateHistoryItem(String[] projection, String col_name, String code_row, String item) {
		// As we're going to do an update only we don't need need to worry
		// about the preferences; if the item wasn't saved it won't be updated
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;    
		Cursor cursor = null;
		try {
			db = helper.getWritableDatabase();
			cursor = db.query(DBHelper.HISTORY_TABLE_NAME,
					projection,
					DBHelper.HISTORY_CODE_ROW_COL + "=?",
					new String[] { code_row },
					null,
					null,
					DBHelper.HISTORY_TIMESTAMP_COL + " DESC",
					"1");
			String oldID = null;
			if (cursor.moveToNext()) {
				oldID = cursor.getString(0);

				ContentValues values = new ContentValues();
				values.put(col_name, item);

				db.update(DBHelper.HISTORY_TABLE_NAME, values, DBHelper.ID_COL + "=?", new String[] { oldID });
			}

		} finally {
			close(cursor, db);
		}
	}

	@SuppressWarnings("unused")
	private void deletePrevious(String text) {
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		try {
			db = helper.getWritableDatabase();      
			db.delete(DBHelper.HISTORY_TABLE_NAME, DBHelper.HISTORY_CODE_ROW_COL + "=?", new String[] { text });
		} finally {
			close(null, db);
		}
	}

	public void trimHistory() {
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = helper.getWritableDatabase();      
			cursor = db.query(DBHelper.HISTORY_TABLE_NAME,
					ID_COL_PROJECTION,
					null, null, null, null,
					DBHelper.HISTORY_TIMESTAMP_COL + " DESC");
			cursor.move(MAX_ITEMS);
			while (cursor.moveToNext()) {
				db.delete(DBHelper.HISTORY_TABLE_NAME, DBHelper.ID_COL + '=' + cursor.getString(0), null);
			}
		} finally {
			close(cursor, db);
		}
	}

	/**
	 * <p>Builds a text representation of the scanning history. Each scan is encoded on one
	 * line, terminated by a line break (\r\n). The values in each line are comma-separated,
	 * and double-quoted. Double-quotes within values are escaped with a sequence of two
	 * double-quotes. The fields output are:</p>
	 *
	 * <ul>
	 *  <li>Code row</li>
	 *  <li>Formatted version of timestamp</li>
	 *  <li>Address text</li>
	 *  <li>Paid timespamp</li>
	 * </ul>
	 */
	CharSequence buildHistory() {
		StringBuilder historyText = new StringBuilder(1000);
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = helper.getWritableDatabase();
			cursor = db.query(DBHelper.HISTORY_TABLE_NAME,
					HISTORY_COLUMNS,
					null, null, null, null,
					DBHelper.HISTORY_TIMESTAMP_COL + " DESC");

			while (cursor.moveToNext()) {

				EsrResult result = new EsrResult(cursor.getString(0));

				// Add timestamp, formatted
				long timestamp = cursor.getLong(1);
				historyText.append('"').append(messageHistoryField(
						EXPORT_DATE_TIME_FORMAT.format(new Date(timestamp)))).append("\",");

				historyText.append('"').append(messageHistoryField(result.toString())).append("\",");

				historyText.append('"').append(messageHistoryField(cursor.getString(2)).split("[\\r\\n]+")[0]).append("\",");

				historyText.append('"').append(messageHistoryField(result.getCompleteCode())).append("\"\r\n");
			}
			return historyText;
		} finally {
			close(cursor, db);
		}
	}

	void clearHistory() {
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		try {
			db = helper.getWritableDatabase();      
			db.delete(DBHelper.HISTORY_TABLE_NAME, null, null);
		} finally {
			close(null, db);
		}
	}

	static Uri saveHistory(String history) {
		File bsRoot = new File(Environment.getExternalStorageDirectory(), "ESRScan");
		File historyRoot = new File(bsRoot, "History");
		if (!historyRoot.exists() && !historyRoot.mkdirs()) {
			Log.w(TAG, "Couldn't make dir " + historyRoot);
			return null;
		}
		File historyFile = new File(historyRoot, "history-" + System.currentTimeMillis() + ".csv");
		OutputStreamWriter out = null;
		try {
			out = new OutputStreamWriter(new FileOutputStream(historyFile), Charset.forName("UTF-8"));
			out.write(history);
			return Uri.parse("file://" + historyFile.getAbsolutePath());
		} catch (IOException ioe) {
			Log.w(TAG, "Couldn't access file " + historyFile + " due to " + ioe);
			return null;
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException ioe) {
					// do nothing
				}
			}
		}
	}

	public void addAddress(String account, String address) {
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;    
		Cursor cursor = null;
		try {
			db = helper.getWritableDatabase();
			cursor = db.query(DBHelper.ADDRESS_TABLE_NAME,
					ADDRESS_COLUMNS,
					DBHelper.ADDRESS_ACCOUNT_COL + "=?",
					new String[] { account },
					null,
					null,
					null,
					"1");
			String oldID = null;
			if (cursor.moveToNext()) {
				oldID = cursor.getString(0);
				ContentValues values = new ContentValues();
				values.put(DBHelper.ADDRESS_ADDRESS_COL, address);
				db.update(DBHelper.HISTORY_TABLE_NAME, values, DBHelper.ID_COL + "=?", new String[] { oldID });
			}
			else{
				ContentValues values = new ContentValues();
				values.put(DBHelper.ADDRESS_ACCOUNT_COL, account);
				values.put(DBHelper.ADDRESS_ADDRESS_COL, address);

				// Insert the new entry into the DB.
				db.insert(DBHelper.ADDRESS_TABLE_NAME, null, values);
			}
		}
		finally {
			close(null, db);
		}
	}

	public String getAddress(String account) {
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;    
		Cursor cursor = null;
		try {
			db = helper.getWritableDatabase();
			cursor = db.query(DBHelper.ADDRESS_TABLE_NAME,
					ADDRESS_COLUMNS,
					DBHelper.ADDRESS_ACCOUNT_COL + "=?",
					new String[] { account },
					null,
					null,
					null,
					"1");
			if (cursor.moveToNext()) {
				return cursor.getString(1);
			}
			else{
				return "";
			}
		}
		finally {
			close(null, db);
		}
	}

	private static String messageHistoryField(String value) {
		return value == null ? "" : value.replace("\"","\"\"");
	}

	private static void close(Cursor cursor, SQLiteDatabase database) {
		if (cursor != null) {
			cursor.close();
		}
		if (database != null) {
			database.close();
		}
	}

}
