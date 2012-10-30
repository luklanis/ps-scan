/*
 * Copyright (C) 2010 ZXing authors
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

package ch.luklanis.esscan.camera;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.util.Log;
//import android.view.Display;
import android.view.Display;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

import ch.luklanis.esscan.R;
import ch.luklanis.esscan.PreferencesActivity;
import ch.luklanis.esscan.camera.exposure.ExposureInterface;
import ch.luklanis.esscan.camera.exposure.ExposureManager;

import java.util.Collection;

/**
 * A class which deals with reading, parsing, and setting the camera parameters which are used to
 * configure the camera hardware.
 * 
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing
 */
final class CameraConfigurationManager {

	private static final String TAG = "CameraConfiguration";
	private static final int MIN_PREVIEW_PIXELS = 320 * 240; // small screen
	//  private static final int MAX_PREVIEW_PIXELS = 800 * 480; // large/HD screen

	private final Activity activity;
	private Point previewResolution;
	private Point cameraResolution;
	private int heightDiff;
	private final ExposureInterface exposure;

	CameraConfigurationManager(Activity activity) {
		this.activity = activity;
		exposure = new ExposureManager().build();
	}

	/**
	 * Reads, one time, values from the camera that are needed by the app.
	 */
	void initFromCameraParameters(Camera camera) {
		Camera.Parameters parameters = camera.getParameters();
		WindowManager manager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);

		SurfaceView previewView = (SurfaceView) activity.findViewById(R.id.preview_view);
		int previewWidth = previewView.getWidth();
		int previewHeight = previewView.getHeight();

		Display display = manager.getDefaultDisplay();
		int screenWidth = display.getWidth();
		int screenHeight = display.getHeight();

		// We're landscape-only, and have apparently seen issues with display thinking it's portrait 
		// when waking from sleep. If it's not landscape, assume it's mistaken and reverse them:
		if (screenWidth < screenHeight) {
			int temp = screenWidth;
			screenWidth = screenHeight;
			screenHeight = temp;
		}

		// Camera surface has the same height as the screen. Because of
		// the Notification- and ActionBar preview's height is less than screen's
		// so we had to take notice of it in offset calculation
		this.heightDiff = screenHeight - previewHeight;

		if (previewWidth < previewHeight) {
			Log.i(TAG, "Display reports portrait orientation; assuming this is incorrect");
			previewWidth = screenWidth;

			this.heightDiff = screenWidth - previewHeight;
			previewHeight = (screenHeight - (screenWidth - previewHeight));
		}

		Point screenResolution = new Point(screenWidth, screenHeight);

		cameraResolution = findBestPreviewSizeValue(parameters, screenResolution, false);

		LayoutParams params = previewView.getLayoutParams();
		params.height = screenHeight;
		previewView.setLayoutParams(params);

		previewHeight = screenHeight;

		int cameraHeight = cameraResolution.y;

		if (cameraHeight > cameraResolution.x) {
			cameraHeight = cameraResolution.x;
		}

		previewResolution = new Point(previewWidth, previewHeight);
		Log.i(TAG, "Preview resolution: " + previewResolution);

		Log.i(TAG, "Camera resolution: " + cameraResolution);
	}

	void setDesiredCameraParameters(Camera camera) {
		Camera.Parameters parameters = camera.getParameters();

		if (parameters == null) {
			Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
			return;
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

		initializeTorch(parameters, prefs);

		String focusMode = null;
		if (!prefs.getBoolean(PreferencesActivity.KEY_ONLY_MACRO_FOCUS, false)) {
			if (prefs.getBoolean(PreferencesActivity.KEY_NO_CONTINUES_AUTO_FOCUS, true)) {
				focusMode = findSettableValue(parameters.getSupportedFocusModes(),
						Camera.Parameters.FOCUS_MODE_AUTO);
			} else {
				focusMode = findSettableValue(parameters.getSupportedFocusModes(),
						"continuous-picture", // Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE in 4.0+
						"continuous-video",   // Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO in 4.0+
						Camera.Parameters.FOCUS_MODE_AUTO);
			}
		}

		// Maybe selected auto-focus but not available, so fall through here:
		if (focusMode == null) {
			focusMode = findSettableValue(parameters.getSupportedFocusModes(),
					Camera.Parameters.FOCUS_MODE_MACRO,
					"edof"); // Camera.Parameters.FOCUS_MODE_EDOF in 2.2+
		}

		if (focusMode != null) {
			parameters.setFocusMode(focusMode);
		}

		parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
		camera.setParameters(parameters);
	}

	Point getCameraResolution() {
		return cameraResolution;
	}

	Point getPreviewResolution() {
		return previewResolution;
	}

	void setTorch(Camera camera, boolean newSetting) {
		Camera.Parameters parameters = camera.getParameters();
		doSetTorch(parameters, newSetting);
		camera.setParameters(parameters);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		boolean currentSetting = prefs.getBoolean(PreferencesActivity.KEY_ENABLE_TORCH, false);
		if (currentSetting != newSetting) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(PreferencesActivity.KEY_ENABLE_TORCH, newSetting);
			editor.commit();
		}
	}

	int getHeightDiff() {
		return this.heightDiff;
	}

	public void initializeTorch(Camera.Parameters parameters, SharedPreferences prefs) {
		boolean currentSetting = prefs.getBoolean(PreferencesActivity.KEY_ENABLE_TORCH, false);
		doSetTorch(parameters, currentSetting);
	}

	private void doSetTorch(Camera.Parameters parameters, boolean newSetting) {
		String flashMode;
		if (newSetting) {
			flashMode = findSettableValue(parameters.getSupportedFlashModes(),
					Camera.Parameters.FLASH_MODE_TORCH,
					Camera.Parameters.FLASH_MODE_ON);
		} else {
			flashMode = findSettableValue(parameters.getSupportedFlashModes(),
					Camera.Parameters.FLASH_MODE_OFF);
		}
		if (flashMode != null) {
			parameters.setFlashMode(flashMode);
		}

		exposure.setExposure(parameters, newSetting);
	}

	private static Point findBestPreviewSizeValue(Camera.Parameters parameters,
			Point screenResolution,
			boolean portrait) {
		Point bestSize = null;
		int diff = Integer.MAX_VALUE;
		for (Camera.Size supportedPreviewSize : parameters.getSupportedPreviewSizes()) {
			int pixels = supportedPreviewSize.height * supportedPreviewSize.width;
			if (pixels < MIN_PREVIEW_PIXELS) {
				continue;
			}
			int supportedWidth = portrait ? supportedPreviewSize.height : supportedPreviewSize.width;
			int supportedHeight = portrait ? supportedPreviewSize.width : supportedPreviewSize.height;
			int newDiff = Math.abs(screenResolution.x * supportedHeight - supportedWidth * screenResolution.y);
			if (newDiff == 0) {
				bestSize = new Point(supportedWidth, supportedHeight);
				break;
			}
			if (newDiff < diff) {
				bestSize = new Point(supportedWidth, supportedHeight);
				diff = newDiff;
			}
		}
		if (bestSize == null) {
			Camera.Size defaultSize = parameters.getPreviewSize();
			bestSize = new Point(defaultSize.width, defaultSize.height);
		}
		return bestSize;
	}

	private static String findSettableValue(Collection<String> supportedValues,
			String... desiredValues) {
		Log.i(TAG, "Supported values: " + supportedValues);
		String result = null;
		if (supportedValues != null) {
			for (String desiredValue : desiredValues) {
				if (supportedValues.contains(desiredValue)) {
					result = desiredValue;
					break;
				}
			}
		}
		Log.i(TAG, "Settable value: " + result);
		return result;
	}

}
