/*
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

import android.content.SharedPreferences;

/**
 * Helper class to enable language-specific character blacklists/whitelists.
 */
public class OcrCharacterHelper {
  public static final String KEY_CHARACTER_BLACKLIST_PAYMENT_SLIP = "preference_character_blacklist_payment_slip"; 

  public static final String KEY_CHARACTER_WHITELIST_PAYMENT_SLIP = "preference_character_whitelist_payment_slip"; 
  
  private OcrCharacterHelper() {} // Private constructor to enforce noninstantiability
  
  public static String getDefaultBlacklist(String languageCode) {
    if (languageCode.equals("deu")) { return ""; } // Payment slip
    else {
      throw new IllegalArgumentException();
    }
  }
  
  public static String getDefaultWhitelist(String languageCode) {
    if (languageCode.equals("eng")) { return "!?@#$%()<>_-+=/.,:;'\"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"; } // English
    else if (languageCode.equals("deu")) { return ">+0123456789"; } // Payment slip
    else {
      throw new IllegalArgumentException();
    }
  }

  public static String getBlacklist(SharedPreferences prefs, String languageCode) {
    if (languageCode.equals("deu")) { return prefs.getString(KEY_CHARACTER_BLACKLIST_PAYMENT_SLIP, getDefaultBlacklist(languageCode)); }
    else {
      throw new IllegalArgumentException();
    }    
  }
  
  public static String getWhitelist(SharedPreferences prefs, String languageCode) {
    if (languageCode.equals("deu")) { return prefs.getString(KEY_CHARACTER_WHITELIST_PAYMENT_SLIP, getDefaultWhitelist(languageCode)); }
    else {
      throw new IllegalArgumentException();
    }        
  }
  
  public static void setBlacklist(SharedPreferences prefs, String languageCode, String blacklist) {
    if (languageCode.equals("deu")) { prefs.edit().putString(KEY_CHARACTER_BLACKLIST_PAYMENT_SLIP, blacklist).commit(); }
    else {
      throw new IllegalArgumentException();
    }    
  }
  
  public static void setWhitelist(SharedPreferences prefs, String languageCode, String whitelist) {
    if (languageCode.equals("deu")) { prefs.edit().putString(KEY_CHARACTER_WHITELIST_PAYMENT_SLIP, whitelist).commit(); }
    else {
      throw new IllegalArgumentException();
    }    
  }
}
