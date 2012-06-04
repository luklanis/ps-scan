/*
 * Copyright (C) 2008 ZXing authors
 * Copyright 2011 Robert Theis
 * Copyright 2012 Lukas Landis
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

package ch.luklanis.esscanlite;


import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.googlecode.tesseract.android.TessBaseAPI;

import ch.luklanis.esscan.camera.CameraManager;
import ch.luklanis.esscanlite.R;
import ch.luklanis.esscan.language.LanguageCodeHelper;
import ch.luklanis.esscan.paymentslip.DTAFileCreator;
import ch.luklanis.esscan.paymentslip.EsrResult;
import ch.luklanis.esscan.paymentslip.EsrValidation;
import ch.luklanis.esscan.paymentslip.PsValidation;
import ch.luklanis.esscanlite.BeepManager;
import ch.luklanis.esscanlite.HelpActivity;
import ch.luklanis.esscanlite.OcrResult;
import ch.luklanis.esscanlite.PreferencesActivity;
import ch.luklanis.esscanlite.history.HistoryActivity;
import ch.luklanis.esscanlite.history.HistoryItem;
import ch.luklanis.esscanlite.history.HistoryManager;

import android.app.AlertDialog;
import android.app.ProgressDialog;
//import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
//import android.content.ClipboardManager;
import android.text.ClipboardManager;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 * 
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing/
 */
public final class CaptureActivity extends SherlockActivity implements SurfaceHolder.Callback {

	private static final String TAG = CaptureActivity.class.getSimpleName();

	/** The default OCR engine to use. */
	public static final String DEFAULT_OCR_ENGINE_MODE = "Tesseract";

	/** Languages for which Cube data is available. */
	static final String[] CUBE_SUPPORTED_LANGUAGES = { 
		"ara", // Arabic
		"eng", // English
		"hin" // Hindi
	};

	/** Languages that require Cube, and cannot run using Tesseract. */
	private static final String[] CUBE_REQUIRED_LANGUAGES = { 
		"ara" // Arabic
	};

	/** Resource to use for data file downloads. */
	static final String DOWNLOAD_BASE = "http://tesseract-ocr.googlecode.com/files/";

	/** Download filename for orientation and script detection (OSD) data. */
	static final String OSD_FILENAME = "tesseract-ocr-3.01.osd.tar";

	/** Destination filename for orientation and script detection (OSD) data. */
	static final String OSD_FILENAME_BASE = "osd.traineddata";

	/** Minimum mean confidence score necessary to not reject single-shot OCR result. Currently unused. */
	static final int MINIMUM_MEAN_CONFIDENCE = 0; // 0 means don't reject any scored results

	/** Length of time before the next autofocus request, if the last one was successful. Used in CaptureActivityHandler. */
	static final long AUTOFOCUS_SUCCESS_INTERVAL_MS = 3000L;

	/** Length of time before the next autofocus request, if the last request failed. Used in CaptureActivityHandler. */
	static final long AUTOFOCUS_FAILURE_INTERVAL_MS = 1000L;

	// Options menu, for copy to clipboard
	private static final int OPTIONS_COPY_RECOGNIZED_TEXT_ID = Menu.FIRST;

	private static final int OPTIONS_SHARE_RECOGNIZED_TEXT_ID = Menu.FIRST + 1;

	public static final int HISTORY_REQUEST_CODE = 0x0000bacc;

	private CameraManager cameraManager;
	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private TextView statusViewBottom;
	//private TextView statusViewTop;
	private View statusViewTop;
	private TextView ocrResultView;
	private View resultView;
	private HistoryItem lastItem;
	private boolean hasSurface;
	private BeepManager beepManager;
	private TessBaseAPI baseApi; // Java interface for the Tesseract OCR engine
	private String sourceLanguageCodeOcr; // ISO 639-3 language code
	private String sourceLanguageReadable; // Language name, for example, "English"
	private int pageSegmentationMode = TessBaseAPI.PSM_SINGLE_LINE;
	private int ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
	private String characterWhitelist;

	private SharedPreferences prefs;
	private OnSharedPreferenceChangeListener listener;
	private ProgressDialog dialog; // for initOcr - language download & unzip
	private ProgressDialog indeterminateDialog; // also for initOcr - init OCR engine
	private boolean isEngineReady;
	private boolean isPaused;
	private static boolean isFirstLaunch; // True if this is the first time the app is being run
	private HistoryManager historyManager;

	private PsValidation psValidation;

	private int lastValidationStep;

	private boolean showOcrResult;

	Handler getHandler() {
		return handler;
	}

	CameraManager getCameraManager() {
		return cameraManager;
	}

	public PsValidation getValidation(){
		return psValidation;
	}
	
	private final Button.OnClickListener resultCopyListener = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			clipboardManager.setText(lastItem.getResult().getCompleteCode());

			//        clipboardManager.setPrimaryClip(ClipData.newPlainText("ocrResult", ocrResultView.getText()));
			//      if (clipboardManager.hasPrimaryClip()) {
			if (clipboardManager.hasText()) {
				Toast toast = Toast.makeText(v.getContext(), R.string.msg_copied, Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.BOTTOM, 0, 0);
				toast.show();
			}
		}
	};
	
	private final Button.OnClickListener exportAgainListener = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
			builder.setMessage(R.string.msg_sure);
			builder.setNeutralButton(R.string.button_cancel, null);
			builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					historyManager.updateHistoryItemFileName(lastItem.getResult().getCompleteCode(), null);

					Button reexportButton = (Button) findViewById(R.id.button_export_again);

					TextView dtaFilenameTextView = (TextView) findViewById(R.id.esr_result_dta_file);
					dtaFilenameTextView.setText("");
					reexportButton.setVisibility(View.GONE);
				}
			});
			builder.show();
		}
	};
	
	private final Button.OnClickListener saveListener = new Button.OnClickListener() {
		public void onClick(View v) {
			boolean somethingSaved = false;

			if(lastItem.getResult().getAmount() == ""){
				EditText amountEditText = (EditText) findViewById(R.id.esr_result_amount);
				String newAmount = amountEditText.getText().toString().replace(',', '.');
				try {
					float newAmountTemp = Float.parseFloat(newAmount);
					newAmountTemp *= 100;
					newAmountTemp -= (newAmountTemp % 5);
					newAmountTemp /= 100;

					newAmount = String.valueOf(newAmountTemp);

					if(newAmount.indexOf('.') == newAmount.length() - 2){
						newAmount += "0";
					}

					if(historyManager == null){
						Log.e(TAG, "onClick: historyManager is null!");
						return;
					}

					historyManager.updateHistoryItemAmount(lastItem.getResult().getCompleteCode(), 
							newAmount);
					amountEditText.setText(newAmount);

					somethingSaved = true;
				} catch (NumberFormatException e) {
					setOKAlert(v.getContext(), R.string.msg_amount_not_valid);
				}
			}

			EditText addressEditText = (EditText) findViewById(R.id.esr_result_address);
			String address = addressEditText.getText().toString();
			if(address.length() > 0){

				int error = DTAFileCreator.validateAddress(address);
				if(error != 0){
					setOKAlert(v.getContext(), error);
				}

				if (lastItem.getAddressNumber() == -1) {
					historyManager.updateHistoryItemAddress(lastItem.getResult().getCompleteCode(), 
							historyManager.addAddress(lastItem.getResult().getAccount(), address));
				}
				else{
					historyManager.updateAddress(lastItem.getResult().getAccount(), 
							lastItem.getAddressNumber(), address);
				}

				somethingSaved = true;
			}

			if(somethingSaved){
				Toast toast = Toast.makeText(v.getContext(), R.string.msg_saved, Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.BOTTOM, 0, 0);
				toast.show();
			}
		}
	};
	
	private final Button.OnClickListener addressChangeListener = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			showAddressDialog(v.getContext());
		}
	};

	@Override
	public void onCreate(Bundle icicle) {
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(icicle);

		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.capture);
		
		//Load partially transparent black background
//        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));  
		
		// Hide Icon in ActionBar
		getSupportActionBar().setDisplayShowHomeEnabled(false);
		
		showOcrResult = false;
		
		hasSurface = false;
		historyManager = new HistoryManager(this);
	    historyManager.trimHistory();
	    
		beepManager = new BeepManager(this);

		Button resultCopy = (Button)findViewById(R.id.button_copy_code_row);
		resultCopy.setOnClickListener(resultCopyListener);

		Button exportAgainButton = (Button)findViewById(R.id.button_export_again);
		exportAgainButton.setOnClickListener(exportAgainListener);

		Button addressChangeButton = (Button)findViewById(R.id.button_address_change);
		addressChangeButton.setOnClickListener(addressChangeListener);

		Button amountSaveButton = (Button) findViewById(R.id.button_result_save);
		amountSaveButton.setOnClickListener(saveListener);

		showHelpOnFirstLaunch();
	}

	@Override
	protected void onResume() {
		super.onResume();   

		cameraManager = new CameraManager(this);

		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		viewfinderView.setCameraManager(cameraManager);
		
		resultView = findViewById(R.id.result_view);
		statusViewTop = findViewById(R.id.status_view_top);

		statusViewBottom = (TextView) findViewById(R.id.status_view_bottom);
		registerForContextMenu(statusViewBottom);

		psValidation = new EsrValidation();
		this.lastValidationStep = psValidation.getCurrentStep();

		isEngineReady = false;

		String previousSourceLanguageCodeOcr = sourceLanguageCodeOcr;
		int previousOcrEngineMode = ocrEngineMode;

		handler = null;

		retrievePreferences();
		
		resetStatusView();
		psValidation.gotoBeginning();
		this.lastValidationStep = psValidation.getCurrentStep();

		// Set up the camera preview surface.
		surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		surfaceHolder = surfaceView.getHolder();
		if (!hasSurface) {
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		// Comment out the following block to test non-OCR functions without an SD card

		// Do OCR engine initialization, if necessary
		boolean doNewInit = (baseApi == null) || !sourceLanguageCodeOcr.equals(previousSourceLanguageCodeOcr) || 
				ocrEngineMode != previousOcrEngineMode;
		if (doNewInit) {      
			// Initialize the OCR engine
			File storageDirectory = getStorageDirectory();
			if (storageDirectory != null) {
				initOcrEngine(storageDirectory, sourceLanguageCodeOcr, sourceLanguageReadable);
			}
		} else {
			// We already have the engine initialized, so just start the camera.
			resumeOCR();
		}
	}

	/** 
	 * Method to start or restart recognition after the OCR engine has been initialized,
	 * or after the app regains focus. Sets state related settings and OCR engine parameters,
	 * and requests camera initialization.
	 */
	void resumeOCR() {
		Log.d(TAG, "resumeOCR()");

		// This method is called when Tesseract has already been successfully initialized, so set 
		// isEngineReady = true here.
		isEngineReady = true;

		isPaused = false;

		if (handler != null) {
			handler.resetState();
		}
		if (baseApi != null) {
			baseApi.setPageSegMode(pageSegmentationMode);
			baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "");
			baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, characterWhitelist);
		}

		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		}
	}

	/** Called when the shutter button is pressed in continuous mode. */
	void onShutterButtonPressContinuous() {
		isPaused = true;
		handler.stop();  
		beepManager.playBeepSoundAndVibrate();
		if (lastItem != null) {
			showResult(lastItem);
		} else {
			Toast toast = Toast.makeText(this, "OCR failed. Please try again.", Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.TOP, 0, 0);
			toast.show();
			resumeContinuousDecoding();
		}
	}

	/** Called to resume recognition after translation in continuous mode. */
	void resumeContinuousDecoding() {
		isPaused = false;
		resetStatusView();
		setStatusViewForContinuous();
		handler.resetState();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated()");

		if (holder == null) {
			Log.e(TAG, "surfaceCreated gave us a null surface");
		}

		// Only initialize the camera if the OCR engine is ready to go.
		if (!hasSurface && isEngineReady) {
			Log.d(TAG, "surfaceCreated(): calling initCamera()...");
			initCamera(holder);
		}
		hasSurface = true;
	}

	/** Initializes the camera and starts the handler to begin previewing. */
	private void initCamera(SurfaceHolder surfaceHolder) {
		Log.d(TAG, "initCamera()");
		try {

			// Open and initialize the camera
			cameraManager.openDriver(surfaceHolder);

			// Creating the handler starts the preview, which can also throw a RuntimeException.
			handler = new CaptureActivityHandler(this, cameraManager, baseApi);

		} catch (IOException ioe) {
			showErrorMessage("Error", "Could not initialize camera. Please try restarting device.");
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			showErrorMessage("Error", "Could not initialize camera. Please try restarting device.");
		}   
	}

	@Override
	protected void onPause() {
		if (handler != null) {
			handler.quitSynchronously();
		}

		// Stop using the camera, to avoid conflicting with other camera-based apps
		cameraManager.closeDriver();

		if (!hasSurface) {
			SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
			SurfaceHolder surfaceHolder = surfaceView.getHolder();
			surfaceHolder.removeCallback(this);
		}
		super.onPause();
	}

	void stopHandler() {
		if (handler != null) {
			handler.stop();
		}
	}

	@Override
	protected void onDestroy() {
		if (baseApi != null) {
			baseApi.end();
		}
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {

			// First check if we're paused in continuous mode, and if so, just unpause.
			if (isPaused) {
				Log.d(TAG, "only resuming continuous recognition, not quitting...");
				psValidation.gotoBeginning();
				this.lastValidationStep = psValidation.getCurrentStep();
				resumeContinuousDecoding();
				return true;
			}

			// Exit the app if we're not viewing a result.
			if (lastItem == null) {
				setResult(RESULT_CANCELED);
				finish();
				return true;
			} else {
				// Go back to previewing in regular OCR mode.
				resetStatusView();
				if (handler != null) {
					handler.sendEmptyMessage(R.id.restart_preview);
				}
				return true;
			}
		} 
		
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.capture_menu, menu);
		//		super.onCreateOptionsMenu(menu);
		//		menu.add(0, SETTINGS_ID, 0, "Settings").setIcon(android.R.drawable.ic_menu_preferences);
		//		menu.add(0, HISTORY_ID, 0, "History").setIcon(android.R.drawable.ic_menu_recent_history);
		//		menu.add(0, ABOUT_ID, 0, "About").setIcon(android.R.drawable.ic_menu_info_details);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.menu_settings: {
			intent = new Intent().setClass(this, PreferencesActivity.class);
			startActivity(intent);
			break;
		}
		case R.id.menu_history: {
			intent = new Intent(Intent.ACTION_PICK);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.setClassName(this, HistoryActivity.class.getName());
			startActivityForResult(intent, HISTORY_REQUEST_CODE);
			break;
		}
		case R.id.menu_help: {
			intent = new Intent(this, HelpActivity.class);
			intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, HelpActivity.DEFAULT_PAGE);
			startActivity(intent);
			break;
		}
		case R.id.menu_about: {
			intent = new Intent(this, HelpActivity.class);
			intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, HelpActivity.ABOUT_PAGE);
			startActivity(intent);
			break;
		}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(resultCode == RESULT_OK && requestCode == HISTORY_REQUEST_CODE){
			int position = intent.getIntExtra(Intents.History.ITEM_NUMBER, -1);

			HistoryItem item = historyManager.buildHistoryItem(position);

			Message message = Message.obtain(handler, R.id.esr_show_history_item, item);
			message.sendToTarget();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	/** Sets the necessary language code values for the given OCR language. */
	private boolean setSourceLanguage(String languageCode) {
		sourceLanguageCodeOcr = languageCode;
		sourceLanguageReadable = LanguageCodeHelper.getOcrLanguageName(this, languageCode);
		return true;
	}

	/** Finds the proper location on the SD card where we can save files. */
	private File getStorageDirectory() {
		//Log.d(TAG, "getStorageDirectory(): API level is " + Integer.valueOf(android.os.Build.VERSION.SDK_INT));

		String state = null;
		try {
			state = Environment.getExternalStorageState();
		} catch (RuntimeException e) {
			Log.e(TAG, "Is the SD card visible?", e);
			showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable.");
		}

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

			// We can read and write the media
			//    	if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) > 7) {
			// For Android 2.2 and above

			try {
				return getExternalFilesDir(Environment.MEDIA_MOUNTED);
			} catch (NullPointerException e) {
				// We get an error here if the SD card is visible, but full
				Log.e(TAG, "External storage is unavailable");
				showErrorMessage("Error", "Required external storage (such as an SD card) is full or unavailable.");
			}

			//        } else {
			//          // For Android 2.1 and below, explicitly give the path as, for example,
			//          // "/mnt/sdcard/Android/data/ch.luklanis.esscan/files/"
			//          return new File(Environment.getExternalStorageDirectory().toString() + File.separator + 
			//                  "Android" + File.separator + "data" + File.separator + getPackageName() + 
			//                  File.separator + "files" + File.separator);
			//        }

		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			Log.e(TAG, "External storage is read-only");
			showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable for data storage.");
		} else {
			// Something else is wrong. It may be one of many other states, but all we need
			// to know is we can neither read nor write
			Log.e(TAG, "External storage is unavailable");
			showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable.");
		}
		return null;
	}

	/**
	 * Requests initialization of the OCR engine with the given parameters.
	 * 
	 * @param storageRoot Path to location of the tessdata directory to use
	 * @param languageCode Three-letter ISO 639-3 language code for OCR 
	 * @param languageName Name of the language for OCR, for example, "English"
	 */
	private void initOcrEngine(File storageRoot, String languageCode, String languageName) {    
		isEngineReady = false;

		// Set up the dialog box for the thermometer-style download progress indicator
		if (dialog != null) {
			dialog.dismiss();
		}
		dialog = new ProgressDialog(this);

		// If we have a language that only runs using Cube, then set the ocrEngineMode to Cube
		if (ocrEngineMode != TessBaseAPI.OEM_CUBE_ONLY) {
			for (String s : CUBE_REQUIRED_LANGUAGES) {
				if (s.equals(languageCode)) {
					ocrEngineMode = TessBaseAPI.OEM_CUBE_ONLY;
				}
			}
		}

		// If our language doesn't support Cube, then set the ocrEngineMode to Tesseract
		if (ocrEngineMode != TessBaseAPI.OEM_TESSERACT_ONLY) {
			boolean cubeOk = false;
			for (String s : CUBE_SUPPORTED_LANGUAGES) {
				if (s.equals(languageCode)) {
					cubeOk = true;
				}
			}
			if (!cubeOk) {
				ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
			}
		}

		// Display the name of the OCR engine we're initializing in the indeterminate progress dialog box
		indeterminateDialog = new ProgressDialog(this);
		indeterminateDialog.setTitle("Please wait");
		String ocrEngineModeName = getOcrEngineModeName();
		if (ocrEngineModeName.equals("Both")) {
			indeterminateDialog.setMessage("Initializing Cube and Tesseract OCR engines for " + languageName + "...");
		} else {
			indeterminateDialog.setMessage("Initializing " + ocrEngineModeName + " OCR engine for " + languageName + "...");
		}
		indeterminateDialog.setCancelable(false);
		indeterminateDialog.show();

		if (handler != null) {
			handler.quitSynchronously();     
		}

		// Start AsyncTask to install language data and init OCR
		baseApi = new TessBaseAPI();
		new OcrInitAsyncTask(this, baseApi, dialog, indeterminateDialog, languageCode, languageName, ocrEngineMode)
		.execute(storageRoot.toString());
	}

	/**
	 * Displays information relating to the result of OCR, and requests a translation if necessary.
	 * @param ocrResult Object representing successful OCR results
	 * @return True if a non-null result was received for OCR
	 */
	boolean showResult(EsrResult esrResult) {
		beepManager.playBeepSoundAndVibrate();

		HistoryItem historyItem = historyManager.addHistoryItem(esrResult);
		return showResult(historyItem);
	}

	/**
	 * Displays information relating to the result of OCR, and requests a translation if necessary.
	 * @param fromHistory 
	 * 
	 * @param ocrResult Object representing successful OCR results
	 * @return True if a non-null result was received for OCR
	 */
	boolean showResult(HistoryItem historyItem) {
		lastItem = historyItem;

		try {
			// Test whether the result is null
			lastItem.getResult().getCompleteCode();
		} catch (NullPointerException e) {
			Toast toast = Toast.makeText(this, "OCR failed. Please try again.", Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.TOP, 0, 0);
			toast.show();
			resumeContinuousDecoding();
			return false;
		}

		if(handler != null)
		{
			handler.stop();
			isPaused = true;
		}

		EsrResult result = historyItem.getResult();

		// Turn off capture-related UI elements
		statusViewBottom.setVisibility(View.GONE);
		statusViewTop.setVisibility(View.GONE);
		viewfinderView.setVisibility(View.GONE); 
		resultView.setVisibility(View.VISIBLE);

		ImageView bitmapImageView = (ImageView) findViewById(R.id.image_view);
		bitmapImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(),
				R.drawable.ez_or));

		// Display the recognized text
		//		TextView ocrResultTextView = (TextView) findViewById(R.id.ocr_result_text_view);
		//		ocrResultTextView.setText(esrResult.getCompleteCode());

		// Crudely scale betweeen 22 and 32 -- bigger font for shorter text
		//		int scaledSize = Math.max(14, 32 - esrResult.getCompleteCode().length() / 4);
		//		ocrResultTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);

		TextView accountTextView = (TextView) findViewById(R.id.esr_result_account);
		accountTextView.setText(result.getAccount());

		EditText amountEditText = (EditText) findViewById(R.id.esr_result_amount);
		Button amountSaveButton = (Button) findViewById(R.id.button_result_save);

		String amountFromCode = result.getAmount(); 
		if(amountFromCode != ""){
			amountEditText.setEnabled(false);
			amountEditText.setText(amountFromCode);
		}
		else{
			String amountManuel = lastItem.getAmount();
			if(amountManuel == null || amountManuel == "" || amountManuel.length() == 0){
				amountEditText.setText(R.string.esr_result_amount_not_set);
				amountEditText.selectAll();
			}
			else{
				amountEditText.setText(amountManuel);
			}
			amountSaveButton.setVisibility(View.VISIBLE);
		}

		TextView currencyTextView = (TextView) findViewById(R.id.esr_result_currency);
		currencyTextView.setText(result.getCurrency());

		TextView referenceTextView = (TextView) findViewById(R.id.esr_result_reference_number);
		referenceTextView.setText(result.getReference());

		String dtaFilename = lastItem.getDTAFilename();
		Button reexportButton = (Button) findViewById(R.id.button_export_again);
		TextView dtaFilenameTextView = (TextView) findViewById(R.id.esr_result_dta_file);

		if(dtaFilename != null && dtaFilename != ""){
			dtaFilenameTextView.setText(lastItem.getDTAFilename());
			reexportButton.setVisibility(View.VISIBLE);
		}
		else{
			reexportButton.setVisibility(View.GONE);
			dtaFilenameTextView.setText("");
		}

		Button addressChangeButton = (Button) findViewById(R.id.button_address_change);
		addressChangeButton.setVisibility(View.VISIBLE);

		EditText addressEditText = (EditText) findViewById(R.id.esr_result_address);
		addressEditText.setText("");

		if(lastItem.getAddressNumber() != -1){
			addressEditText.setText(lastItem.getAddress());
		}
		else{
			showAddressDialog(this);
		}

		return true;
	}

	private void showAddressDialog(Context context) {
		EsrResult result = lastItem.getResult();
		List<String> addresses = new ArrayList<String>();
		addresses.addAll(historyManager.getAddresses(result.getAccount()));
		addresses.add(getResources().getString(R.string.address_new));

		if(addresses.size() <= 1){		
			Button addressChangeButton = (Button)findViewById(R.id.button_address_change);
			addressChangeButton.setVisibility(View.GONE);
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.address_dialog_title);
		builder.setItems(addresses.toArray(new String[addresses.size()]), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				EditText addressEditText = (EditText) findViewById(R.id.esr_result_address);
				String address = historyManager.getAddress(lastItem.getResult().getAccount(), which);

				if(address != ""){
					historyManager.updateHistoryItemAddress(lastItem.getResult().getCompleteCode(), which);

					lastItem.setAddress(address);
					lastItem.setAddressNumber(which);
					addressEditText.setText(lastItem.getAddress());
					
					Toast toast = Toast.makeText(CaptureActivity.this, R.string.msg_saved, Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.BOTTOM, 0, 0);
					toast.show();
				}
				else{
					lastItem.setAddress("");
					lastItem.setAddressNumber(-1);
					addressEditText.setText(lastItem.getAddress());
				}
			}
		});
		builder.setNeutralButton(R.string.button_cancel, null);
		builder.show();
	}

	/**
	 * Displays information relating to the results of a successful real-time OCR request.
	 * 
	 * @param ocrResult Object representing successful OCR results
	 */
	void handleOcrContinuousDecode(OcrResult ocrResult) {

		// Send an OcrResultText object to the ViewfinderView for text rendering
		viewfinderView.addResultText(new OcrResultText(ocrResult.getText(), 
				ocrResult.getWordConfidences(),
				ocrResult.getMeanConfidence(),
				ocrResult.getBitmapDimensions(),
				ocrResult.getCharacterBoundingBoxes(),
				ocrResult.getWordBoundingBoxes(),
				ocrResult.getTextlineBoundingBoxes(),
				ocrResult.getRegionBoundingBoxes()));
		
		statusViewBottom.setText(ocrResult.getText());

		if(this.psValidation.getCurrentStep() != this.lastValidationStep){
			this.lastValidationStep = this.psValidation.getCurrentStep();
			beepManager.playBeepSoundAndVibrate();
			refreshStatusView();
		}
	}

	/**
	 * Version of handleOcrContinuousDecode for failed OCR requests. Displays a failure message.
	 * 
	 * @param obj Metadata for the failed OCR request.
	 */
	void handleOcrContinuousDecode(OcrResultFailure obj) {
		//		lastItem = null;
		Log.i(TAG, "handleOcrContinuousDecode: set lastItem to null");
	}

	/**
	 * Given either a Spannable String or a regular String and a token, apply
	 * the given CharacterStyle to the span between the tokens.
	 * 
	 * NOTE: This method was adapted from:
	 *  http://www.androidengineer.com/2010/08/easy-method-for-formatting-android.html
	 * 
	 * <p>
	 * For example, {@code setSpanBetweenTokens("Hello ##world##!", "##", new
	 * ForegroundColorSpan(0xFFFF0000));} will return a CharSequence {@code
	 * "Hello world!"} with {@code world} in red.
	 * 
	 */
	@SuppressWarnings("unused")
	private CharSequence setSpanBetweenTokens(CharSequence text, String token,
			CharacterStyle... cs) {
		// Start and end refer to the points where the span will apply
		int tokenLen = token.length();
		int start = text.toString().indexOf(token) + tokenLen;
		int end = text.toString().indexOf(token, start);

		if (start > -1 && end > -1) {
			// Copy the spannable string to a mutable spannable string
			SpannableStringBuilder ssb = new SpannableStringBuilder(text);
			for (CharacterStyle c : cs)
				ssb.setSpan(c, start, end, 0);
					text = ssb;
		}
		return text;
	}

	/**
	 * Resets view elements.
	 */
	private void resetStatusView() {
		resultView.setVisibility(View.GONE);

		refreshStatusView();
		statusViewTop.setVisibility(View.VISIBLE);
		
		statusViewBottom.setText("");
		
		if (showOcrResult) {
			statusViewBottom.setVisibility(View.VISIBLE);
		}

		viewfinderView.setVisibility(View.VISIBLE);

		lastItem = null;
		Log.i(TAG, "resetStatusView: set lastItem to null");
		viewfinderView.removeResultText();
	}

	private void refreshStatusView() {
		TextView statusView1 = (TextView) findViewById(R.id.status_view_1);
		TextView statusView2 = (TextView) findViewById(R.id.status_view_2);
		TextView statusView3 = (TextView) findViewById(R.id.status_view_3);

		statusView1.setBackgroundResource(0);
		statusView2.setBackgroundResource(0);
		statusView3.setBackgroundResource(0);

		switch (this.psValidation.getCurrentStep()) {
		case 1:
			statusView1.setBackgroundResource(R.drawable.status_view_background);
			break;
		case 2:
			statusView2.setBackgroundResource(R.drawable.status_view_background);
			break;
		case 3:
			statusView3.setBackgroundResource(R.drawable.status_view_background);
			break;

		default:
			break;
		}
	}

	/** Displays a pop-up message showing the name of the current OCR source language. */
	void showLanguageName() {   
		Toast toast = Toast.makeText(this, "OCR: " + sourceLanguageReadable, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.TOP, 0, 0);
		toast.show();
	}

	/**
	 * Displays an initial message to the user while waiting for the first OCR request to be
	 * completed after starting realtime OCR.
	 */
	void setStatusViewForContinuous() {
		viewfinderView.removeResultText();
	}

	/** Request the viewfinder to be invalidated. */
	void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}

	/**
	 * We want the help screen to be shown automatically the first time a new version of the app is
	 * run. The easiest way to do this is to check android:versionCode from the manifest, and compare
	 * it to a value stored as a preference.
	 */
	private boolean showHelpOnFirstLaunch() {
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			int currentVersion = info.versionCode;
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			int lastVersion = prefs.getInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, 0);

			if (currentVersion > lastVersion) {

				// Record the last version for which we last displayed the What's New (Help) page
				prefs.edit().putInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, currentVersion).commit();
				Intent intent = new Intent(this, HelpActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

				// Show the default page on a clean install, and the what's new page on an upgrade.
				String page = lastVersion == 0 ? HelpActivity.DEFAULT_PAGE : HelpActivity.WHATS_NEW_PAGE;
				intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, page);
				startActivity(intent);
				return true;
			}
		} catch (PackageManager.NameNotFoundException e) {
			Log.w(TAG, e);
		}
		return false;
	}

	/**
	 * Returns a string that represents which OCR engine(s) are currently set to be run.
	 * 
	 * @return OCR engine mode
	 */
	String getOcrEngineModeName() {
		return DEFAULT_OCR_ENGINE_MODE;
	}

	/**
	 * Gets values from shared preferences and sets the corresponding data members in this activity.
	 */
	private void retrievePreferences() {
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Retrieve from preferences, and set in this Activity, the language preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		setSourceLanguage(prefs.getString(PreferencesActivity.KEY_SOURCE_LANGUAGE_PREFERENCE, "deu"));

		ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;

		characterWhitelist = OcrCharacterHelper.getWhitelist(prefs, sourceLanguageCodeOcr);

		prefs.registerOnSharedPreferenceChangeListener(listener);
		
		showOcrResult = prefs.getBoolean(PreferencesActivity.KEY_SHOW_OCR_RESULT_PREFERENCE, false);

		beepManager.updatePrefs();
	}

	/**
	 * Sets default values for preferences. To be called the first time this app is run.
	 */
	private void setDefaultPreferences() {
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Character whitelist
		prefs.edit().putString(PreferencesActivity.KEY_CHARACTER_WHITELIST, 
				OcrCharacterHelper.getDefaultWhitelist(
						prefs.getString(PreferencesActivity.KEY_SOURCE_LANGUAGE_PREFERENCE, "deu"))).commit();
	}

	/**
	 * Displays an error message dialog box to the user on the UI thread.
	 * 
	 * @param title The title for the dialog box
	 * @param message The error message to be displayed
	 */
	void showErrorMessage(String title, String message) {
		new AlertDialog.Builder(this)
		.setTitle(title)
		.setMessage(message)
		.setOnCancelListener(new FinishListener(this))
		.setPositiveButton( "Done", new FinishListener(this))
		.show();
	}

	@SuppressWarnings("unused")
	private void setOKAlert(int id){
		setOKAlert(this, id);
	}

	private void setOKAlert(Context context, int id){
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(id);
		builder.setPositiveButton(R.string.button_ok, null);
		builder.show();
	}
}
