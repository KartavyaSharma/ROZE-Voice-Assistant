package ibmyppp.kartavyasharma.com.roze;

import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Created by Kartavya Sharma on 02-Aug-17.
 */
public class RozeNotificationListenerService extends NotificationListenerService{
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        Intent intent = new  Intent(this, RozeVoiceService.class);
        intent.setAction("STATUS_BAR_NOTIFICATION_RECEIVED");
        intent.putExtra("notification", sbn.getNotification().extras.getString("android.text"));
        this.startService(intent);
        Log.d("ROZE", "Notification hs been posted. Calling service. Message is - " + sbn.getNotification().extras.getString("android.text"));
    }
}
