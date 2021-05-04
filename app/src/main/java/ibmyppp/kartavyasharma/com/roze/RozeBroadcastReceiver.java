package ibmyppp.kartavyasharma.com.roze;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Created by Kartavya Sharma on 02-Aug-17.
 */
public class RozeBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, RozeVoiceService.class );
        serviceIntent.setAction(intent.getAction());
        serviceIntent.putExtra("callerId", intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
        Log.d("ROZE", "Broadcast receiver fired and starting service from " + intent.getAction());
        context.startService(serviceIntent);
    }
}
