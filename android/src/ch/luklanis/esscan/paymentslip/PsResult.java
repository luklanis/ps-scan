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
public abstract class PsResult {
	protected final String completeCode;

	protected final long timestamp;

	public PsResult(String completeCode) {
		this.completeCode = completeCode;
		this.timestamp = System.currentTimeMillis();
	}

	public PsResult(String completeCode, long timestamp) {
		this.completeCode = completeCode;
		this.timestamp = timestamp;
	}

	public String getCompleteCode() {
		return completeCode;
	}

	public long getTimestamp() {
		return timestamp;
	}
	
	public static String getCoderowType(String completeCodeRow) {
		int plusLocation = completeCodeRow.indexOf("+");
		int greaterThanLocation = completeCodeRow.indexOf(">");
		
		return (plusLocation > greaterThanLocation) ? "orange" : "red";
	}
	
	public String getType() {
		return getCoderowType(this.completeCode);
	}

	public abstract String getAccount();

	@Override
	public abstract String toString();
}
