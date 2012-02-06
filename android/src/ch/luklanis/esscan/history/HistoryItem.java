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

package ch.luklanis.esscan.history;

import ch.luklanis.esscan.paymentslip.EsrResult;

public final class HistoryItem {

	private final EsrResult result;
	private String address;
	private String amount;
	private final String dtaFile;
	private boolean exported;

	public HistoryItem(EsrResult result) {
		this.result = result;
		this.address = null;
		this.amount = null;
		this.dtaFile = null;
		this.exported = false;
	}

	HistoryItem(EsrResult result, String amount) {
		this.result = result;
		this.address = null;
		this.amount = amount;
		this.dtaFile = null;
		this.exported = false;
	}

	HistoryItem(EsrResult result, String amount, String address) {
		this.result = result;
		this.address = address;
		this.amount = amount;
		this.dtaFile = null;
		this.exported = false;
	}

	HistoryItem(EsrResult result, String amount, String address, String dtaFile) {
		this.result = result;
		this.address = address;
		this.amount = amount;
		this.dtaFile = dtaFile;
		this.exported = false;
	}

	public EsrResult getResult() {
		return result;
	}

	public String getAddress() {
		return address;
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
}
