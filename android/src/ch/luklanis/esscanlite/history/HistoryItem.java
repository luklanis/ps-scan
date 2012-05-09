/*
 * Copyright 2012 ZXing authors
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

package ch.luklanis.esscanlite.history;

import ch.luklanis.esscan.paymentslip.EsrResult;

public final class HistoryItem {

	private final EsrResult result;
	private int addressNumber;
	private String amount;
	private final String dtaFile;
	private boolean exported;
	private String address;

	public HistoryItem(EsrResult result) {
		this.result = result;
		this.addressNumber = -1;
		this.amount = null;
		this.dtaFile = null;
		this.exported = false;
		this.address = "";
	}

//	HistoryItem(EsrResult result, String amount) {
//		this.result = result;
//		this.addressNumber = -1;
//		this.amount = amount;
//		this.dtaFile = null;
//		this.exported = false;
//	}
//
//	HistoryItem(EsrResult result, String amount, int addressNumber) {
//		this.result = result;
//		this.addressNumber = addressNumber;
//		this.amount = amount;
//		this.dtaFile = null;
//		this.exported = false;
//	}

	HistoryItem(EsrResult result, String amount, int addressNumber, String dtaFile) {
		this.result = result;
		this.addressNumber = addressNumber;
		this.amount = amount;
		this.dtaFile = dtaFile;
		this.exported = false;
		this.address = "";
	}

	public EsrResult getResult() {
		return result;
	}

//	public void setAddress(String address){
//		this.address = address;
//	}

	public String getAmount() {
		if(result.getAmount() != ""){
			return result.getAmount();
		}
		
		return amount;
	}

	public boolean getExported() {
		return this.exported || this.dtaFile != null;
	}

	public void setExported(boolean exported) {
		this.exported = exported;
	}

	public String getDTAFilename() {
		return this.dtaFile;
	}

	public int getAddressNumber() {
		return addressNumber;
	}

	public void setAddressNumber(int addressNumber) {
		this.addressNumber = addressNumber;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
}
