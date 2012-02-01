package ch.luklanis.esscan.paymentslip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ch.luklanis.esscan.PreferencesActivity;
import ch.luklanis.esscan.history.HistoryItem;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ParseException;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

public class DTAFileCreator {

	private static final String TAG = DTAFileCreator.class.getName();
	private Activity activity;
	
	public DTAFileCreator(Activity activity){
		this.activity = activity;
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
	public CharSequence buildDTA(List<HistoryItem> historyItems) {
		StringBuilder historyText = new StringBuilder(1000);
		
		String today = getDateFormated(new Date(System.currentTimeMillis()));
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		
		String iban = prefs.getString(PreferencesActivity.KEY_IBAN, "");

		if(iban == ""){
			return "";
		}

		for (int i = 0; i < historyItems.size(); i++) {
			HistoryItem historyItem = historyItems.get(i);
			
			String clearing = String.valueOf((Integer.parseInt(iban.substring(4, 9))));

			// HEADER
			historyText
			.append("01")
			.append(getExecutionDateFormated())
			.append(spacePaddedEnd("", 12))
			.append(padded("", '0', 5, true))
			.append(nullToEmpty(today))
			.append(spacePaddedEnd(clearing, 7))
			.append(padded("", 'X', 5, true))
			.append(padded(String.valueOf(i + 1), '0', 5, false))
			.append("82600"); 

		}
		return historyText;
	}

	public static Uri saveDTAFile(String history) {
		File bsRoot = new File(Environment.getExternalStorageDirectory(), "ESRScan");
		File historyRoot = new File(bsRoot, "DTA");
		if (!historyRoot.exists() && !historyRoot.mkdirs()) {
			Log.w(TAG, "Couldn't make dir " + historyRoot);
			return null;
		}
		File historyFile = new File(historyRoot, "DTA-" + System.currentTimeMillis() + ".001");
		OutputStreamWriter out = null;
		try {
			out = new OutputStreamWriter(new FileOutputStream(historyFile), Charset.forName("ISO-8859-1"));
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
	
	private static String getDateFormated(Date date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
		return sdf.format(date);
	}
	
	private String getExecutionDateFormated(){
		Date now = new Date(System.currentTimeMillis());
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		String day = prefs.getString(PreferencesActivity.KEY_EXECUTION_DAY, "26");

		Calendar nowCalendar = Calendar.getInstance();
		nowCalendar.setTime(now);

		Calendar expectedCalendar = Calendar.getInstance();
		expectedCalendar.setTime(now);
		expectedCalendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(day));

		if(expectedCalendar.before(nowCalendar)){
			int month = expectedCalendar.get(Calendar.MONTH);

			if(month < Calendar.DECEMBER){
				expectedCalendar.set(Calendar.MONTH, month + 1);
			}
			else{
				expectedCalendar.set(Calendar.MONTH, Calendar.JANUARY);
				expectedCalendar.set(Calendar.YEAR, (expectedCalendar.get(Calendar.YEAR) + 1));
			}
		}

		return sdf.format(expectedCalendar.getTime());
	}

	  private static String nullToEmpty(String value) {
	    return value == null ? "" : value;
	  }
	  
	  private static CharSequence spacePaddedEnd(String text, int length){
			return padded(text, ' ', length, true);
	  }
	  
	  private static CharSequence padded(String text, char pad, int length, boolean padEnd){
			if(text.length() > length){
				return text.subSequence(0, length);
			}
			
			StringBuilder paddedText = new StringBuilder(length);
			
			if(padEnd){
			paddedText.append(text);
			}
			
			for(int i = 0; i < length - text.length(); i++){
				paddedText.append(pad);
			}
			
			if(!padEnd){
				paddedText.append(text);
			}
			
			return paddedText;
	  }
}
