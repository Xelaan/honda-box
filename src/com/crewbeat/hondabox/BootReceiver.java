package com.crewbeat.hondabox;
import android.content.*;
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        String action=intent==null?null:intent.getAction();
        if(!Intent.ACTION_BOOT_COMPLETED.equals(action)&&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))return;
        if(!BackgroundSettings.mayAutoStart(c))return;
        if(Intent.ACTION_BOOT_COMPLETED.equals(action))BackgroundRecovery.notePower(c,true);
        BackgroundRecovery.start(c,Intent.ACTION_BOOT_COMPLETED.equals(action)?"boot":"package_replaced");
    }
}
