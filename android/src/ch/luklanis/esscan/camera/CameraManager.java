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

package ch.luklanis.esscan.camera;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import ch.luklanis.esscan.PlanarYUVLuminanceSource;
import ch.luklanis.esscan.PreferencesActivity;
import ch.luklanis.esscan.camera.open.OpenCameraManager;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing
 */
public final class CameraManager {

	public static final int MIN_FRAME_WIDTH = 240; // originally 240
	public static final int MIN_FRAME_HEIGHT = 48; // originally 240

	public static final double FRAME_WIDTH_INCHES = 3.74;
	public static final double FRAME_HEIGHT_INCHES = 0.23;

	private final Activity activity;
	private final CameraConfigurationManager configManager;
	private Camera camera;
	private AutoFocusManager autoFocusManager;
	private Rect framingRect;
	private Rect framingRectInPreview;
	private boolean initialized;
	private boolean previewing;
	private boolean reverseImage;

	/**
	 * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
	 * clear the handler so it will only receive one message.
	 */
	private final PreviewCallback previewCallback;

	public CameraManager(Activity activity) {
		this.activity = activity;
		this.configManager = new CameraConfigurationManager(activity);
		previewCallback = new PreviewCallback(configManager);
	}

	/**
	 * Opens the camera driver and initializes the hardware parameters.
	 *
	 * @param holder The surface object which the camera will draw preview frames into.
	 * @throws IOException Indicates the camera driver failed to open.
	 */
	public void openDriver(SurfaceHolder holder) throws IOException {
		Camera theCamera = camera;
		if (theCamera == null) {
			theCamera = new OpenCameraManager().build().open();
			if (theCamera == null) {
				throw new IOException();
			}
			camera = theCamera;
		}
		camera.setPreviewDisplay(holder);

		if (!initialized) {
			initialized = true;
			configManager.initFromCameraParameters(theCamera);
		}

		configManager.setDesiredCameraParameters(theCamera);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

		reverseImage = prefs.getBoolean(PreferencesActivity.KEY_REVERSE_IMAGE, false);
	}

	/**
	 * Closes the camera driver if still in use.
	 */
	public void closeDriver() {
		if (camera != null) {
			camera.release();
			camera = null;

			// Make sure to clear these each time we close the camera, so that any scanning rect
			// requested by intent is forgotten.
			framingRect = null;
			framingRectInPreview = null;
		}
	}

	/**
	 * Asks the camera hardware to begin drawing preview frames to the screen.
	 */
	public void startPreview() {
		Camera theCamera = camera;
		if (theCamera != null && !previewing) {
			theCamera.startPreview();
			previewing = true;
			autoFocusManager = new AutoFocusManager(activity, camera);
		}
	}

	/**
	 * Tells the camera to stop drawing preview frames.
	 */
	public void stopPreview() {
		if (autoFocusManager != null) {
			autoFocusManager.stop();
			autoFocusManager = null;
		}
		if (camera != null && previewing) {
			camera.stopPreview();
			previewCallback.setHandler(null, 0);

			this.configManager.setTorch(camera, false);

			previewing = false;
		}
	}

	/**
	 * Convenience method for {@link com.google.zxing.client.android.CaptureActivity}
	 */
	public synchronized void setTorch(boolean newSetting) {
		if (camera != null) {
			if (autoFocusManager != null) {
				autoFocusManager.stop();
			}
			configManager.setTorch(camera, newSetting);
			if (autoFocusManager != null) {
				autoFocusManager.start();
			}
		}
	}

	/**
	 * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
	 * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
	 * respectively.
	 *
	 * @param handler The handler to send the message to.
	 * @param message The what field of the message to be sent.
	 */
	public void requestOcrDecode(Handler handler, int message) {
		Camera theCamera = camera;
		if (theCamera != null && previewing) {			
			previewCallback.setHandler(handler, message);
			theCamera.setOneShotPreviewCallback(previewCallback);
		}
	}

	/**
	 * Calculates the framing rect which the UI should draw to show the user where to place the
	 * barcode. This target helps with alignment as well as forces the user to hold the device
	 * far enough away to ensure the image will be in focus.
	 *
	 * @return The rectangle to draw on screen in window coordinates.
	 */
	public Rect getFramingRect() {
		if (framingRect == null) {
			if (camera == null) {
				return null;
			}

			Point previewResolution = configManager.getPreviewResolution();

			DisplayMetrics metrics = new DisplayMetrics();
			activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

			int width = (int) (metrics.xdpi * FRAME_WIDTH_INCHES);
			if (width < MIN_FRAME_WIDTH) {
				width = MIN_FRAME_WIDTH;
			} else if (width > previewResolution.x) {
				width = previewResolution.x;
			}

			int height = (int) (metrics.ydpi * FRAME_HEIGHT_INCHES);
			if (height < MIN_FRAME_HEIGHT) {
				height = MIN_FRAME_HEIGHT;
			} 

			int leftOffset = (previewResolution.x - width) / 2;
			int topOffset = ((previewResolution.y - height) / 2);

			framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
		}
		return framingRect;
	}

	/**
	 * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
	 * not UI / screen.
	 */
	public Rect getFramingRectInPreview() {
		if (framingRectInPreview == null) {
			Rect framingRect = getFramingRect();

			if (framingRect == null) {
				return null;
			}

			Rect rect = new Rect(framingRect);

			rect.offset(0, getFramingTopOffset());

			Point cameraResolution = configManager.getCameraResolution();
			Point screenResolution = configManager.getPreviewResolution();

			float scaleX = (float)cameraResolution.x / (float)screenResolution.x;
			float scaleY = (float)cameraResolution.y / (float)screenResolution.y;

			rect.left = (int)(rect.left * scaleX);
			rect.right = (int)(rect.right * scaleX);
			rect.top = (int)(rect.top * scaleY);
			rect.bottom = (int)(rect.bottom * scaleY);
			framingRectInPreview = rect;
		}
		return framingRectInPreview;
	}

	/**
	 * A factory method to build the appropriate LuminanceSource object based on the format
	 * of the preview buffers, as described by Camera.Parameters.
	 *
	 * @param data A preview frame.
	 * @param width The width of the image.
	 * @param height The height of the image.
	 * @return A PlanarYUVLuminanceSource instance.
	 */
	public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
		Rect rect = getFramingRectInPreview();
		if (rect == null) {
			return null;
		}
		// Go ahead and assume it's YUV rather than die.
		return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
				rect.width(), rect.height(), reverseImage);
	}

	public int getFramingTopOffset() {
		return configManager.getHeightDiff() / 2;
	}

}
