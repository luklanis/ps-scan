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

public abstract class PsValidation {
    private static final int STEP_COUNT = 1;  
    
    protected static final int[][] MODULO10 = {
    	{ 0, 9, 4, 6, 8, 2, 7, 1, 3, 5 },
    	{ 9, 4, 6, 8, 2, 7, 1, 3, 5, 0 },
    	{ 4, 6, 8, 2, 7, 1, 3, 5, 0, 9 },
    	{ 6, 8, 2, 7, 1, 3, 5, 0, 9, 4 },
    	{ 8, 2, 7, 1, 3, 5, 0, 9, 4, 6 },
    	{ 2, 7, 1, 3, 5, 0, 9, 4, 6, 8 },
    	{ 7, 1, 3, 5, 0, 9, 4, 6, 8, 2 },
    	{ 1, 3, 5, 0, 9, 4, 6, 8, 2, 7 },
    	{ 3, 5, 0, 9, 4, 6, 8, 2, 7, 1 },
    	{ 5, 0, 9, 4, 6, 8, 2, 7, 1, 3 }
    };
    
    protected static final int[] CHECK_DIGIT = {
    	0, 9, 8, 7, 6, 5, 4, 3, 2, 1
    };
    
	protected int currentStep;
	
	protected String relatedText;
	protected boolean finished;
	
	public PsValidation() {
		gotoBeginning(true);
	}
    
	protected int getCheckDigit(int[] digits){
		int lastValue = 0;
		
		for(int i = 0; i < digits.length; i++){
				lastValue = MODULO10[lastValue][digits[i]];
		}
		
		return CHECK_DIGIT[lastValue];
    }
	
	public int getStepCount(){
		return STEP_COUNT;
	}
	
	public int getCurrentStep(){
		return currentStep + 1;
	}
	
	public boolean nextStep(){
		if (currentStep < getStepCount() - 1){
			currentStep++;
			relatedText = null;
			return true;
		}
		
		finished = true;
		return false;
	}
	
	public void gotoBeginning(boolean reset){
		currentStep = 0;
		finished = false;
		relatedText = null;
		resetCompleteCode();
	}
	
	public boolean finished(){
		return finished;
	}
	
	public int[] getDigitsFromText(String text, int length) throws Exception{
		int[] digits = new int[length];
		
		for(int i = 0; i < digits.length; i++){
			digits[i] = text.charAt(i) - '0';
			
			if(digits[i] < 0 || digits[i] > 9){
				throw new Exception(String.format("%s is not a digit", digits[i]));
			}
		}
		
		return digits;
	}

	public abstract boolean validate(String text);
	
	public abstract String getRelatedText();	
	
	public abstract String getRelatedText(String text);
	
	public abstract String getCompleteCode();
	
	public abstract void resetCompleteCode();	
	
	public abstract String getSpokenType();	
}
