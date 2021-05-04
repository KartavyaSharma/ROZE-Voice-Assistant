package ibmyppp.kartavyasharma.com.roze;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Kartavya Sharma on 02-Aug-17.
 */
public class RozeStorage {
    private static SharedPreferences sharedPref;
    public static void setup(Context ctx){
        sharedPref = ctx.getSharedPreferences(Constants.ROZE_SHARED_PREFERENCE_NAME , Context.MODE_PRIVATE);
        if(!sharedPref.contains(Constants.ROZE_SHARED_PREFERENCE_KEY_APP_STATUS_NAME)){
            RozeStorage.setKey(Constants.ROZE_SHARED_PREFERENCE_KEY_APP_STATUS_NAME, Constants.ROZE_SHARED_PREFERENCE_KEY_APP_STATUS_VALUE_STOPPED);
        }
    }

    public static String readKey(String key){
        if(sharedPref != null){
            return sharedPref.getString(key, "");
        }
        return "";
    }

    public static void setKey(String key, String value){
        if(sharedPref != null){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(key, value);
            editor.commit();
        }
    }
}
