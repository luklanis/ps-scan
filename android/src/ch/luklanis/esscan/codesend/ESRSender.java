package ch.luklanis.esscan.codesend;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ESRSender extends Service {
	
	public class LocalBinder extends Binder {
		public ESRSender getService() {
			return ESRSender.this;
		}
	}

	private static final int SERVER_PORT = 8765;
	private static final String TAG = ESRSender.class.getPackage().getName() + "." + ESRSender.class.getName();
	private final IBinder binder = new LocalBinder();
	private AcceptSocketsAsync acceptSocketsAsync;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return this.binder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		super.onStartCommand(intent, flags, startId);
		
		acceptSocketsAsync = new AcceptSocketsAsync(getApplicationContext(), SERVER_PORT);
		acceptSocketsAsync.execute("");
		
		return START_STICKY;
	}
	
	public void sendToListeners(String... messages) {
		ArrayList<Socket> sockets = this.acceptSocketsAsync.getAcceptedSockets();
		
		ArrayList<DataOutputStream> dataOutputStreams = new ArrayList<DataOutputStream>();
		
		for(Socket socket : sockets) {
			try {
				dataOutputStreams.add(new DataOutputStream(socket.getOutputStream()));
			} catch (IOException e) {
				Log.w(TAG, "Failed to get a output stream");
			}
		}
		
		SendMessageAsync sendMessageAsync = new SendMessageAsync(dataOutputStreams);
		sendMessageAsync.execute(messages);
	}

	@Override
	public void onDestroy() {
		acceptSocketsAsync.cancel(true);
		super.onDestroy();
	}
}
