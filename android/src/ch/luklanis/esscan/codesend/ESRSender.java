package ch.luklanis.esscan.codesend;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
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
//	private AcceptSocketsAsync acceptSocketsAsync = null;

	private ServerSocket server;
	private ArrayList<Socket> sockets;
	private Handler handler = new Handler();

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return this.binder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		super.onStartCommand(intent, flags, startId);
		
		this.sockets = new ArrayList<Socket>();

		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo info = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

//				if (!info.isConnected()) {
//					Log.w(TAG, "Wifi is not connected!");
//					return false;
//				}

				try {
					server = new ServerSocket(SERVER_PORT);

					while(true) {
						Socket socket = server.accept();
						sockets.add(socket);
					}


				} catch (IOException e) {
					Log.e(TAG, "Open a server socket failed!", e);
				} 
			}
		};
		
		new Thread(runnable).start();
//		if (acceptSocketsAsync == null) {
//			acceptSocketsAsync = new AcceptSocketsAsync(getApplicationContext(), SERVER_PORT);
//			acceptSocketsAsync.execute("");
//		}

		return START_STICKY;
	}

	public void sendToListeners(String... messages) {
//		ArrayList<Socket> sockets = this.acceptSocketsAsync.getAcceptedSockets();

		ArrayList<DataOutputStream> dataOutputStreams = new ArrayList<DataOutputStream>();

		for(Socket socket : this.sockets) {
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
//		acceptSocketsAsync.cancel(true);
//		acceptSocketsAsync = null;
		try {
			this.server.close();
			
			for (Socket socket : this.sockets){
				socket.close();
			}
		} catch (IOException e) {
		}
		
		super.onDestroy();
	}
}
