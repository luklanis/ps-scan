package ch.luklanis.esscan.validation;

public abstract class PsValidation {
    private static final int STEP_COUNT = 1;  
	protected int currentStep;
	
	public int getStepCount(){
		return STEP_COUNT;
	}
	
	public int getCurrentStep(){
		return currentStep;
	}
	
	public boolean setNextStep(){
		if (currentStep < getStepCount()){
			currentStep++;
			return true;
		}
		
		return false;
	}

	public abstract boolean validate(String text);
	
	public abstract String[] getStepHint();
}
