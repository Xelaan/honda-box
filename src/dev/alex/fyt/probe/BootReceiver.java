package dev.alex.fyt.probe;
import android.content.*;
import android.os.Build;
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){String action=intent==null?null:intent.getAction();if(!Intent.ACTION_BOOT_COMPLETED.equals(action)&&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))return;if(!BackgroundSettings.mayAutoStart(c))return;
        Intent start=new Intent(c,ProbeService.class).putExtra("backgroundStart",true);try{if(Build.VERSION.SDK_INT>=26)c.startForegroundService(start);else c.startService(start);}catch(RuntimeException e){ProbeState.INSTANCE.log("AUTOSTART ERROR: "+e);}
    }
}
