/*
 * Copyright (C) 2008 ZXing authors
 * Copyright 2011 Robert Theis
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
package ch.luklanis.esscan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.Map.Entry;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import ch.luklanis.esscan.history.DBHelper;
import ch.luklanis.esscan.paymentslip.DTAFileCreator;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

/**
 * Class to handle preferences that are saved across sessions of the app. Shows
 * a hierarchy of preferences to the user, organized into sections. These
 * preferences are displayed in the options menu that is shown when the user
 * presses the MENU button.
 * 
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing
 */
public class PreferencesActivity extends SherlockPreferenceActivity implements
OnSharedPreferenceChangeListener {

	// Preference keys not carried over from ZXing project
	public static final String KEY_SOURCE_LANGUAGE_PREFERENCE = "preferences_source_language";
	public static final String KEY_CHARACTER_WHITELIST = "preferences_character_whitelist";
	public static final String KEY_ONLY_MACRO_FOCUS = "preferences_only_macro_focus";
	public static final String KEY_ENABLE_TORCH = "preferences_enable_torch";
	public static final String KEY_ADDRESS = "preferences_address";
	public static final String KEY_IBAN = "preferences_iban";
	public static final String KEY_EXECUTION_DAY = "preferences_execution_day";
	public static final String KEY_EMAIL_ADDRESS = "preferences_email_address";

	// Preference keys carried over from ZXing project
	public static final String KEY_REVERSE_IMAGE = "preferences_reverse_image";
	public static final String KEY_PLAY_BEEP = "preferences_play_beep";
	public static final String KEY_VIBRATE = "preferences_vibrate";

	public static final String KEY_HELP_VERSION_SHOWN = "preferences_help_version_shown";
	public static final String KEY_SHOW_OCR_RESULT_PREFERENCE = "preferences_show_ocr_result";
	public static final String KEY_NOT_SHOW_ALERT = "preferences_not_show_alertid_";
	public static final String KEY_ENABLE_STREAM_MODE = "preferences_enable_stream_mode";
	
	public static final String KEY_BUTTON_BACKUP = "preferences_button_backup";
	public static final String KEY_BUTTON_RESTORE = "preferences_button_restore";
	
	
	private static final String TAG = PreferencesActivity.class.getName();

	//  private ListPreference listPreferenceSourceLanguage;
	private EditTextPreference editTextPreferenceCharacterWhitelist;

	private static SharedPreferences sharedPreferences;

	/**
	 * Set the default preference values.
	 * 
	 * @param Bundle
	 *            savedInstanceState the current Activity's state, as passed by
	 *            Android
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Hide Icon in ActionBar
		getSupportActionBar().setDisplayShowHomeEnabled(false);
		
		addPreferencesFromResource(R.xml.preferences);

		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		//    listPreferenceSourceLanguage = (ListPreference) getPreferenceScreen().findPreference(KEY_SOURCE_LANGUAGE_PREFERENCE);
		editTextPreferenceCharacterWhitelist = (EditTextPreference) getPreferenceScreen().findPreference(KEY_CHARACTER_WHITELIST);
		
		Preference backupButton = (Preference)findPreference(KEY_BUTTON_BACKUP);
		backupButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
		                @Override
		                public boolean onPreferenceClick(Preference arg0) { 
		                    backupData();  
		                    return true;
		                }
		            });
		
		Preference restoreButton = (Preference)findPreference(KEY_BUTTON_RESTORE);
		restoreButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
		                @Override
		                public boolean onPreferenceClick(Preference arg0) { 
		                    restoreData();  
		                    return true;
		                }
		            });
	}

	/**
	 * Interface definition for a callback to be invoked when a shared
	 * preference is changed. Sets summary text for the app's preferences. Summary text values show the
	 * current settings for the values.
	 * 
	 * @param sharedPreferences
	 *            the Android.content.SharedPreferences that received the change
	 * @param key
	 *            the key of the preference that was changed, added, or removed
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {    
		//    // Update preference summary values to show current preferences
		//    if(key.equals(KEY_SOURCE_LANGUAGE_PREFERENCE)) {
		//      
		//      // Set the summary text for the source language name
		//      listPreferenceSourceLanguage.setSummary(LanguageCodeHelper.getOcrLanguageName(getBaseContext(), sharedPreferences.getString(key, "deu")));
		//      
		//      // Retrieve the character whitelist for the new language
		//      String whitelist = OcrCharacterHelper.getWhitelist(sharedPreferences, listPreferenceSourceLanguage.getValue());
		//      
		//      // Save the character whitelist to preferences
		//      sharedPreferences.edit().putString(KEY_CHARACTER_WHITELIST, whitelist).commit();
		//      
		//      // Set the whitelist summary text
		//      editTextPreferenceCharacterWhitelist.setSummary(whitelist);
		//
		//    } else if (key.equals(KEY_CHARACTER_WHITELIST)) {
		//  
		//      // Save a separate, language-specific character blacklist for this language
		//      OcrCharacterHelper.setWhitelist(sharedPreferences, 
		//          listPreferenceSourceLanguage.getValue(), 
		//          sharedPreferences.getString(key, OcrCharacterHelper.getDefaultWhitelist(listPreferenceSourceLanguage.getValue())));
		//      
		//      // Set the summary text
		//      editTextPreferenceCharacterWhitelist.setSummary(sharedPreferences.getString(key, OcrCharacterHelper.getDefaultWhitelist(listPreferenceSourceLanguage.getValue())));

		if (key.equals(KEY_CHARACTER_WHITELIST)) {

			// Save a separate, language-specific character blacklist for this language
			OcrCharacterHelper.setWhitelist(sharedPreferences, 
					"deu", 
					sharedPreferences.getString(key, OcrCharacterHelper.getDefaultWhitelist("deu")));

			// Set the summary text
			editTextPreferenceCharacterWhitelist.setSummary(sharedPreferences.getString(key, OcrCharacterHelper.getDefaultWhitelist("deu")));

		} else if (key.equals(KEY_IBAN)){
			String iban = sharedPreferences.getString(key, "").toUpperCase();
			sharedPreferences.edit().putString(key, iban).commit();

			int warning = DTAFileCreator.validateIBAN(iban);
			if (warning != 0){
				setOKAlert(warning);
			}
		}else if (key.equals(KEY_ADDRESS)){
			String address = sharedPreferences.getString(key, "");

			int warning = DTAFileCreator.validateAddress(address);
			if (warning != 0){
				setOKAlert(warning);
			}
		}
	}

	/**
	 * Sets up initial preference summary text
	 * values and registers the OnSharedPreferenceChangeListener.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		// Set up the initial summary values
		//    listPreferenceSourceLanguage.setSummary(LanguageCodeHelper.getOcrLanguageName(getBaseContext(), sharedPreferences.getString(KEY_SOURCE_LANGUAGE_PREFERENCE, "deu")));
		editTextPreferenceCharacterWhitelist.setSummary(sharedPreferences.getString(KEY_CHARACTER_WHITELIST, OcrCharacterHelper.getDefaultWhitelist("deu")));

		// Set up a listener whenever a key changes
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
	}

	/**
	 * Called when Activity is about to lose focus. Unregisters the
	 * OnSharedPreferenceChangeListener.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
	}

	private void backupData() {
		FileChannel src = null;
		FileChannel dst = null;
		ObjectOutputStream prefBackup = null;

		try {
			File sd = Environment.getExternalStorageDirectory();
			File data = Environment.getDataDirectory();

			if (sd.canWrite()) {
				File bsRoot = new File(Environment.getExternalStorageDirectory(), CaptureActivity.EXTERNAL_STORAGE_DIRECTORY);
				if (!bsRoot.exists() && !bsRoot.mkdirs()) {
					Log.w(TAG, "Couldn't make dir " + bsRoot);
					return;
				}

				String currentDBPath = "data/" + this.getPackageName() + "/databases/" + DBHelper.DB_NAME;
				String backupDBPath = CaptureActivity.EXTERNAL_STORAGE_DIRECTORY + "/" + DBHelper.DB_NAME;
				String backupPrefsPath = CaptureActivity.EXTERNAL_STORAGE_DIRECTORY + "/preferences.xml";
				File currentDB = new File(data, currentDBPath);
				File backupDB = new File(sd, backupDBPath);
				File backupPrefs = new File(sd, backupPrefsPath);

				if (currentDB.exists()) {
					src = new FileInputStream(currentDB).getChannel();
					dst = new FileOutputStream(backupDB).getChannel();
					dst.transferFrom(src, 0, src.size());

					prefBackup = new ObjectOutputStream(new FileOutputStream(backupPrefs));
					SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
					prefBackup.writeObject(pref.getAll());

					Toast toast = Toast.makeText(getApplicationContext(), 
							getResources().getString(R.string.msg_database_saved), 
							Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.BOTTOM, 0, 0);
					toast.show();
				}
			}
		} catch (Exception e) {
			Log.w(TAG, "restore failed", e);
		} finally {
			try {
				if (prefBackup != null) {
					prefBackup.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			try {
				if (src != null) {
					src.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			try {
				if (dst != null) {
					dst.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	private void restoreData() {
		ObjectInputStream input = null;
		FileChannel src = null;
		FileChannel dst = null;

		try {
			File sd = Environment.getExternalStorageDirectory();
			File data = Environment.getDataDirectory();

			String currentDBPath = "data/" + this.getPackageName() + "/databases/" + DBHelper.DB_NAME;
			String backupDBPath = CaptureActivity.EXTERNAL_STORAGE_DIRECTORY + "/" + DBHelper.DB_NAME;
			String backupPrefsPath = CaptureActivity.EXTERNAL_STORAGE_DIRECTORY + "/preferences.xml";
			File currentDB = new File(data, currentDBPath);
			File backupDB = new File(sd, backupDBPath);
			File backupPrefs = new File(sd, backupPrefsPath);

			if (currentDB.canWrite() && backupDB.exists()) {
				src = new FileInputStream(backupDB).getChannel();
				dst = new FileOutputStream(currentDB).getChannel();
				dst.transferFrom(src, 0, src.size());

				input = new ObjectInputStream(new FileInputStream(backupPrefs));
				Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
				prefEdit.clear();

				@SuppressWarnings("unchecked")
				Map<String, ?> entries = (Map<String, ?>) input.readObject();
				for (Entry<String, ?> entry : entries.entrySet()) {
					Object v = entry.getValue();
					String key = entry.getKey();

					if (v instanceof Boolean)
						prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
					else if (v instanceof Float)
						prefEdit.putFloat(key, ((Float) v).floatValue());
					else if (v instanceof Integer)
						prefEdit.putInt(key, ((Integer) v).intValue());
					else if (v instanceof Long)
						prefEdit.putLong(key, ((Long) v).longValue());
					else if (v instanceof String)
						prefEdit.putString(key, ((String) v));
				}
				prefEdit.commit();

				Toast toast = Toast.makeText(getApplicationContext(), 
						getResources().getString(R.string.msg_database_restored), 
						Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.BOTTOM, 0, 0);
				toast.show();

				this.finish();
			}
		} catch (Exception e) {
			Log.w(TAG, "restore failed", e);
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			try {
				if (src != null) {
					src.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			try {
				if (dst != null) {
					dst.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	private void setOKAlert(int id){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(id);
		builder.setPositiveButton(R.string.button_ok, new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				reload();
			}
		});
		builder.show();
	}

	private void reload(){
		startActivity(getIntent()); 
		finish();
	}
}