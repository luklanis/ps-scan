package ch.luklanis.esreceiver.datareceived;

import java.util.EventListener;

public interface OnDataReceivedListener extends EventListener {
	public void dataReceived(DataReceivedEvent event);
}
