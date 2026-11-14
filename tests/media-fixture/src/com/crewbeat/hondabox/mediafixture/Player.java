package com.crewbeat.hondabox.mediafixture;
import android.app.*;import android.content.*;import android.media.*;import android.media.session.*;import android.os.*;
/** Test-only cross-UID active media sessions; publishes metadata without playing any sound. */
public final class Player extends Service {
 private final MediaSession[] sessions=new MediaSession[2];
 private String decoded(Intent i,String key){String v=i.getStringExtra(key+"64");return v==null||v.equals("_")?"":new String(android.util.Base64.decode(v,android.util.Base64.DEFAULT),java.nio.charset.StandardCharsets.UTF_8);}
 @Override public IBinder onBind(Intent i){return null;}
 @Override public void onCreate(){super.onCreate();if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish")){stopSelf();return;}if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("test","TEST Media",NotificationManager.IMPORTANCE_LOW));}
 @Override public int onStartCommand(Intent i,int flags,int id){if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish")){stopSelf();return START_NOT_STICKY;}
  Notification.Builder n=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"test"):new Notification.Builder(this);startForeground(91,n.setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("TEST Android MediaSession").build());if(i==null)return START_NOT_STICKY;
  int slot=Math.max(0,Math.min(1,i.getIntExtra("slot",0)));if(i.getBooleanExtra("release",false)){if(sessions[slot]!=null){sessions[slot].release();sessions[slot]=null;}return START_NOT_STICKY;}
  if(sessions[slot]==null){sessions[slot]=new MediaSession(this,"Honda Android media test "+slot);sessions[slot].setSessionActivity(PendingIntent.getActivity(this,slot,new Intent(this,PlayerScreen.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));sessions[slot].setActive(true);}
  MediaSession s=sessions[slot];if(i.hasExtra("title64")){MediaMetadata.Builder m=new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,decoded(i,"title")).putString(MediaMetadata.METADATA_KEY_ARTIST,decoded(i,"artist"));if(i.hasExtra("id64"))m.putString(MediaMetadata.METADATA_KEY_MEDIA_ID,decoded(i,"id"));s.setMetadata(m.build());}
  s.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE).setState(i.getIntExtra("state",PlaybackState.STATE_PLAYING),i.getLongExtra("position",0),1).build());return START_NOT_STICKY;
 }
 @Override public void onDestroy(){for(MediaSession s:sessions)if(s!=null)s.release();super.onDestroy();}
}
