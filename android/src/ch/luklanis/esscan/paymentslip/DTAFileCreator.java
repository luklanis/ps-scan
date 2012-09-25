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

import ch.luklanis.esscan.CaptureActivity;
import ch.luklanis.esscan.PreferencesActivity;
import ch.luklanis.esscan.R;
import ch.luklanis.esscan.history.HistoryItem;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class DTAFileCreator {

	private static final String TAG = DTAFileCreator.class.getName();
	private static final String NEWLINE_PATTERN = "[\\r\\n]+";
	private static final String SPACE_PATTERN = "\\s";
	private Context context;
	private String fileName;
	private File historyFile;
	private File historyRoot;

	public DTAFileCreator(Context context){
		this.context = context;

		File bsRoot = new File(Environment.getExternalStorageDirectory(), CaptureActivity.EXTERNAL_STORAGE_DIRECTORY);
		
		historyRoot = new File(bsRoot, "DTA");
		fileName = "DTA-" + System.currentTimeMillis() + ".001";
		historyFile = new File(historyRoot, fileName);
	}
	
	public Uri getDTAFileUri() {
		return Uri.parse("file://" + historyFile.getAbsolutePath());
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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		String iban = prefs.getString(PreferencesActivity.KEY_IBAN, "").replaceAll(SPACE_PATTERN, "");

		String[] ownAddress = prefs.getString(PreferencesActivity.KEY_ADDRESS, "").split(NEWLINE_PATTERN);

		float totalAmount = 0;

		if(iban == ""){
			return "";
		}

		String clearing = String.valueOf((Integer.parseInt(iban.substring(4, 9))));

		List<HistoryItem> filteredHistoryItem = new ArrayList<HistoryItem>();

		for (HistoryItem historyItem : historyItems) {
			
			if (!historyItem.getResult().getType().equals("orange")) {
				continue;
			}
			
			EsrResult result = new EsrResult(historyItem.getResult().getCompleteCode());
			
			if(result.getCurrency() == "CHF"){
				filteredHistoryItem.add(historyItem);
			}
		}

		for (int i = 0; i < filteredHistoryItem.size(); i++) {			
			EsrResult esrResult = new EsrResult(filteredHistoryItem.get(i).getResult().getCompleteCode());

			String currency = esrResult.getCurrency();

			String account = esrResult.getAccountUnformated();

			String addressLine = filteredHistoryItem.get(i).getAddress();
			String[] address = addressLine != null 
					? addressLine.split(NEWLINE_PATTERN)
							: new String[0];

					CharSequence paddedSequenz = padded(String.valueOf(i + 1), '0', 5, false);

					String amount = filteredHistoryItem.get(i).getAmount();

					totalAmount += Float.parseFloat(amount);

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
					.append(spacePaddedEnd(amount.replace('.', ','), 12))
					.append(spacePaddedEnd("", 14))	// Reserve
					.append("02")	// Begin Segment 02
					.append(spacePaddedEnd(ownAddress[0], 20))
					.append(spacePaddedEnd(ownAddress[1], 20));

					String ownAddressTemp = "";
					if(ownAddress.length > 2){
						ownAddressTemp += spacePaddedEnd(ownAddress[2], 20);
					}
					if(ownAddress.length > 3){
						ownAddressTemp += spacePaddedEnd(ownAddress[3], 20);
					}
					
					dtaText
					.append(spacePaddedEnd(ownAddressTemp, 40))
					.append(spacePaddedEnd("", 46));	// Reserve

					dtaText
					.append("03")	// Begin Segment 03
					.append("/C/")	// Account begin
					.append(padded(account, '0', 9, false));	// Account

					String addressTemp = "";
					if(address.length > 0){
						addressTemp += spacePaddedEnd(address[0], 20);
					}
					if(address.length > 1){
						addressTemp += spacePaddedEnd(address[1], 20);
					}
					if(address.length > 2){
						addressTemp += spacePaddedEnd(address[2], 20);
					}
					if(address.length > 3){
						addressTemp += spacePaddedEnd(address[3], 20);
					}
					
					dtaText.append(spacePaddedEnd(addressTemp, 80));

					String referenzNumber = esrResult.getReference().replaceAll(SPACE_PATTERN, "");
					if(account.length() > 5){
						dtaText.append(padded(referenzNumber, '0', 27, false));	// Referenz number
					}
					else{
						dtaText.append(spacePaddedEnd(referenzNumber, 27));	// Refernz number (with 5 digits account)
						Log.w(TAG, "account only 5 digits long -> this will not work!");
					}

					dtaText.append("  ")	// ESR Checksum (only with 5 digits, which is not supported)
					.append(spacePaddedEnd("", 5));	// Reserve
					
					filteredHistoryItem.get(i).setExported(true);
		}

		// HEADER for Total Record
		dtaText
		.append("01")	// Segment number
		.append(padded("", '0', 6, true))	// desired execution date (not set in Total Record)
		.append(spacePaddedEnd("", 12))	// Clearing number of the target bank (not needed for ESR payments)
		.append(padded("", '0', 5, true))	// Sequenz number (has to be 5 x 0)
		.append(nullToEmpty(today))	// creation date
		.append(spacePaddedEnd("", 7))	// own clearing number (not set in Total Record)
		.append(padded("", 'X', 5, true))	// identification number
		.append(padded(String.valueOf(filteredHistoryItem.size() + 1), '0', 5, false))	// sequenz number
		.append("89000");	// transaction type (Total Record = 890), payment type (ESR = 0) and a flag (always 0)

		String[] totalAmountSplit = String.valueOf(totalAmount).split("\\.");
		
		String totalAmountTemp = totalAmountSplit[0] + "," + padded(totalAmountSplit[1], '0', 3, true);

		// Total Record
		dtaText
		.append(spacePaddedEnd(totalAmountTemp, 16))
		.append(spacePaddedEnd("", 59)); // Reserve

		return dtaText;
	}

	public boolean saveDTAFile(String dta) {
		if (!historyRoot.exists() && !historyRoot.mkdirs()) {
			Log.w(TAG, "Couldn't make dir " + historyRoot);
			return false;
		}
		
		OutputStreamWriter out = null;
		try {
			out = new OutputStreamWriter(new FileOutputStream(historyFile), Charset.forName("ISO-8859-1"));
			out.write(dta);
			return true;
		} catch (IOException ioe) {
			Log.w(TAG, "Couldn't access file " + historyFile + " due to " + ioe);
			return false;
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
	
	public static int validateAddress(String address){
		if(address.length() > 0){
			String[] lines = address.split("[\\r\\n]+");

			if(lines.length > 4){
				return R.string.msg_address_line_to_long;
			}
			else{
				for (String line : lines) {
					if(line.length() > 20){
						return R.string.msg_address_line_to_long;
					}
				}
			}
		}
		
		return 0;
	}
	
	public static int validateIBAN(String iban){
    	iban = iban.replaceAll("[\\s\\r\\n]+", "");
    	
    	if(iban == ""){
    		return R.string.msg_own_iban_is_not_valid;
    	}
    	
    	if(iban.length() != 21){
    		return R.string.msg_own_iban_is_not_valid;
    	}
    	
    	iban = iban.substring(4, 21) + iban.substring(0, 4);

		StringBuilder ibanNumber = new StringBuilder(1000);
		
		for(int i=0; i<iban.length();i++){
			char ibanChar = iban.charAt(i);
			
			if(ibanChar < '0' || ibanChar > '9') {				
				int ibanLetter = 10 + (ibanChar - 'A');

				if(ibanLetter < 10 || ibanLetter > (('Z' - 'A') + 10)) {
		    		return R.string.msg_own_iban_is_not_valid;
				}
				
				ibanNumber.append(ibanLetter);
			}
			else{
				ibanNumber.append(ibanChar);
			}
		}
		
		int lastEnd = 0;
		int subIbanLength = 9;
		int subIbanLengthWithModulo = subIbanLength - 2;
		int modulo97 = 97;
		
		int subIban = Integer.parseInt(ibanNumber.substring(lastEnd, subIbanLength));
		int lastModulo = subIban % modulo97;
		lastEnd = subIbanLength;
		
		try {
			while(lastEnd < ibanNumber.length()){
				if((lastEnd + subIbanLengthWithModulo) < ibanNumber.length()){
					int newEnd = lastEnd + subIbanLengthWithModulo;
					subIban = Integer.parseInt(String.format("%s%s", lastModulo, ibanNumber.substring(lastEnd, newEnd)));
					lastEnd = newEnd;
				}
				else{
					subIban = Integer.parseInt(String.format("%s%s", lastModulo, ibanNumber.substring(lastEnd)));
					lastEnd = ibanNumber.length();
				}

				lastModulo = subIban % modulo97;
			}
		} catch (NumberFormatException ex) {
    		return R.string.msg_own_iban_is_not_valid;
		}
		
		if(lastModulo != 1){
    		return R.string.msg_own_iban_is_not_valid;
		}
		
		return 0;
	}

	public int getFirstErrorId() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		String iban = prefs.getString(PreferencesActivity.KEY_IBAN, "").replaceAll("\\s", "");

		if(TextUtils.isEmpty(iban)){
			return R.string.msg_own_iban_is_not_set;
		}

		if(validateIBAN(iban) != 0){
			return R.string.msg_own_iban_is_not_valid;
		}

		String[] ownAddress = prefs.getString(PreferencesActivity.KEY_ADDRESS, "").split(NEWLINE_PATTERN);

		if(ownAddress.length < 2){
			return R.string.msg_own_address_is_not_set;
		}

		return 0;
	}

	public String getFirstError(List<HistoryItem> historyItems){
		int error = getFirstErrorId();
		
		Resources res = context.getResources();

		if(error != 0){
			return res.getString(error);
		}
		
		if (historyItems != null) {
			List<HistoryItem> items = new ArrayList<HistoryItem>();
			
			boolean nothingToExport = true;
	
			for (HistoryItem historyItem : historyItems) {
				
				if (!historyItem.getResult().getType().equals("orange")) {
					continue;
				}
				
				EsrResult result = new EsrResult(historyItem.getResult().getCompleteCode());
				
				if(result.getCurrency() == "CHF"){
					items.add(historyItem);
				}
				
				if(historyItem.getDTAFilename() == null) {
					nothingToExport = false;
				}
			}
			
			if (nothingToExport) {
				return res.getString(R.string.msg_nothing_to_export);
			}
	
			for (int i = 0; i < items.size(); i++) {
				HistoryItem item = items.get(i);
	
				if(nullToEmpty(item.getAmount()).equals("")){
					return res.getString(R.string.msg_amount_is_empty, item.getResult().getAccount());
				}
			}
		}

		return "";
	}

	private static String getDateFormated(Date date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
		return sdf.format(date);
	}

	private String getExecutionDateFormated(){
		Date now = new Date(System.currentTimeMillis());
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
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
