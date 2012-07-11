package ch.luklanis.esreceiver.connectionstate;

import java.util.EventObject;

public class ConnectionStateChangedEvent extends EventObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4569709366192446455L;
	private ConnectionState state;
	
	public ConnectionStateChangedEvent(Object source, ConnectionState state){
		super(source);
		
		this.state = state;
	}
	
	public ConnectionState getConnectionState() {
		return this.state;
	}
}
