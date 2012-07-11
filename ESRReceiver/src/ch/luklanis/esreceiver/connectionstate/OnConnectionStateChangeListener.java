package ch.luklanis.esreceiver.connectionstate;

import java.util.EventListener;

public interface OnConnectionStateChangeListener extends EventListener {
	public void connectionStateChanged(ConnectionStateChangedEvent event);
}
