/*
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

import java.util.List;

import ch.luklanis.esscanlite.R;
import ch.luklanis.esscan.paymentslip.EsrResult;
import ch.luklanis.esscan.paymentslip.PsValidation;

import com.googlecode.tesseract.android.TessBaseAPI;

//import ch.luklanis.esscan.language.PseudoTranslator;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Class to send OCR requests to the OCR engine in a separate thread and appropriately send a
 * success/failure message.
 */
final class OcrRecognizeAsyncTask extends AsyncTask<String, String, Boolean> {

//  private static final boolean PERFORM_EDGE_THRESHOLDING = false; 
//  private static final boolean PERFORM_FISHER_THRESHOLDING = false; 
//  private static final boolean PERFORM_OTSU_THRESHOLDING = false; 
//  private static final boolean PERFORM_SOBEL_THRESHOLDING = false; 
  
//  private static final boolean PERFORM_PSEUDOTRANSLATION = false;
  
  private CaptureActivity activity;
  private TessBaseAPI baseApi;
  private Bitmap bitmap;
  private OcrResult ocrResult;
  private OcrResultFailure ocrResultFailure;
  private ProgressDialog indeterminateDialog;
  private long start;
  private long end;
  
  // Constructor for single-shot mode
  OcrRecognizeAsyncTask(CaptureActivity activity, TessBaseAPI baseApi, 
      ProgressDialog indeterminateDialog, Bitmap bitmap) {
    this.activity = activity;
    this.baseApi = baseApi;
    this.indeterminateDialog = indeterminateDialog;
    this.bitmap = bitmap;
  }

  // Constructor for continuous recognition mode
  OcrRecognizeAsyncTask(CaptureActivity activity, TessBaseAPI baseApi, Bitmap bitmap) {
    this.activity = activity;
    this.baseApi = baseApi;
    this.bitmap = bitmap;
  }
  
  @Override
  protected Boolean doInBackground(String... arg0) {
    String textResult = null;   
    int[] wordConfidences = null;
    int overallConf = -1;
    start = System.currentTimeMillis();
    end = start;
    
    try {
//      if (PERFORM_EDGE_THRESHOLDING) {
//        Pix thresholdedImage = Thresholder.edgeAdaptiveThreshold(ReadFile.readBitmap(bitmap), 32, 64, 32, 1);
//        Log.e("OcrRecognizeAsyncTask", "thresholding completed. converting to bmp. size:" + bitmap.getWidth() + "x" + bitmap.getHeight());
//        bitmap = WriteFile.writeBitmap(thresholdedImage);
//      }
//      if (PERFORM_FISHER_THRESHOLDING) {
//        Pix thresholdedImage = Thresholder.fisherAdaptiveThreshold(ReadFile.readBitmap(bitmap), 48, 48, 0.1F, 2.5F);
//        Log.e("OcrRecognizeAsyncTask", "thresholding completed. converting to bmp. size:" + bitmap.getWidth() + "x" + bitmap.getHeight());
//        bitmap = WriteFile.writeBitmap(thresholdedImage);
//      }
//      if (PERFORM_OTSU_THRESHOLDING) {
//        Pix thresholdedImage = Binarize.otsuAdaptiveThreshold(ReadFile.readBitmap(bitmap), 64, 64, 2, 2, 0.1F);
//        Log.e("OcrRecognizeAsyncTask", "thresholding completed. converting to bmp. size:" + bitmap.getWidth() + "x" + bitmap.getHeight());
//        bitmap = WriteFile.writeBitmap(thresholdedImage);
//      }
//      if (PERFORM_SOBEL_THRESHOLDING) {
//        Pix thresholdedImage = Thresholder.sobelEdgeThreshold(ReadFile.readBitmap(bitmap), 64);
//        Log.e("OcrRecognizeAsyncTask", "thresholding completed. converting to bmp. size:" + bitmap.getWidth() + "x" + bitmap.getHeight());
//        bitmap = WriteFile.writeBitmap(thresholdedImage);
//      }
      Log.i("OcrRecognizeAsyncTask", "converted to bitmap. doing setImage()...");
      baseApi.setImage(bitmap);
      
      Log.i("OcrRecognizeAsyncTask", "setImage() completed");
      textResult = baseApi.getUTF8Text();
      Log.i("OcrRecognizeAsyncTask", "getUTF8Text() completed");
      wordConfidences = baseApi.wordConfidences();
      overallConf = baseApi.meanConfidence();
      end = System.currentTimeMillis();
    } catch (RuntimeException e) {
      Log.e("OcrRecognizeAsyncTask", "Caught RuntimeException in request to Tesseract. Setting state to CONTINUOUS_STOPPED.");
      e.printStackTrace();
      try {
        baseApi.clear();
        activity.stopHandler();
      } catch (NullPointerException e1) {
        // Continue
      }
      return false;
    }

    // Check for failure to recognize text
    if (textResult == null || textResult.equals("")) {
      ocrResultFailure = new OcrResultFailure(end - start);
      return false;
    }  
    
    // Get bounding boxes for characters and words
    List<Rect> wordBoxes = baseApi.getWords().getBoxRects();
    List<Rect> characterBoxes = baseApi.getCharacters().getBoxRects();
    List<Rect> textlineBoxes = baseApi.getTextlines().getBoxRects();
    List<Rect> regionBoxes = baseApi.getRegions().getBoxRects();
    
    PsValidation validation = activity.getValidation();
    
//    String relatedText = "";
    
    while(validation.validate(textResult)){
    	if(!validation.nextStep()){
    		break;
    	}
    }
    
//    if(!validation.finished()){
//    	relatedText = validation.getRelatedText();
//    }
//    
//	textResult = validation.getCompleteCode() + relatedText;
    //textResult = validation.getCompleteCode();
      
    ocrResult = new OcrResult(bitmap, textResult, wordConfidences, overallConf, characterBoxes, 
        textlineBoxes, wordBoxes, regionBoxes, (end - start));
    return true;
  }
  
  @Override
  protected void onPostExecute(Boolean result) {
    super.onPostExecute(result);

    Handler handler = activity.getHandler();
    PsValidation validation = activity.getValidation();
    if (validation.finished() && handler != null) {
      // Send results for single-shot mode recognition.
      if (result) {
        Message message = Message.obtain(handler, R.id.esr_decode_succeeded, new EsrResult(validation.getCompleteCode()));
        message.sendToTarget();
      } 
      
      if(indeterminateDialog != null){
    	  indeterminateDialog.dismiss();
      }
    } else  if (handler != null) {
      // Send results for continuous mode recognition.
      if (result) {
        try {
          // Send the result to CaptureActivityHandler
          Message message = Message.obtain(handler, R.id.ocr_continuous_decode_succeeded, ocrResult);
          message.sendToTarget();
        } catch (NullPointerException e) {
          activity.stopHandler();
        }
      } else {
        bitmap.recycle();
        try {
          Message message = Message.obtain(handler, R.id.ocr_continuous_decode_failed, ocrResultFailure);
          message.sendToTarget();
        } catch (NullPointerException e) {
          activity.stopHandler();
        }
      }
    }
    if (baseApi != null) {
      baseApi.clear();
    }
  }
}
