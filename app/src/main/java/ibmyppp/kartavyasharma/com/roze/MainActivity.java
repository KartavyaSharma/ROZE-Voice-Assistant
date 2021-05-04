package ibmyppp.kartavyasharma.com.roze;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button buttonStartRoze;
    Button buttonStopRoze;
    EditText editTextSpeed;
    EditText editTextTestSpeed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonStartRoze = (Button) findViewById(R.id.buttonStart);
        buttonStartRoze.setOnClickListener(this);
        buttonStopRoze = (Button) findViewById(R.id.buttonStop);
        buttonStopRoze.setOnClickListener(this);
        editTextSpeed = (EditText) findViewById(R.id.editTextSpeed);
        editTextTestSpeed = (EditText) findViewById(R.id.editTextTestSpeed);

        RozeStorage.setup(getApplicationContext());
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        List<String> languages = new ArrayList<String>();
        languages.add("English");
        languages.add("German");
        languages.add("Hindi");
        languages.add("Japanese");
        languages.add("Kannada");
        languages.add("Spanish");
        languages.add("Tamil");
        ArrayAdapter<String> langArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, languages);
        langArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(langArrayAdapter);
    }


    @Override
    public void onClick(View v) {

        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName(this, RozeVoiceService.class);

        switch(v.getId()){
            case R.id.buttonStart:
                RozeStorage.setKey(Constants.ROZE_SHARED_PREFERENCE_KEY_SETTINGS_MAX_SPEED, editTextSpeed.getText().toString());
                RozeStorage.setKey(Constants.ROZE_SHARED_PREFERENCE_KEY_SETTINGS_TEST_SPEED, editTextTestSpeed.getText().toString());
                packageManager.setComponentEnabledSetting(componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                Intent intent = new Intent(getApplicationContext(), RozeVoiceService.class);
                intent.setAction(Constants.ROZE_SERVICE_START_SOURCE_MAIN_APP);
                getApplicationContext().stopService(intent);
                getApplicationContext().startService(intent);
                showToast("Start Roze");
                RozeStorage.setKey(Constants.ROZE_SHARED_PREFERENCE_KEY_APP_STATUS_NAME, Constants.ROZE_SHARED_PREFERENCE_KEY_APP_STATUS_VALUE_STARTED);

                break;
            case R.id.buttonStop:
                Log.d("ROZE","Service stop requested!");
                Intent listenerServiceIntnet = new  Intent(this, RozeVoiceService.class);
                listenerServiceIntnet.setAction(Constants.ROZE_SERVICE_START_SOURCE_MAIN_APP_STOP);
                ComponentName serviceComponentName = getApplicationContext().startService(listenerServiceIntnet);
                if(serviceComponentName != null){
                    boolean result = getApplicationContext().stopService(listenerServiceIntnet);
                    if(result){
                        packageManager.setComponentEnabledSetting(componentName,
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                        showToast("Stop Roze");
                        RozeStorage.setKey(Constants.ROZE_SHARED_PREFERENCE_KEY_APP_STATUS_NAME, Constants.ROZE_SHARED_PREFERENCE_KEY_APP_STATUS_VALUE_STARTED);
                    }
                } else {
                    Log.d("ROZE","Service could not be stopped, so disable would not work!");
                }
                break;
        }
    }

    private void showToast(String msg){
        Toast.makeText(getApplicationContext(), msg,
                Toast.LENGTH_LONG).show();    }
}
