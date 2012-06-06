package ch.luklanis.esscan.codesend;

import java.io.DataOutputStream;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.util.Log;

public class SendMessageAsync extends AsyncTask<String, Integer, Boolean> {
	
	private static final String TAG = SendMessageAsync.class.getPackage().getName() + "." + SendMessageAsync.class.getName();
	private ArrayList<DataOutputStream> dataOutputStreams;

	public SendMessageAsync(ArrayList<DataOutputStream> dataOutputStreams) {
		this.dataOutputStreams = dataOutputStreams;
	}

	@Override
	protected Boolean doInBackground(String... params) {

		for(DataOutputStream dataOutputStream : dataOutputStreams) {
			try {
				dataOutputStream.writeUTF(params[0]);
			} catch (Exception e) {
				Log.e(TAG, "Send code row failed", e);
			}
		}
		
		return true;
	}

}
