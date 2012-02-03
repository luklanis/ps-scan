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

import ch.luklanis.esscan.language.LanguageCodeHelper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Class to handle preferences that are saved across sessions of the app. Shows
 * a hierarchy of preferences to the user, organized into sections. These
 * preferences are displayed in the options menu that is shown when the user
 * presses the MENU button.
 * 
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing
 */
public class PreferencesActivity extends PreferenceActivity implements
  OnSharedPreferenceChangeListener {
  
  // Preference keys not carried over from ZXing project
  public static final String KEY_SOURCE_LANGUAGE_PREFERENCE = "preferences_source_language";
  public static final String KEY_CHARACTER_WHITELIST = "preferences_character_whitelist";
  public static final String KEY_ONLY_MACRO_FOCUS = "preferences_only_macro_focus";
  public static final String KEY_ADDRESS = "preferences_address";
  public static final String KEY_IBAN = "preferences_iban";
  public static final String KEY_EXECUTION_DAY = "preferences_execution_day";
  
  // Preference keys carried over from ZXing project
  public static final String KEY_REVERSE_IMAGE = "preferences_reverse_image";
  public static final String KEY_PLAY_BEEP = "preferences_play_beep";
  public static final String KEY_VIBRATE = "preferences_vibrate";
  
  public static final String KEY_HELP_VERSION_SHOWN = "preferences_help_version_shown";
  
  //private CheckBoxPreference checkBoxPreferenceContinuousPreview;
  private ListPreference listPreferenceSourceLanguage;
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
    addPreferencesFromResource(R.xml.preferences);
    
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    
    listPreferenceSourceLanguage = (ListPreference) getPreferenceScreen().findPreference(KEY_SOURCE_LANGUAGE_PREFERENCE);
    editTextPreferenceCharacterWhitelist = (EditTextPreference) getPreferenceScreen().findPreference(KEY_CHARACTER_WHITELIST);
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
    // Update preference summary values to show current preferences
    if(key.equals(KEY_SOURCE_LANGUAGE_PREFERENCE)) {
      
      // Set the summary text for the source language name
      listPreferenceSourceLanguage.setSummary(LanguageCodeHelper.getOcrLanguageName(getBaseContext(), sharedPreferences.getString(key, "deu")));
      
      // Retrieve the character whitelist for the new language
      String whitelist = OcrCharacterHelper.getWhitelist(sharedPreferences, listPreferenceSourceLanguage.getValue());
      
      // Save the character whitelist to preferences
      sharedPreferences.edit().putString(KEY_CHARACTER_WHITELIST, whitelist).commit();
      
      // Set the whitelist summary text
      editTextPreferenceCharacterWhitelist.setSummary(whitelist);

    } else if (key.equals(KEY_CHARACTER_WHITELIST)) {
      
      // Save a separate, language-specific character blacklist for this language
      OcrCharacterHelper.setWhitelist(sharedPreferences, 
          listPreferenceSourceLanguage.getValue(), 
          sharedPreferences.getString(key, OcrCharacterHelper.getDefaultWhitelist(listPreferenceSourceLanguage.getValue())));
      
      // Set the summary text
      editTextPreferenceCharacterWhitelist.setSummary(sharedPreferences.getString(key, OcrCharacterHelper.getDefaultWhitelist(listPreferenceSourceLanguage.getValue())));
      
    } else if (key.equals(KEY_IBAN)){
    	String iban = sharedPreferences.getString(key, "").replaceAll("[\\s\\r\\n]+", "");
    	
    	if(iban == ""){
    		return;
    	}
    	
    	if(iban.length() != 21){
    		setOKAlert(R.string.msg_own_iban_is_not_valid);
    		return;
    	}
    	
    	iban = iban.substring(4, 21) + iban.substring(0, 4);

		StringBuilder ibanNumber = new StringBuilder(1000);
		
		for(int i=0; i<iban.length();i++){
			char ibanChar = iban.charAt(i);
			
			if(ibanChar < '0' || ibanChar > '9'){
				int ibanLetter = 10 + (ibanChar - 'A');
				ibanNumber.append(ibanLetter);
			}
			else{
				ibanNumber.append(ibanChar);
			}
		}
		
		int lastEnd = 0;
		int subIbanLength = 9;
		int modulo97 = 97;
		
		int subIban = Integer.parseInt(ibanNumber.substring(lastEnd, subIbanLength));
		int lastModulo = subIban % modulo97;
		lastEnd = subIbanLength;
		
		while(lastEnd < ibanNumber.length()){
			if((lastEnd + subIbanLength) < ibanNumber.length()){
				int newEnd = lastEnd + subIbanLength - 2;
				subIban = Integer.parseInt(String.format("%2d%s", lastModulo, ibanNumber.substring(lastEnd, newEnd)));
				lastEnd = newEnd;
			}
			else{
				subIban = Integer.parseInt(String.format("%2d%s", lastModulo, ibanNumber.substring(lastEnd, ibanNumber.length())));
				lastEnd = ibanNumber.length();
			}
			
			lastModulo = subIban % modulo97;
		}
		
		if(lastModulo != 1){
    		setOKAlert(R.string.msg_own_iban_is_not_valid);
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
    listPreferenceSourceLanguage.setSummary(LanguageCodeHelper.getOcrLanguageName(getBaseContext(), sharedPreferences.getString(KEY_SOURCE_LANGUAGE_PREFERENCE, "deu")));
    editTextPreferenceCharacterWhitelist.setSummary(sharedPreferences.getString(KEY_CHARACTER_WHITELIST, OcrCharacterHelper.getDefaultWhitelist(listPreferenceSourceLanguage.getValue())));
    
    // Set up a listener whenever a key changes
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  /**
   * Called when Activity is about to lose focus. Unregisters the
   * OnSharedPreferenceChangeListener.
   */
  @Override
  protected void onPause() {
    super.onPause();
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
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