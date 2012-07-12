package ch.luklanis.esreceiver.datareceived;

import java.util.EventObject;

public class DataReceivedEvent extends EventObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5410646011969650617L;
	private String data;

	public DataReceivedEvent(Object source, String data) {
		super(source);
		this.data = data;
	}
	
	public String getData(){
		return this.data;
	}
}
