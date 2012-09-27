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
package ch.luklanis.esscan;

import com.googlecode.tesseract.android.TessBaseAPI;

import ch.luklanis.esscan.CaptureActivity;
import ch.luklanis.esscan.R;
import ch.luklanis.esscan.camera.CameraManager;
import ch.luklanis.esscan.history.HistoryItem;
import ch.luklanis.esscan.paymentslip.PsResult;
import ch.luklanis.esscan.OcrResult;

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

	private final CaptureActivity activity;
	private final DecodeThread decodeThread;
	private static State state;
	private final CameraManager cameraManager;

	private enum State {
		PREVIEW,
		SUCCESS,
		DONE
	}

	CaptureActivityHandler(CaptureActivity activity, CameraManager cameraManager, TessBaseAPI baseApi) {
		this.activity = activity;
		this.cameraManager = cameraManager;

		decodeThread = new DecodeThread(activity, 
				baseApi);
		decodeThread.start();

		state = State.SUCCESS;

		// Start ourselves capturing previews and decoding.
		cameraManager.startPreview();
		restartOcrPreviewAndDecode();
	}

	@Override
	public void handleMessage(Message message) {

		switch (message.what) {
		case R.id.restart_decode:
			Log.d(TAG, "Got restart decode message");
			DecodeHandler.resetDecodeState();
			restartOcrPreviewAndDecode();
			break;
		case R.id.decode_succeeded:
			Log.d(TAG, "Got decode succeeded message");
			state = State.SUCCESS;
			try {
				activity.handleOcrContinuousDecode((OcrResult) message.obj);
			} catch (NullPointerException e) {
				// Continue
			}
			DecodeHandler.resetDecodeState();
			restartOcrPreviewAndDecode();
			break;
		case R.id.decode_failed:
			state = State.PREVIEW;
			DecodeHandler.resetDecodeState();
			cameraManager.requestOcrDecode(decodeThread.getHandler(), R.id.decode);
			break;
		case R.id.esr_decode_succeeded:
			state = State.DONE;
			PsResult result = (PsResult) message.obj;

			activity.showResult(result);
			DecodeHandler.resetDecodeState();
			break;
		case R.id.esr_show_history_item:
			activity.showResult((HistoryItem) message.obj);
			DecodeHandler.resetDecodeState();
			break;
		}
	}

	void quitSynchronously() {    
		state = State.DONE;
		if (cameraManager != null) {
			cameraManager.stopPreview();
		}

		try {
			// Wait at most half a second; should be enough time, and onPause() will timeout quickly
			decodeThread.join(500L);
		} catch (InterruptedException e) {
			// continue
		}

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
			// Continue capturing camera frames
			cameraManager.startPreview();

			// Continue requesting decode of images
			cameraManager.requestOcrDecode(decodeThread.getHandler(), R.id.decode);
			activity.drawViewfinder();    
		}
	}
}
