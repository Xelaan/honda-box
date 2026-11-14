package com.crewbeat.hondabox;
import android.app.PendingIntent;import android.content.*;import android.media.MediaMetadata;import android.media.session.*;import android.os.*;import java.util.*;
/** Event-driven active-session reader. Runs only while Honda Box is running and enabled. */
public final class AndroidMediaMonitor {
 private static AndroidMediaMonitor instance;private static volatile AndroidTrack current=AndroidTrack.EMPTY;
 private final Context context;private final MediaSessionManager manager;private final Handler ui=new Handler(Looper.getMainLooper());
 private final Map<MediaSession.Token,Watch> watches=new LinkedHashMap<>();private MediaController selected;
 private final MediaSessionManager.OnActiveSessionsChangedListener sessions=this::sessions;
 private boolean stopped;
 private AndroidMediaMonitor(Context c){context=c.getApplicationContext();manager=(MediaSessionManager)c.getSystemService(Context.MEDIA_SESSION_SERVICE);}
 public static AndroidTrack current(){return current;}
 public static void refresh(Context c){if(Looper.myLooper()!=Looper.getMainLooper()){new Handler(Looper.getMainLooper()).post(()->refresh(c));return;}
  if(!ProbeState.INSTANCE.overlaySnapshot().running||!AndroidMediaSettings.enabled(c)||!AndroidMediaSettings.access(c)){stop();return;}
  if(instance!=null)return;AndroidMediaMonitor next=new AndroidMediaMonitor(c);instance=next;
  try{ComponentName component=new ComponentName(c,AndroidMediaListener.class);next.manager.addOnActiveSessionsChangedListener(next.sessions,component,next.ui);next.sessions(next.manager.getActiveSessions(component));}
  catch(RuntimeException e){stop();ProbeState.INSTANCE.log("ANDROID METADATA: session access unavailable ("+e.getClass().getSimpleName()+")");}
 }
 public static void stop(){if(Looper.myLooper()!=Looper.getMainLooper()){new Handler(Looper.getMainLooper()).post(AndroidMediaMonitor::stop);return;}
  AndroidMediaMonitor old=instance;instance=null;current=AndroidTrack.EMPTY;if(old==null)return;old.stopped=true;
  try{old.manager.removeOnActiveSessionsChangedListener(old.sessions);}catch(RuntimeException ignored){}
  for(Watch w:old.watches.values())w.close();old.watches.clear();old.selected=null;
 }
 private final class Watch extends MediaController.Callback {
  final MediaController controller;Watch(MediaController c){controller=c;}
  void close(){try{controller.unregisterCallback(this);}catch(RuntimeException ignored){}}
  @Override public void onMetadataChanged(MediaMetadata metadata){publish();}
  @Override public void onPlaybackStateChanged(PlaybackState state){publish();}
  @Override public void onSessionDestroyed(){watches.remove(controller.getSessionToken());close();publish();}
 }
 private void sessions(List<MediaController> controllers){if(stopped)return;Set<MediaSession.Token> present=new HashSet<>();
  if(controllers!=null)for(MediaController c:controllers){MediaSession.Token token=c.getSessionToken();present.add(token);if(!watches.containsKey(token)){Watch w=new Watch(c);try{c.registerCallback(w,ui);watches.put(token,w);}catch(RuntimeException ignored){}}}
  Iterator<Map.Entry<MediaSession.Token,Watch>> i=watches.entrySet().iterator();while(i.hasNext()){Map.Entry<MediaSession.Token,Watch> e=i.next();if(!present.contains(e.getKey())){e.getValue().close();i.remove();}}
  publish();
 }
 private static boolean playing(PlaybackState p){if(p==null)return false;int s=p.getState();return s==PlaybackState.STATE_PLAYING||s==PlaybackState.STATE_FAST_FORWARDING||s==PlaybackState.STATE_REWINDING;}
 private void publish(){if(stopped)return;MediaController best=null;int bestRank=-1;long bestTime=-1;
  for(Watch w:watches.values())try{PlaybackState p=w.controller.getPlaybackState();int rank=playing(p)?3:p!=null&&(p.getState()==PlaybackState.STATE_BUFFERING||p.getState()==PlaybackState.STATE_CONNECTING)?2:p!=null&&p.getState()==PlaybackState.STATE_PAUSED?1:0;
   if(rank==0)continue;long time=p.getLastPositionUpdateTime();boolean wasSelected=selected!=null&&selected.getSessionToken().equals(w.controller.getSessionToken());
   if(rank>bestRank||rank==bestRank&&(time>bestTime||time==bestTime&&wasSelected)){best=w.controller;bestRank=rank;bestTime=time;}
  }catch(RuntimeException ignored){}
  selected=best;if(best==null){current=AndroidTrack.EMPTY;return;}
  try{MediaMetadata m=best.getMetadata();if(m==null){current=AndroidTrack.EMPTY;return;}String title=m.getString(MediaMetadata.METADATA_KEY_TITLE);if(title==null||title.trim().isEmpty())title=m.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
   String artist=m.getString(MediaMetadata.METADATA_KEY_ARTIST);if(artist==null||artist.trim().isEmpty())artist=m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
   String pkg=best.getPackageName(),name=current.packageName.equals(pkg)?current.appName:pkg;if(name.equals(pkg))try{name=context.getPackageManager().getApplicationLabel(context.getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception ignored){}
   current=new AndroidTrack(pkg,name,m.getString(MediaMetadata.METADATA_KEY_MEDIA_ID),title,artist,playing(best.getPlaybackState()));
  }catch(RuntimeException e){current=AndroidTrack.EMPTY;}
 }
 /** Only an explicit tap opens a player; no media controls are sent. */
 public static void open(Context context,String expectedPackage){AndroidMediaMonitor monitor=instance;if(monitor==null||monitor.selected==null||!current.packageName.equals(expectedPackage))return;
  try{PendingIntent launch=monitor.selected.getSessionActivity();if(launch!=null){launch.send();return;}Intent intent=context.getPackageManager().getLaunchIntentForPackage(expectedPackage);if(intent!=null)context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
  catch(PendingIntent.CanceledException|RuntimeException e){ProbeState.INSTANCE.log("ANDROID PLAYER OPEN: "+e.getClass().getSimpleName());}
 }
}
