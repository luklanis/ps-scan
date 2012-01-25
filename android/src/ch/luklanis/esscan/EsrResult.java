/*
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
package ch.luklanis.esscan;

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;

/**
 * Encapsulates the result of OCR.
 */
public final class EsrResult {
  private final String text;
  
  private final long timestamp;
  
  public EsrResult(String text) {
    this.text = text;
    this.timestamp = System.currentTimeMillis();
  }
  
  public EsrResult(String text, long timestamp) {
    this.text = text;
    this.timestamp = timestamp;
  }
  
  public String getText() {
    return text;
  }
  
  public long getTimestamp() {
    return timestamp;
  }
  
  @Override
  public String toString() {
    return text;
  }
}
