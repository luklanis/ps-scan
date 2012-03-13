package ch.luklanis.esscan.test;

import ch.luklanis.esscan.paymentslip.EsrValidation;
import ch.luklanis.esscan.paymentslip.PsValidation;
import junit.framework.TestCase;

public final class ESRValidationTest extends TestCase {

	private PsValidation psValidation;
	
	private final String exampleCodeRow = "0100003949753>210000000003139471430009017+ 010001628>"; 
	
	public void testInit() {
		psValidation = new EsrValidation();
		
		assertNotNull("Validation class should be initialized", psValidation);
		
		assertTrue("Current Step should be set to the beginning", psValidation.getCurrentStep() == 1);
	}
	
	public void testRelatedTextForFirstStep() {
		psValidation = new EsrValidation();
		
		String relatedText = psValidation.getRelatedText(exampleCodeRow);
		
		assertEquals("Related text should be only the interesting part for the first step", 
				exampleCodeRow.substring(0, 14), relatedText);
	}
	
	public void testRelatedTextForSecondStep() {
		psValidation = new EsrValidation();
		
		psValidation.nextStep();
		
		String relatedText = psValidation.getRelatedText(exampleCodeRow);
		
		assertEquals("Related text should be only the interesting part for the second step", 
				exampleCodeRow.substring(14, 42), relatedText);
	}
	
	public void testRelatedTextForLastStep() {
		psValidation = new EsrValidation();
		
		psValidation.nextStep();
		psValidation.nextStep();
		
		String relatedText = psValidation.getRelatedText(exampleCodeRow);
		
		assertEquals("Related text should be only the interesting part for the last step", 
				exampleCodeRow.substring(43, 53), relatedText);
	}
	
	public void testCorrectValidate() {
		psValidation = new EsrValidation();
		
		String rightFirstPart = exampleCodeRow.substring(0, 16);
		
		assertTrue("Validation of " + rightFirstPart + " should be true", psValidation.validate(rightFirstPart));
	}
	
	public void testIncorrectValidation() {
		psValidation = new EsrValidation();
		
		String falseFirstPart = exampleCodeRow.substring(0, 12) + "4" + exampleCodeRow.substring(14, 16);

		assertFalse("Validation of " + falseFirstPart + " should be false", psValidation.validate(falseFirstPart));
	}
	
	public void testCompleteRowValidation() {
		
		psValidation = new EsrValidation();

	    while(psValidation.validate(exampleCodeRow)){
	    	if(!psValidation.nextStep()){
	    		break;
	    	}
	    }
	    
	    assertEquals("Complete code row == input", exampleCodeRow, psValidation.getCompleteCode());
	}
}
