package de.clemensloos.elan.sender;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Vibrator;

public class Utils {

	
    public static void showDialog(String message, Activity activity, Vibrator myVib) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setMessage(message)
		       .setCancelable(false)
		       .setPositiveButton(activity.getResources().getString(R.string.label_ok), new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                
		           }
		       })
		       ;
		AlertDialog errorDialog = builder.create();
		if (myVib != null) {
			myVib.vibrate(new long[] {0, 100, 100, 100}, -1);
		}
		errorDialog.show();
    }
    
    
    
	public static boolean isWiFiNetworkAvailable(Activity activity) {
	    ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    // if no network is available networkInfo will be null
	    // otherwise check if we are connected
	    if (networkInfo != null && networkInfo.isConnected()) {
	        return true;
	    }
	    return false;
	}
	
}
