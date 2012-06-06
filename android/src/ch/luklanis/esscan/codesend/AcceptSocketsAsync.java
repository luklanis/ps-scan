package ch.luklanis.esscan.codesend;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

public class AcceptSocketsAsync extends AsyncTask<String, Integer, Boolean> {

	private static final String TAG = AcceptSocketsAsync.class.getPackage().getName() + "." + AcceptSocketsAsync.class.getName();
	private Context context;
	private int port;

	private ServerSocket server;
	private ArrayList<Socket> sockets;

	public AcceptSocketsAsync(Context context, int port){
		this.context = context;
		this.port = port;
		
		this.sockets = new ArrayList<Socket>();
	}

	public ArrayList<Socket> getAcceptedSockets() {
		return this.sockets;
	}

	@Override
	protected Boolean doInBackground(String... params) {
		ConnectivityManager connManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

//		if (!info.isConnected()) {
//			Log.w(TAG, "Wifi is not connected!");
//			return false;
//		}

		try {
			this.server = new ServerSocket(port);

			while(true) {
				Socket socket = this.server.accept();
				this.sockets.add(socket);
			}


		} catch (IOException e) {
			Log.e(TAG, "Open a server socket failed!", e);
		}

		return true;
	}

	@Override
	protected void onCancelled() {

		if (!this.server.isClosed()) {
			for(Socket socket : this.sockets){
				try {
					if(!socket.isClosed()){
						socket.close();
					}
				} catch (IOException e) {
				}
			}

			try {
				this.server.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		super.onCancelled();
	}

}
