package com.crewbeat.hondabox;
import android.app.Activity;
import android.content.Intent;
import android.os.*;
/** NoDisplay launcher entry: DUDU can start the service without opening a screen. */
public final class BackgroundStartActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);
        // FYT may invoke the launcher automatically. Resume a deliberate pause from the UI instead.
        if(BackgroundSettings.mayAutoStart(this)){BackgroundRecovery.notePower(this,true);BackgroundRecovery.start(this,"fyt_launcher");}
        finish();
    }
}
