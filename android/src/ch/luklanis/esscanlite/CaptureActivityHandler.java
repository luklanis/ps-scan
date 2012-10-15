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
package ch.luklanis.esscanlite;

import com.googlecode.tesseract.android.TessBaseAPI;

import ch.luklanis.esscanlite.R;
import ch.luklanis.esscan.camera.CameraManager;
import ch.luklanis.esscan.paymentslip.PsResult;
import ch.luklanis.esscanlite.CaptureActivity;
import ch.luklanis.esscanlite.OcrResult;
import ch.luklanis.esscanlite.history.HistoryItem;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class handles all the messaging which comprises the state machine for capture.
 *
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing/
 */
final class CaptureActivityHandler extends Handler {

	private static final String TAG = CaptureActivityHandler.class.getSimpleName();

	private static final long OCR_INIT_DELAY = 200;

	private static State state;
	private final CaptureActivity activity;
	private final CameraManager cameraManager;
	private DecodeThread decodeThread;

	private enum State {
		PREVIEW,
		SUCCESS,
		DONE
	}

	CaptureActivityHandler(CaptureActivity activity, CameraManager cameraManager) {
		this.activity = activity;
		this.cameraManager = cameraManager;

		decodeThread = null;

		state = State.SUCCESS;

		// Start ourselves capturing previews and decode.
		restartOcrPreviewAndDecode();
	}

	@Override
	public void handleMessage(Message message) {

		switch (message.what) {
		case R.id.restart_decode:
			Log.d(TAG, "Got restart decode message");
			state = State.PREVIEW;
			requestOcrDecodeWhenThreadReady();
			break;
		case R.id.decode_succeeded:
			Log.d(TAG, "Got decode succeeded message");
			if (state != State.DONE) {
				state = State.SUCCESS;
				try {
					activity.presentOcrDecodeResult((OcrResult) message.obj);
				} catch (NullPointerException e) {
					// Continue
				}
				requestOcrDecodeWhenThreadReady();
				activity.drawViewfinder();  
			}
			break;
		case R.id.decode_failed:
			if (state != State.DONE) {
				state = State.PREVIEW;
				requestOcrDecodeWhenThreadReady();
			}
			break;
		case R.id.es_decode_succeeded:
			state = State.DONE;
			PsResult result = (PsResult) message.obj;

			activity.showResult(result);
			break;
		}
	}

	public void startDecode(TessBaseAPI baseApi) {
		if (this.decodeThread == null) {
			this.decodeThread = new DecodeThread(this.activity, baseApi);
			this.decodeThread.start();
		}
	}

	public void quitSynchronously() {    
		state = State.DONE;
		if (cameraManager != null) {
			cameraManager.stopPreview();
		}

		if (decodeThread != null) {
			try {
				Message message = Message.obtain(decodeThread.getHandler(), R.id.quit);
				message.sendToTarget();

				// Wait at most half a second; should be enough time, and onPause() will timeout quickly
				decodeThread.join(500L);
			} catch (InterruptedException e) {
				// continue
			}
		}

		decodeThread = null;

		// Be absolutely sure we don't send any queued up messages
		removeMessages(R.id.restart_decode);
		removeMessages(R.id.decode_failed);
		removeMessages(R.id.decode_succeeded);
	}

	/**
	 *  Send a decode request for realtime OCR mode
	 */
	private void restartOcrPreviewAndDecode() {
		if (state == State.SUCCESS) {
			state = State.PREVIEW;
			// Continue capturing camera frames
			cameraManager.startPreview();

			requestOcrDecodeWhenThreadReady();
			activity.drawViewfinder();    
		}
	}

	private void requestOcrDecodeWhenThreadReady() {
		if (this.decodeThread != null) {
			// Continue requesting decode of images
			cameraManager.requestOcrDecode(decodeThread.getHandler(), R.id.decode);
		} else {
			this.sendEmptyMessageDelayed(R.id.decode_failed, OCR_INIT_DELAY);
			Log.w(TAG, "Skipping decode because OCR isn't initialized yet");
		}
	}
}
