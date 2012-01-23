package ch.luklanis.esscan.validation;

import java.util.Currency;

import android.util.Log;

import com.googlecode.leptonica.android.Convert;

public class EsrValidation extends PsValidation {
	private static final String TAG = "ESR Validation";
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
    	{ 28, 17 },  // TODO check length
    	{ 10, -1 }  // TODO check length
    };
    
    private static final String[] STEP_FORMAT = {
    	"%s", "%s", " %s"
    };
    
    private String[] completeCode;
    
    private int getCheckDigit(int[] digits){
		int lastValue = 0;
		
		for(int i = 0; i < digits.length; i++){
				lastValue = MODULO10[lastValue][digits[i]];
		}
		
		return CHECK_DIGIT[lastValue];
    }
    
    public EsrValidation() {
		completeCode = new String[STEP_COUNT];
	}
	
	@Override
	public int getStepCount(){
		return STEP_COUNT;
	}

	@Override
	public boolean validate(String text) {
		try{
			text = getRelatedText(text);	

			if(text.charAt(text.length() - 1) != CONTROL_CHARS_IN_STEP[currentStep] 
					|| (text.length() != VALID_LENGTHS_IN_STEP[currentStep][0] 
							&& text.length() != VALID_LENGTHS_IN_STEP[currentStep][1])){
				return false;
			}

			int[] digits = getDigitsFromText(text, text.length() - 1);
			int[] withoutCheckDigit = new int[digits.length - 1];

			for(int i = 0; i < withoutCheckDigit.length; i++){
				withoutCheckDigit[i] = digits[i];
			}

			int checkDigit = getCheckDigit(withoutCheckDigit);

			if(checkDigit == digits[digits.length - 1]){
				completeCode[currentStep] = String.format(STEP_FORMAT[currentStep], text);
				return true;
			}
		}
		catch(Exception exc)
		{
			Log.e(TAG, exc.toString());
			return false;
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

	@Override
	public String getRelatedText() {
		String text = "";
		
		return getRelatedText(text);
	}

	@Override
	public String getRelatedText(String text) {
		if(text == null && relatedText != null){
			return relatedText;
		}
		
		if(text == null){
			return null;
		}
		
		relatedText = text.replaceAll("\\s", "");
		
		int indexOfCurrentControlChar = relatedText.indexOf(String.valueOf(CONTROL_CHARS_IN_STEP[currentStep]));
		
		if(indexOfCurrentControlChar != -1 && indexOfCurrentControlChar != (relatedText.length() - 1)){
			relatedText = relatedText.substring(0, indexOfCurrentControlChar);
		}
		
		if(currentStep > 1){
			int indexOfControlCharBefore = text.indexOf(String.valueOf(CONTROL_CHARS_IN_STEP[currentStep-1]));
			
			if(indexOfControlCharBefore != -1 && indexOfControlCharBefore < (relatedText.length() - 1)){
				relatedText = relatedText.substring(indexOfControlCharBefore + 1);
			}
		}
		
		return relatedText;
	}

	@Override
	public String getCompleteCode() {
		String result = "";
		
		for(int i = 0; i < completeCode.length; i++){
			if(completeCode[i] != null){
				result += completeCode[i];
			}
		}
		
		return result;
	}
}
