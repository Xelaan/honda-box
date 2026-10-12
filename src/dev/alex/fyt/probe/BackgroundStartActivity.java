package dev.alex.fyt.probe;
import android.app.Activity;
import android.content.Intent;
import android.os.*;
/** NoDisplay launcher entry: DUDU can start the service without opening a screen. */
public final class BackgroundStartActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);
        Intent start=new Intent(this,ProbeService.class).putExtra("backgroundStart",true);
        if(Build.VERSION.SDK_INT>=26)startForegroundService(start);else startService(start);
        finish();
    }
}
