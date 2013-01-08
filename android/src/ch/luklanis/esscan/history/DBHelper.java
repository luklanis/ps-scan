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

import ch.luklanis.esscan.paymentslip.EsResult;
import ch.luklanis.esscan.paymentslip.EsrResult;
import ch.luklanis.esscan.paymentslip.PsResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.content.Context;

/**
 * @author Sean Owen
 */
public final class DBHelper extends SQLiteOpenHelper {

	public static final String DB_NAME = "esrscan.db";
	private static final int DB_VERSION = 7;
	static final String ID_COL = "id";

	static final String HISTORY_TABLE_NAME = "history";
	static final String HISTORY_CODE_ROW_COL = "code_row";
	static final String HISTORY_TIMESTAMP_COL = "timestamp";
	static final String HISTORY_ADDRESS_ID_COL = "address_id";
	static final String HISTORY_AMOUNT_COL = "amount";
	static final String HISTORY_FILE_NAME_COL = "file";

	static final String ADDRESS_TABLE_NAME = "address";
	static final String ADDRESS_ACCOUNT_COL = "account";
	static final String ADDRESS_ADDRESS_COL = "address";
	static final String ADDRESS_TIMESTAMP_COL = "timestamp";

	static final String CREATE_HISTORY_TABLE = "CREATE TABLE " + HISTORY_TABLE_NAME + " (" +
			ID_COL + " INTEGER PRIMARY KEY, " +
			HISTORY_CODE_ROW_COL + " TEXT, " +
			HISTORY_TIMESTAMP_COL + " INTEGER, " +
			HISTORY_ADDRESS_ID_COL + " INTEGER, " +
			HISTORY_AMOUNT_COL + " TEXT, " +
			HISTORY_FILE_NAME_COL + " TEXT)";

	static final String CREATE_ADDRESSS_TABLE = "CREATE TABLE " + ADDRESS_TABLE_NAME + " (" +
			ID_COL + " INTEGER PRIMARY KEY, " +
			ADDRESS_ACCOUNT_COL + " TEXT, " +
			ADDRESS_TIMESTAMP_COL + " INTEGER, " +
			ADDRESS_ADDRESS_COL + " TEXT)";

	DBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
		sqLiteDatabase.execSQL(CREATE_HISTORY_TABLE);

		sqLiteDatabase.execSQL(CREATE_ADDRESSS_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
		if (newVersion == 7) {
			sqLiteDatabase.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " RENAME TO history_old");
			sqLiteDatabase.execSQL(CREATE_HISTORY_TABLE);
			sqLiteDatabase.execSQL("INSERT INTO " + HISTORY_TABLE_NAME + " (" +
					HISTORY_CODE_ROW_COL + ", " +
					HISTORY_TIMESTAMP_COL + ", " +
					HISTORY_ADDRESS_ID_COL + ", " +
					HISTORY_AMOUNT_COL + ", " +
					HISTORY_FILE_NAME_COL + ") " +
					"SELECT " +
					HISTORY_CODE_ROW_COL + ", " +
					HISTORY_TIMESTAMP_COL + ", " +
					"address, " +
					HISTORY_AMOUNT_COL + ", " +
					HISTORY_FILE_NAME_COL + " FROM history_old");

			Cursor cursor = sqLiteDatabase.query(HISTORY_TABLE_NAME, 
					new String[] { ID_COL, HISTORY_CODE_ROW_COL, HISTORY_ADDRESS_ID_COL }, 
					null, 
					null, 
					null, 
					null, 
					null);

			while (cursor.moveToNext()) {
				String code_row = cursor.getString(1);
				int addressNumber = cursor.getInt(2);

				PsResult result;
				if (PsResult.getCoderowType(code_row).equals(EsrResult.PS_TYPE_NAME)) {
					result = new EsrResult(code_row);
				} else {
					result = new EsResult(code_row);
				}

				if(addressNumber != -1)
				{
					int addressId = getAddressId(sqLiteDatabase, result.getAccount(), addressNumber);
					ContentValues values = new ContentValues();
					values.put(HISTORY_ADDRESS_ID_COL, addressId);
					sqLiteDatabase.update(HISTORY_TABLE_NAME, values, DBHelper.ID_COL + '=' + cursor.getString(0), null);
				}
			}
		} else {
			sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + HISTORY_TABLE_NAME);
			sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ADDRESS_TABLE_NAME);
			onCreate(sqLiteDatabase);
		}
	}

	public int getAddressId(SQLiteDatabase db, String account, int addressNumber) {
		Cursor cursor = null;
		cursor = db.query(DBHelper.ADDRESS_TABLE_NAME,
				new String[] { ID_COL },
				DBHelper.ADDRESS_ACCOUNT_COL + "=?",
				new String[] { account },
				null,
				null,
				DBHelper.ADDRESS_TIMESTAMP_COL);
		if (cursor.move(addressNumber + 1)) {
			return cursor.getInt(0);
		}

		return -1;
	}

}
