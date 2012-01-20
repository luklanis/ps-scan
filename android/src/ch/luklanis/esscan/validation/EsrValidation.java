package ch.luklanis.esscan.validation;

import com.googlecode.leptonica.android.Convert;

public class EsrValidation extends PsValidation {
    private static final int STEP_COUNT = 3;  
    
    private static final int[][] MODULO10 = {
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
    
    private static final int[] CHECK_DIGIT = {
    	0, 9, 8, 7, 6, 5, 4, 3, 2, 1
    };
    
    private static final char[] CONTROL_CHARS_IN_STEP = {
    	'>', '+', '>'
    };
    
    private static final int[][] VALID_LENGTHS_IN_STEP = {
    	{ 4, 14 },
    	{ 40 },  // TODO check length
    	{ 10 }  // TODO check length
    };
    
    private int getCheckDigit(int[] digits){
		int lastValue = 0;
		
		for(int i = 0; i < digits.length; i++){
				lastValue = MODULO10[lastValue][digits[i]];
		}
		
		return CHECK_DIGIT[lastValue];
    }
	
	@Override
	public int getStepCount(){
		return STEP_COUNT;
	}

	@Override
	public boolean validate(String text) {
		if(text.charAt(text.length() - 1) != CONTROL_CHARS_IN_STEP[currentStep] 
				|| (text.length() != VALID_LENGTHS_IN_STEP[currentStep][0] 
						&& text.length() != VALID_LENGTHS_IN_STEP[currentStep][1])){
			return false;
		}
		
		int[] digits = new int[text.length() - 2];
		
		for(int i = 0; i < digits.length; i++){
			
		}
		
		return false;
	}

	@Override
	public String[] getStepHint() {
		switch(getCurrentStep())
		{
		case 1: return new String[]{ "<" };
		case 2: return  new String[]{ "+", "<" };
		case 3: return new String[]{ "<" };

		default: return null;
		}
	}
}
