package ibmyppp.kartavyasharma.com.roze;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.google.android.gms.location.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Kartavya Sharma on 02-Aug-17.
 */
public class RozeVoiceService extends Service implements TextToSpeech.OnInitListener {

    private FusedLocationProviderClient theFusedLocationClient;
    private boolean serviceStarted = false;
    private boolean isListeningToLocationUpdates = false;
    private LocationCallback theLocationCallback;
    private long lastAnnouncementMillis = SystemClock.elapsedRealtime() - (90 * 1000);
    private long lastLocationChangedEventtMillis = SystemClock.elapsedRealtime() - (10 * 1000);
    private boolean serviceCleanupStarted = false;
    private float speedLimitIn_K_H = 60;
    private float speedForTesting = 0;


    private boolean locationIsBeingProcessed = false;

    private final int LOCATION_UPDATE_INTERVAL = 20 * 1000;

    class MessageToSpeak{
        public String message;
        public boolean queued;
    }

    int textToSpeechStatus = -1;

    public RozeVoiceService() {
    }
    List<MessageToSpeak> myMessages = new ArrayList<MessageToSpeak>();
    TextToSpeech tts;

    @Override
    public void onInit(int status) {
        Log.d("ROZE", "TTS onInit called");
        textToSpeechStatus = status;
        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("ROZE", "This Language is not supported");
            } else {
                speakOut();
            }

        } else {
            Log.e("ROZE", "Initilization Failed!");
        }



    }

    @Override
    public void onCreate() {
        Log.d("ROZE", "Creating message reader service");
        tts = new TextToSpeech(this, this);
        RozeStorage.setup(getApplicationContext());

        theLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onLocationChanged(locationResult.getLastLocation());
            }
        };



        super.onCreate();
    }

    @Override
    public boolean stopService(Intent name) {
        cleanupServie();
        return super.stopService(name);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String currentStatus = RozeStorage.readKey(Constants.ROZE_SHARED_PREFERENCE_KEY_APP_STATUS_NAME);
//        switch(currentStatus){
//            case  Constants.ROZE_SHARED_PREFERENCE_KEY_APP_STATUS_VALUE_STARTED:
//                //do nothing and proceed
//                break;
//            case Constants.ROZE_SHARED_PREFERENCE_KEY_APP_STATUS_VALUE_STOPPED:
//                //return super.onStartCommand(intent, flags, startId);
//        }

        String action = intent.getAction();
        Log.d("ROZE", "service started from " + intent.getAction());
        if(action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)){
            readNumber(intent);
        }else if(action.equals(Constants.ROZE_SERVICE_START_SOURCE_MAIN_APP)){
            //do nothing here but keep condition so it doesn't fall in else. Silly design but....
        }else if(action.equals(Constants.ROZE_SERVICE_START_SOURCE_MAIN_APP_STOP)){
            cleanupServie();
            return super.onStartCommand(intent, flags, startId);
        }
        else {
            readNotifications(intent);
        }
        if(serviceStarted) return super.onStartCommand(intent, flags, startId);
        try{
            speedLimitIn_K_H = Integer.valueOf(RozeStorage.readKey(Constants.ROZE_SHARED_PREFERENCE_KEY_SETTINGS_MAX_SPEED), 10);
        }catch (NumberFormatException e){
            speedLimitIn_K_H = 60;
        }
        try{
            speedForTesting = Integer.valueOf(RozeStorage.readKey(Constants.ROZE_SHARED_PREFERENCE_KEY_SETTINGS_TEST_SPEED), 10);
        }catch (NumberFormatException e){
            speedForTesting = 0;
        }


        if(speedLimitIn_K_H == 0) speedLimitIn_K_H = 60;
        serviceStarted = true;
        theFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        startGPSMonitoring();



        return super.onStartCommand(intent, flags, startId);
    }

    private void cleanupServie(){
        Log.d("ROZE", "Service cleanup started");
        serviceCleanupStarted = true;
        tts.stop();
        tts.shutdown();
        serviceStarted = false;
        stopGPSMonitoring();
        lastAnnouncementMillis = SystemClock.elapsedRealtime() - (90 * 1000);
        locationIsBeingProcessed = false;
    }

    private void onLocationChanged(Location loc) {
        Log.d("ROZE", "OnLocationChanged called - " + SystemClock.elapsedRealtime());
        long millisNow = SystemClock.elapsedRealtime();
        if(millisNow - lastLocationChangedEventtMillis < 2 * 1000) return;
        lastLocationChangedEventtMillis = millisNow;
        if(!locationIsBeingProcessed){
            locationIsBeingProcessed = true;
            if(speedForTesting != 0){
                loc.setSpeed((speedForTesting * 1000)/3600);
            }
            if(loc.hasSpeed()){
                float youSpeed = loc.getSpeed();
                int youSpeedInKM_hr = (int)((youSpeed * 3600)/1000);
                Log.d("ROZE", "Location has speed - " + youSpeedInKM_hr + " km/h" + " and limit is - " + speedLimitIn_K_H);
                Log.d("ROZE", "Is your speed greater than limit - " + (youSpeedInKM_hr > speedLimitIn_K_H));

                if(youSpeedInKM_hr > speedLimitIn_K_H && (millisNow - lastAnnouncementMillis > (60 * 1000)) ){
                    lastAnnouncementMillis = millisNow;
                    MessageToSpeak m = new MessageToSpeak();
                    m.message = "You are going fast at " + youSpeedInKM_hr + " kilometer per hour";
                    myMessages.add(m);
                    Log.d("ROZE", "Calling speak out from onLocationChanged");
                    speakOut();
                }
            } else {
                Log.d("ROZE", "Location has no speed - " + loc.getSpeed());
            }
            locationIsBeingProcessed = false;
        }
    }

    private void startGPSMonitoring(){
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(15000)
                .setFastestInterval(LOCATION_UPDATE_INTERVAL)
                .setInterval(LOCATION_UPDATE_INTERVAL);
        try{
            theFusedLocationClient.requestLocationUpdates(locationRequest, theLocationCallback, Looper.myLooper());
            isListeningToLocationUpdates = true;
            Log.d("ROZE", "GPS monitoring started");
        } catch(SecurityException se){
            Log.d("ROZE", "Failure: GPS monitoring could not be started. Security exception!");
        }
    }

    private void stopGPSMonitoring(){
        if (isListeningToLocationUpdates) {
            theFusedLocationClient.removeLocationUpdates(theLocationCallback);
            Log.d("ROZE", "GPS Monitoring stopped");
            isListeningToLocationUpdates = false;
        }
    }

    private void readNumber(Intent intent){
        String callerId = intent.getStringExtra("callerId");
        Log.d("ROZE", "Call received from - " + callerId);
        MessageToSpeak d = new MessageToSpeak();
        d.message = intent.getStringExtra("callerId" + "Called you and I disconnected because you are driving");
        speakOut();
    }

    private void readNotifications(Intent intent){
        Log.d("ROZE", "Received message has text - " + intent.getStringExtra("notification"));
        MessageToSpeak n = new MessageToSpeak();
        n.message = intent.getStringExtra("notification");
        n.queued = false;
        myMessages.add(n);
        speakOut();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    private void speakOut() {
        if(serviceCleanupStarted) return;
        for (MessageToSpeak n : myMessages
                ) {
            if(textToSpeechStatus == TextToSpeech.SUCCESS && !n.queued){
                n.queued = true;
                if(!serviceCleanupStarted){
                    tts.speak(n.message, TextToSpeech.QUEUE_ADD, null);
               }
            }
        }
    }


}
