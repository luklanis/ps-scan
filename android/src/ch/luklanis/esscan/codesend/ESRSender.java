package ch.luklanis.esscan.codesend;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class ESRSender extends Service {
	
	public static final boolean EXISTS = false;

	public class LocalBinder extends Binder {
		public ESRSender getService() {
			return ESRSender.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isConnectedLocal() {
		return false;
	}

	public void stopServer() {
	}

	public String getLocalIpAddress() {
		return "";
	}

	public boolean sendToListeners(String... messages) {
		return false;
	}
}
