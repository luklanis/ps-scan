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

package ch.luklanis.esscanlite.history;

import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.Context;

/**
 * @author Sean Owen
 */
public final class DBHelper extends SQLiteOpenHelper {

  public static final String DB_NAME = "esrscan.db";
  private static final int DB_VERSION = 6;
  static final String ID_COL = "id";
  
  static final String HISTORY_TABLE_NAME = "history";
  static final String HISTORY_CODE_ROW_COL = "code_row";
  static final String HISTORY_TIMESTAMP_COL = "timestamp";
  static final String HISTORY_ADDRESS_COL = "address";
  static final String HISTORY_AMOUNT_COL = "amount";
  static final String HISTORY_FILE_NAME_COL = "file";
  
  static final String ADDRESS_TABLE_NAME = "address";
  static final String ADDRESS_ACCOUNT_COL = "account";
  static final String ADDRESS_ADDRESS_COL = "address";
  static final String ADDRESS_TIMESTAMP_COL = "timestamp";

  DBHelper(Context context) {
    super(context, DB_NAME, null, DB_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase sqLiteDatabase) {
    sqLiteDatabase.execSQL(
            "CREATE TABLE " + HISTORY_TABLE_NAME + " (" +
            ID_COL + " INTEGER PRIMARY KEY, " +
            HISTORY_CODE_ROW_COL + " TEXT, " +
            HISTORY_TIMESTAMP_COL + " INTEGER, " +
            HISTORY_ADDRESS_COL + " INTEGER, " +
            HISTORY_AMOUNT_COL + " TEXT, " +
            HISTORY_FILE_NAME_COL + " TEXT);");
    
    sqLiteDatabase.execSQL(
            "CREATE TABLE " + ADDRESS_TABLE_NAME + " (" +
            ID_COL + " INTEGER PRIMARY KEY, " +
            ADDRESS_ACCOUNT_COL + " TEXT, " +
            ADDRESS_TIMESTAMP_COL + " INTEGER, " +
            ADDRESS_ADDRESS_COL + " TEXT);");
  }

  @Override
  public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + HISTORY_TABLE_NAME);
    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ADDRESS_TABLE_NAME);
    onCreate(sqLiteDatabase);
  }

}
