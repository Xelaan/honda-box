package com.crewbeat.hondabox;
import android.content.*;
/** Best-effort manifest fallback; the FYT startup manager remains needed after force-stop. */
public final class FytPowerReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        if(intent==null||!BackgroundRecovery.ACC_ON.equals(intent.getAction()))return;
        if(!BackgroundSettings.mayAutoStart(c))return;
        BackgroundRecovery.notePower(c,true);BackgroundRecovery.start(c,"acc_on");
    }
}
