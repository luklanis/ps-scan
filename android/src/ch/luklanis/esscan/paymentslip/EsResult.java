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
public final class EsResult extends PsResult{

	public EsResult(String completeCode) {
		super(completeCode);
	}
	
	public EsResult(String completeCode, long timestamp) {
		super(completeCode, timestamp);
	}

	@Override
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

	@Override
	public String toString() {
		return getAccount();
	}
}
