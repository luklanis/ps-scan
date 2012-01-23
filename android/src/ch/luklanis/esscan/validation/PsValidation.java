package ch.luklanis.esscan.validation;

import java.io.IOException;

public abstract class PsValidation {
    private static final int STEP_COUNT = 1;  
	protected int currentStep;
	
	protected String relatedText;
	private boolean finished;
	
	public PsValidation() {
		gotoBeginning();
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
	
	public void gotoBeginning(){
		currentStep = 0;
		finished = false;
		relatedText = null;
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
	
	public abstract String[] getStepHint();
	
	public abstract String getRelatedText();	
	
	public abstract String getRelatedText(String text);
	
	public abstract String getCompleteCode();	
}
