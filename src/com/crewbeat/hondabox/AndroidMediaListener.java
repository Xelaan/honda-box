package com.crewbeat.hondabox;
import android.service.notification.NotificationListenerService;
/** Permission bridge only. No notification contents are read or stored. */
public final class AndroidMediaListener extends NotificationListenerService {
 @Override public void onListenerConnected(){AndroidMediaMonitor.refresh(this);}
 @Override public void onListenerDisconnected(){AndroidMediaMonitor.stop();}
 @Override public void onDestroy(){AndroidMediaMonitor.stop();super.onDestroy();}
}
