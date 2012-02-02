package ch.luklanis.esscan.paymentslip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ch.luklanis.esscan.PreferencesActivity;
import ch.luklanis.esscan.R;
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
		StringBuilder dtaText = new StringBuilder(1000);
		
		String today = getDateFormated(new Date(System.currentTimeMillis()));
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		
		String iban = prefs.getString(PreferencesActivity.KEY_IBAN, "").replaceAll("\\s", "");
		
		String[] ownAddress = prefs.getString(PreferencesActivity.KEY_ADDRESS, "").split("\\r\\n");
		
		float totalAmount = 0;

		if(iban == ""){
			return "";
		}
		
		String clearing = String.valueOf((Integer.parseInt(iban.substring(4, 9))));
		
		List<EsrResult> esrResults = new ArrayList<EsrResult>();
		
		for (HistoryItem historyItem : historyItems) {
			if(historyItem.getResult().getCurrency() == "CHF"){
				esrResults.add(historyItem.getResult());
			}
		}

		for (int i = 0; i < esrResults.size(); i++) {
			EsrResult esrResult = esrResults.get(i);
			
			String currency = esrResult.getCurrency();
			
			String account = esrResult.getAccount().replaceAll("-", "");
			
			String[] address = esrResult.getAddress() != null ? esrResult.getAddress().split("\\r\\n"):new String[0];
			
			CharSequence paddedSequenz = padded(String.valueOf(i + 1), '0', 5, false);
			
			String amount = esrResult.getAmount().replace('.', ',');
			
			totalAmount += Float.parseFloat(esrResult.getAmount());

			// HEADER for ESR
			dtaText
			.append("01")	// Segment number
			.append(getExecutionDateFormated())	// desired execution date
			.append(spacePaddedEnd("", 12))	// Clearing number of the target bank (not needed for ESR payments)
			.append(padded("", '0', 5, true))	// Sequenz number (has to be 5 x 0)
			.append(nullToEmpty(today))	// creation date
			.append(spacePaddedEnd(clearing, 7))	// own clearing number
			.append(padded("", 'X', 5, true))	// identification number
			.append(paddedSequenz)	// sequenz number
			.append("82600");	// transaction type (ESR = 826), payment type (ESR = 0) and a flag (always 0)
			
			// ESR
			dtaText
			.append(padded("", 'X', 5, true))	// identification number (again)
			.append("WZ0000")	// transaction number part 1
			.append(paddedSequenz)	// transaction number part 2
			.append(spacePaddedEnd(iban, 24))	// own IBAN
			.append(spacePaddedEnd("", 6))	// Valuta (Blanks in ESR)
			.append(currency)
			.append(spacePaddedEnd(amount, 12))
			.append(spacePaddedEnd("", 14))	// Reserve
			.append("02")	// Begin Segment 02
			.append(spacePaddedEnd(ownAddress[0], 20))
			.append(spacePaddedEnd(ownAddress[1], 20));
			
			if(ownAddress.length > 2){
				dtaText.append(spacePaddedEnd(ownAddress[2], 20));
			}
			if(ownAddress.length > 3){
				dtaText.append(spacePaddedEnd(ownAddress[3], 20));
			}
			dtaText.append(spacePaddedEnd("", 46));	// Reserve
			
			dtaText
			.append("03")	// Begin Segment 03
			.append("/C/")	// Account begin
			.append(padded(account, '0', 9, false));	// Account
			
			if(address.length > 0){
				dtaText.append(spacePaddedEnd(address[0], 20));
			}
			if(address.length > 1){
				dtaText.append(spacePaddedEnd(address[1], 20));
			}
			if(address.length > 2){
				dtaText.append(spacePaddedEnd(address[2], 20));
			}
			if(address.length > 3){
				dtaText.append(spacePaddedEnd(address[3], 20));
			}
			
			if(account.length() > 5){
				dtaText.append(padded(esrResult.getReference(), '0', 27, false));	// Referenz number
			}
			else{
				dtaText.append(spacePaddedEnd(esrResult.getReference(), 27));	// Refernz number (with 5 digits account)
				Log.w(TAG, "account only 5 digits long -> this will not work!");
			}
			
			dtaText.append("  ")	// ESR Checksum (only with 5 digits, which is not supported)
			.append(spacePaddedEnd("", 5));	// Reserve
		}
		
		// HEADER for Total Record
		dtaText
		.append("01")	// Segment number
		.append(getExecutionDateFormated())	// desired execution date
		.append(spacePaddedEnd("", 12))	// Clearing number of the target bank (not needed for ESR payments)
		.append(padded("", '0', 5, true))	// Sequenz number (has to be 5 x 0)
		.append(nullToEmpty(today))	// creation date
		.append(spacePaddedEnd(clearing, 7))	// own clearing number
		.append(padded("", 'X', 5, true))	// identification number
		.append(padded(String.valueOf(esrResults.size() + 1), '0', 5, false))	// sequenz number
		.append("89000");	// transaction type (Total Record = 890), payment type (ESR = 0) and a flag (always 0)
		
		String totalAmountTemp = String.valueOf(totalAmount).replace('.', ',');
		int indexOfDecimal = totalAmountTemp.indexOf(',');
		totalAmountTemp = totalAmountTemp.substring(indexOfDecimal + 1, indexOfDecimal + 3);
		
		// Total Record
		dtaText
		.append(spacePaddedEnd(totalAmountTemp, 16))
		.append(spacePaddedEnd("", 59)); // Reserve
		
		return dtaText;
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

		if(!expectedCalendar.after(nowCalendar)){
//			int month = expectedCalendar.get(Calendar.MONTH);
//
//			if(month < Calendar.DECEMBER){
//				expectedCalendar.set(Calendar.MONTH, month + 1);
//			}
//			else{
//				expectedCalendar.set(Calendar.MONTH, Calendar.JANUARY);
//				expectedCalendar.set(Calendar.YEAR, (expectedCalendar.get(Calendar.YEAR) + 1));
//			}
			expectedCalendar.add(Calendar.MONTH, 1);
		}
		
		int dayOfWeek = expectedCalendar.get(Calendar.DAY_OF_WEEK);
		
		switch(dayOfWeek){
		case Calendar.SATURDAY:
			expectedCalendar.add(Calendar.DAY_OF_WEEK, 2);
			break;
		case Calendar.SUNDAY:
			expectedCalendar.add(Calendar.DAY_OF_WEEK, 1);
			break;
		default:
			break;
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
