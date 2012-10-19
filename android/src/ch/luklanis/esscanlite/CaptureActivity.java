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
import ch.luklanis.esscan.language.LanguageCodeHelper;
import ch.luklanis.esscan.paymentslip.EsIbanValidation;
import ch.luklanis.esscan.paymentslip.EsResult;
import ch.luklanis.esscan.paymentslip.EsrResult;
import ch.luklanis.esscan.paymentslip.EsrValidation;
import ch.luklanis.esscan.paymentslip.PsResult;
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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

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

	public static final String EXTERNAL_STORAGE_DIRECTORY = "ESRScan";

	/** Minimum mean confidence score necessary to not reject single-shot OCR result. Currently unused. */
	static final int MINIMUM_MEAN_CONFIDENCE = 0; // 0 means don't reject any scored results

	public static final int HISTORY_REQUEST_CODE = 0x0000bacc;

	private static final int ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
	private static final String sourceLanguageCodeOcr = "psl"; // ISO 639-3 language code

	private static final int pageSegmentationMode = TessBaseAPI.PSM_SINGLE_LINE;
	private static final String characterWhitelist = "0123456789>+";

	private CameraManager cameraManager;
	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private TextView statusViewBottomLeft;
	private boolean hasSurface;
	private BeepManager beepManager;
	private TessBaseAPI baseApi; // Java interface for the Tesseract OCR engine
	private String sourceLanguageReadable; // Language name, for example, "English"

	private SharedPreferences prefs;
	private OnSharedPreferenceChangeListener listener;
	private ProgressDialog dialog; // for initOcr - language download & unzip
	private ProgressDialog indeterminateDialog; // also for initOcr - init OCR engine
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

	@Override
	public void onCreate(Bundle icicle) {
		requestWindowFeature(com.actionbarsherlock.view.Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(icicle);

		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.capture);

		//Load partially transparent black background
		//        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));  

		showOcrResult = false;

		hasSurface = false;
		historyManager = new HistoryManager(this);
		historyManager.trimHistory();

		beepManager = new BeepManager(this);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

	}

	@Override
	protected void onResume() {
		super.onResume();   

		cameraManager = new CameraManager(this);

		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		viewfinderView.setCameraManager(cameraManager);

		statusViewBottomLeft = (TextView) findViewById(R.id.status_view_bottom_left);

		psValidation = new EsrValidation();
		this.lastValidationStep = psValidation.getCurrentStep();

		handler = null;

		retrievePreferences();

		resetStatusView();
		psValidation.gotoBeginning(true);
		this.lastValidationStep = psValidation.getCurrentStep();

		// Do OCR engine initialization, if necessary
		if (baseApi == null) {      
			// Initialize the OCR engine
			File storageDirectory = getFilesDir();
			if (storageDirectory != null) {
				initOcrEngine(storageDirectory, sourceLanguageCodeOcr, sourceLanguageReadable);
			}
		} else {
			resumeOcrEngine();
		}

		// Set up the camera preview surface.
		surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		surfaceHolder = surfaceView.getHolder();

		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		} else {
			// Install the callback and wait for surfaceCreated() to init the camera.
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		checkAndRunFirstLaunch();
	}

	/** 
	 * Method to start or restart recognition after the OCR engine has been initialized,
	 * or after the app regains focus. Sets state related settings and OCR engine parameters,
	 * and requests camera initialization.
	 */
	void resumeOcrEngine() {
		Log.d(TAG, "resumeOcrEngine()");

		// This method is called when Tesseract has already been successfully initialized, so set 
		// isEngineReady = true here.

		if (baseApi != null) {
			if(handler != null) {
				handler.startDecode(baseApi);
			}

			baseApi.setPageSegMode(pageSegmentationMode);
			baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "");
			baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, characterWhitelist);
		}
	}

	/** Called to resume recognition after translation in continuous mode. */
	void restartPreviewAfterDelay(long delayMS) {
		if (handler != null) {
			handler.sendEmptyMessageDelayed(R.id.restart_decode, delayMS);
		}

		resumeOcrEngine();
		resetStatusView();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated()");

		if (holder == null) {
			Log.e(TAG, "surfaceCreated gave us a null surface");
		}

		// Only initialize the camera if the OCR engine is ready to go.
		if (!hasSurface) {
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

			if (handler == null) {
				// Creating the handler starts the preview, which can also throw a RuntimeException.
				handler = new CaptureActivityHandler(this, cameraManager);

				if (baseApi != null) {
					handler.startDecode(baseApi);
				}
			}
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
			handler = null;
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

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (baseApi != null) {
			baseApi.end();
			baseApi = null;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) { 
		case KeyEvent.KEYCODE_BACK:
			setResult(RESULT_CANCELED);
			finish();
			return true;
			// Use volume up/down to turn on light
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			cameraManager.setTorch(false);
			return true;
		case KeyEvent.KEYCODE_VOLUME_UP:
			cameraManager.setTorch(true);
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.capture_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.menu_switch_ps: {
			if (this.psValidation.getSpokenType().equals(EsrResult.PS_TYPE_NAME)) {
				this.psValidation = new EsIbanValidation();
			} else {
				this.psValidation = new EsrValidation();
			}
			resetStatusView();
			break;
		}
		case R.id.menu_history: {
			intent = new Intent(this, HistoryActivity.class);
			startActivityForResult(intent, HISTORY_REQUEST_CODE);
			break;
		}
		case R.id.menu_settings: {
			intent = new Intent(this, PreferencesActivity.class);
			startActivity(intent);
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

	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	/** Finds the proper location on the SD card where we can save files. */
	private File getOldTessdataDirectory() {
		//Log.d(TAG, "getStorageDirectory(): API level is " + Integer.valueOf(android.os.Build.VERSION.SDK_INT));

		String state = null;
		try {
			state = Environment.getExternalStorageState();
		} catch (RuntimeException e) {
			Log.e(TAG, "Is the SD card visible?", e);
			return null;
		}

		if (Environment.MEDIA_MOUNTED.equals(state)) {

			// We can read and write the media
			//    	if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) > 7) {
			// For Android 2.2 and above

			try {
				return getExternalFilesDir(null);
			} catch (NullPointerException e) {
				// We get an error here if the SD card is visible, but full
				Log.e(TAG, "External storage is unavailable");
				return null;
			}
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
		// Set up the dialog box for the thermometer-style download progress indicator
		if (dialog != null) {
			dialog.dismiss();
		}
		dialog = new ProgressDialog(this);

		// Display the name of the OCR engine we're initializing in the indeterminate progress dialog box
		indeterminateDialog = new ProgressDialog(this);
		indeterminateDialog.setTitle("Please wait");
		String ocrEngineModeName = getOcrEngineModeName();
		indeterminateDialog.setMessage("Initializing " + ocrEngineModeName + " OCR engine for " + languageName + "...");
		indeterminateDialog.setCancelable(false);
		indeterminateDialog.show();

		// Start AsyncTask to install language data and init OCR
		new OcrInitAsyncTask(this, new TessBaseAPI(), dialog, indeterminateDialog, languageCode, languageName, ocrEngineMode)
		.execute(storageRoot.toString());
	}

	public void showResult(PsResult psResult) {
		beepManager.playBeepSoundAndVibrate();
		showResult(psResult, false);
	}

	public void showResult(PsResult psResult, boolean fromHistory) {

		if (psResult.getType().equals(EsResult.PS_TYPE_NAME)) {

			showDialogAndRestartScan(R.string.msg_red_result_view_not_available);

			historyManager.addHistoryItem(psResult);
			return;
		}

		Intent intent = new Intent(this, HistoryActivity.class);
		intent.setAction(HistoryActivity.ACTION_SHOW_RESULT);
		intent.putExtra(HistoryActivity.EXTRA_CODE_ROW, psResult.getCompleteCode());
		startActivityForResult(intent, HISTORY_REQUEST_CODE);
	}

	private void showDialogAndRestartScan(int resourceId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(CaptureActivity.this);
		builder.setMessage(resourceId);
		builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				psValidation.gotoBeginning(false);
				lastValidationStep = psValidation.getCurrentStep();
				restartPreviewAfterDelay(0L);
			}
		});
		builder.show();
	}

	/**
	 * Displays information relating to the results of a successful real-time OCR request.
	 * 
	 * @param ocrResult Object representing successful OCR results
	 */
	public void presentOcrDecodeResult(OcrResult ocrResult) {

		// Send an OcrResultText object to the ViewfinderView for text rendering
		viewfinderView.addResultText(new OcrResultText(ocrResult.getText(), 
				ocrResult.getWordConfidences(),
				ocrResult.getMeanConfidence(),
				ocrResult.getBitmapDimensions(),
				ocrResult.getCharacterBoundingBoxes(),
				ocrResult.getWordBoundingBoxes(),
				ocrResult.getTextlineBoundingBoxes(),
				ocrResult.getRegionBoundingBoxes()));

		statusViewBottomLeft.setText(ocrResult.getText());

		if(this.psValidation.getCurrentStep() != this.lastValidationStep){
			this.lastValidationStep = this.psValidation.getCurrentStep();
			beepManager.playBeepSoundAndVibrate();
			refreshStatusView();
		}
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

		View orangeStatusView = findViewById(R.id.status_view_top_orange);
		View redStatusView = findViewById(R.id.status_view_top_red);

		if (this.psValidation.getSpokenType().equals(EsrResult.PS_TYPE_NAME)) {
			redStatusView.setVisibility(View.GONE);
			orangeStatusView.setVisibility(View.VISIBLE);
		} else {
			orangeStatusView.setVisibility(View.GONE);
			redStatusView.setVisibility(View.VISIBLE);
		}

		refreshStatusView();

		statusViewBottomLeft.setText("");

		if (showOcrResult) {
			statusViewBottomLeft.setVisibility(View.VISIBLE);
		}

		viewfinderView.removeResultText();
		viewfinderView.setVisibility(View.VISIBLE);

		Log.i(TAG, "resetStatusView: set lastItem to null");
		viewfinderView.removeResultText();
	}

	private void refreshStatusView() {
		TextView statusView1;
		TextView statusView2;
		TextView statusView3;

		if (this.psValidation.getSpokenType().equals(EsrResult.PS_TYPE_NAME)) {
			statusView1 = (TextView) findViewById(R.id.status_view_1_orange);
			statusView2 = (TextView) findViewById(R.id.status_view_2_orange);
			statusView3 = (TextView) findViewById(R.id.status_view_3_orange);
		} else {
			statusView1 = (TextView) findViewById(R.id.status_view_1_red);
			statusView2 = (TextView) findViewById(R.id.status_view_2_red);
			statusView3 = (TextView) findViewById(R.id.status_view_3_red);
		}

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

	/** Request the viewfinder to be invalidated. */
	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}

	private void DeleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles())
				DeleteRecursive(child);

		fileOrDirectory.delete();
	}

	/**
	 * We want the help screen to be shown automatically the first time a new version of the app is
	 * run. The easiest way to do this is to check android:versionCode from the manifest, and compare
	 * it to a value stored as a preference.
	 */
	private boolean checkAndRunFirstLaunch() {
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			int currentVersion = info.versionCode;
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			int lastVersion = prefs.getInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, 0);

			if (currentVersion > lastVersion) {

				File oldStorage = getOldTessdataDirectory();

				if (oldStorage != null && oldStorage.exists()) {
					DeleteRecursive(new File(oldStorage.toString()));
				}

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
		// Retrieve from preferences, and set in this Activity, the language preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		sourceLanguageReadable = LanguageCodeHelper.getOcrLanguageName(this, sourceLanguageCodeOcr);

		prefs.registerOnSharedPreferenceChangeListener(listener);

		showOcrResult = prefs.getBoolean(PreferencesActivity.KEY_SHOW_OCR_RESULT_PREFERENCE, false);

		beepManager.updatePrefs();
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

	private void setOKAlert(int id){
		setOKAlert(CaptureActivity.this, id);
	}

	private void setOKAlert(Context context, int id){
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(id);
		builder.setPositiveButton(R.string.button_ok, null);
		builder.show();
	}

	public void setBaseApi(TessBaseAPI baseApi) {
		this.baseApi = baseApi;
	}
}
