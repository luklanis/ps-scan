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
package ch.luklanis.esscan.paymentslip;

/**
 * Encapsulates the result of OCR.
 */
public final class EsrResult {
	private final String completeCode;
	private final String address;

	private final long timestamp;

	private long paid;

	public EsrResult(String completeCode) {
		this.completeCode = completeCode;
		this.timestamp = System.currentTimeMillis();
		this.address = null;
		this.paid = 0;
	}

	public EsrResult(String completeCode, long timestamp) {
		this.completeCode = completeCode;
		this.timestamp = timestamp;
		this.address = null;
		this.paid = 0;
	}

	public EsrResult(String completeCode, long timestamp, String address) {
		this.completeCode = completeCode;
		this.timestamp = timestamp;
		this.address = address;
		this.paid = 0;
	}

	public EsrResult(String completeCode, long timestamp, String address, long paid) {
		this.completeCode = completeCode;
		this.timestamp = timestamp;
		this.address = address;
		this.paid = paid;
	}

	public String getCompleteCode() {
		return completeCode;
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
		String code = completeCode;

		if(code.indexOf('>') <= 3){
			return "";
		}

		int beforePoint = Integer.parseInt(code.substring(2, 10));

		return String.valueOf(beforePoint) + "." + code.substring(10, 12);
	}

	public String getCurrency(){
		String code = completeCode;
		int esrType = Integer.parseInt(code.substring(0, 2));

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
		String code = completeCode;
		int indexOfSpace = code.indexOf(' ');

		if(indexOfSpace < 0){
			return "?";
		}

		int indentureNumber = Integer.parseInt(code.substring((indexOfSpace + 3), (indexOfSpace + 9)));

		return code.substring((indexOfSpace + 1), (indexOfSpace + 3)) + "-" + String.valueOf(indentureNumber) + "-"
		+ code.substring((indexOfSpace + 9), (indexOfSpace + 10));
	}

	public String getReference(){
		String code = completeCode;
		int indexOfSpecialChar = code.indexOf('>');
		int indexOfPlus = code.indexOf('+');
		int blockSize = 5;

		if(indexOfSpecialChar < 0 || indexOfPlus < indexOfSpecialChar){
			return "?";
		}

		String referenz = code.substring((indexOfSpecialChar + 1), indexOfPlus);
		
		while(referenz.indexOf('0') == 0){
			referenz = referenz.substring(0, 1);
		}

		int firstChars = referenz.length() % blockSize;
		String referenz_formated = referenz.substring(0, firstChars);

		for (int i = 0; i < referenz.length() / blockSize; i++) {
			int start = (i * blockSize) + firstChars;
			referenz_formated += " " + referenz.substring(start, start + blockSize);
		}

		return referenz_formated;
	}

	@Override
	public String toString() {
		return getAccount() + ", " + getCurrency() + " " + getAmount();
	}
}
