package ch.luklanis.esscan.codesend;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

	private ServerSocket server;
	private ArrayList<Socket> sockets;

	@Override
	public IBinder onBind(Intent intent) {
		return this.binder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		this.sockets = new ArrayList<Socket>();

		if(isConnectedLocal()) {
			try {
				server = new ServerSocket(SERVER_PORT);
			} catch (IOException e) {
				Log.e(TAG, "Open a server socket failed!", e);
			}

			Runnable runnable = new Runnable() {
				@Override
				public void run() {

					while(!server.isClosed()) {
						Socket socket;
						try {
							socket = server.accept();
							sockets.add(socket);
						} catch (IOException e) {
						}
					} 
				}
			};

			new Thread(runnable).start();
		}

		return START_STICKY;
	}

	public boolean isConnectedLocal() {
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//		NetworkInfo info = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		NetworkInfo[] allNetworkInfo = connManager.getAllNetworkInfo();

		for (int i = 0; i < allNetworkInfo.length; i++) {
			NetworkInfo info = allNetworkInfo[i];
			int type = info.getType();

			if (info.isAvailable() && info.isConnected() 
					&& (type == ConnectivityManager.TYPE_BLUETOOTH
					|| type == ConnectivityManager.TYPE_DUMMY
					|| type == ConnectivityManager.TYPE_ETHERNET
					|| type == ConnectivityManager.TYPE_WIFI)) {
				return true;
			}
		}

		return getLocalIpAddress() != null;

//		if (!info.isConnected()) {
//			Log.w(TAG, "Wifi is not connected!");
//			return false;
//		}
//
//		return true;
	}

	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4) {
						return inetAddress.getHostAddress();
					}
				}
			}
		} catch (SocketException ex) {
			Log.e(TAG, ex.toString());
		}
		return null;
	}

	public void sendToListeners(String... messages) {
		ArrayList<DataOutputStream> dataOutputStreams = new ArrayList<DataOutputStream>();

		for(Socket socket : this.sockets) {
			try {
				if (!socket.isClosed()) {
					dataOutputStreams.add(new DataOutputStream(socket.getOutputStream()));
				} 
			} catch (IOException e) {
				try {
					socket.close();
				} catch (IOException ex) {
				}
			}
		}

		SendMessageAsync sendMessageAsync = new SendMessageAsync(dataOutputStreams);
		sendMessageAsync.execute(messages);
	}

	@Override
	public void onDestroy() {
		stopServer();

		super.onDestroy();
	}

	public void stopServer() {
		try {
			if (this.server != null && !this.server.isClosed()) {
				this.server.close();
			}

			for (Socket socket : this.sockets){
				socket.close();
			}
		} catch (IOException e) {
		}
	}
}
