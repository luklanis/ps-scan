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

/**
 * Encapsulates the result of OCR.
 */
public final class EsrResult {
  private final String text;
  private final String address;
  
  private final long timestamp;
  
  private long paid;
  
  public EsrResult(String text) {
    this.text = text;
    this.timestamp = System.currentTimeMillis();
    this.address = null;
    this.paid = 0;
  }
  
  public EsrResult(String text, long timestamp) {
    this.text = text;
    this.timestamp = timestamp;
    this.address = null;
    this.paid = 0;
  }
  
  public EsrResult(String text, long timestamp, String address) {
    this.text = text;
    this.timestamp = timestamp;
    this.address = address;
    this.paid = 0;
  }
  
  public EsrResult(String text, long timestamp, String address, long paid) {
    this.text = text;
    this.timestamp = timestamp;
    this.address = address;
    this.paid = paid;
  }
  
  public String getText() {
    return text;
  }
  
  public long getTimestamp() {
    return timestamp;
  }
  
  public String getAddress() {
    return address;
  }
  
  public long getPaid() {
    return paid;
  }
  
  public void setPaidNow() {
	  paid = System.currentTimeMillis();
  }
  
  public String getAmount(){
	  if(text.indexOf('>') <= 3){
		  return "";
	  }
	  
	  int beforePoint = Integer.parseInt(text.substring(2, 10));
	  
	  return String.valueOf(beforePoint) + "." + text.substring(10, 12);
  }
  
  public String getCurrency(){
	  int esrType = Integer.parseInt(text.substring(0, 2));

	  switch(esrType){
	  case 1:
	  case 3:
	  case 4:
	  case 11:
	  case 14:
		  return "CHF";
	  case 21:
	  case 23:
	  case 31:
	  case 33:
		  return "EUR";
	  default: return "?";
	  }
  }
  
  public String getAccount(){
	  int indexOfSpace = text.indexOf(' ');
	  
	  if(indexOfSpace < 0){
		  return "?";
	  }
	  
	  String tempSubString = text.substring((indexOfSpace + 3), (indexOfSpace + 9));
	  
	  int indentureNumber = Integer.parseInt(tempSubString);
	  
	  return text.substring((indexOfSpace + 1), (indexOfSpace + 3)) + "-" + String.valueOf(indentureNumber) + "-"
			  + text.substring((indexOfSpace + 9), (indexOfSpace + 10));
  }
  
  public String getReference(){
	  int indexOfSpecialChar = text.indexOf('>');
	  int indexOfPlus = text.indexOf('+');
	  
	  if(indexOfSpecialChar < 0 || indexOfPlus < indexOfSpecialChar){
		  return "?";
	  }
	  
	  return text.substring(indexOfSpecialChar, indexOfSpecialChar);
  }
  
  @Override
  public String toString() {
    return text;
  }
}
