package ch.luklanis.esscan.codesend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StopServiceReceiver extends BroadcastReceiver {
	
	private static final String TAG = StopServiceReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "Shutdown payment slip stream service");
		
		Intent stopIntent = new Intent(context, ESRSender.class);
		context.stopService(stopIntent);
	}
}