package com.crewbeat.hondabox.tests;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;import com.crewbeat.hondabox.*;import java.util.*;import java.util.function.BooleanSupplier;
/** Real cross-UID MediaSession callbacks + existing overlay windows. Emulator only, no CAN writes. */
public final class AndroidMediaTests extends Instrumentation {
 private final ProbeState s=ProbeState.INSTANCE;private Context c;private TrackOverlay track;private VolumeOverlay volume;private Activity settings;private int checks;
 private static final String LISTENER="com.crewbeat.hondabox/com.crewbeat.hondabox.AndroidMediaListener";
 @Override public void onCreate(Bundle b){super.onCreate(b);start();}
 private void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 private void main(Runnable r){Throwable[] error={null};runOnMainSync(()->{try{r.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError(error[0]);}
 private void await(BooleanSupplier b,String why){long until=SystemClock.uptimeMillis()+6000;while(!b.getAsBoolean()&&SystemClock.uptimeMillis()<until)SystemClock.sleep(50);check(b.getAsBoolean(),why);}
 private String shell(String command)throws Exception{ParcelFileDescriptor fd=getUiAutomation().executeShellCommand(command);java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);in.close();return out.toString("UTF-8");}
 private String encoded(String s){return s.isEmpty()?"_":android.util.Base64.encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);}
 private void media(int slot,String id,String title,String artist,int state)throws Exception{shell("am start-foreground-service -n com.crewbeat.hondabox.mediafixture/.Player --ei slot "+slot+" --ei state "+state+" --es id64 "+encoded(id)+" --es title64 "+encoded(title)+" --es artist64 "+encoded(artist));}
 private void value(int code,int n){s.event(code,new int[]{n},null,null);}
 private void source(int n){value(98,n);}
 private Object field(Object o,String name){try{java.lang.reflect.Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new AssertionError(e);}}
 private String card(){Object v=field(track,"view");return v instanceof View?String.valueOf(((View)v).getContentDescription()):"";}
 private void restore(SharedPreferences p,Map<String,?> old){SharedPreferences.Editor e=p.edit().clear();for(Map.Entry<String,?> x:old.entrySet()){Object v=x.getValue();if(v instanceof Boolean)e.putBoolean(x.getKey(),(Boolean)v);else if(v instanceof String)e.putString(x.getKey(),(String)v);else if(v instanceof Integer)e.putInt(x.getKey(),(Integer)v);else if(v instanceof Long)e.putLong(x.getKey(),(Long)v);else if(v instanceof Float)e.putFloat(x.getKey(),(Float)v);}e.commit();}
 @Override public void onStart(){Bundle out=new Bundle();int code=0;c=getTargetContext();boolean access=false;Map<String,?> prefs=null,bg=null;String language=I18n.language(c),theme=ThemeSettings.mode(c);
  try{check(Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"),"Emulator only");access=AndroidMediaSettings.access(c);prefs=new HashMap<>(c.getSharedPreferences("overlay",0).getAll());bg=new HashMap<>(c.getSharedPreferences("background",0).getAll());main(()->BackgroundSettings.stop(c));SystemClock.sleep(350);
   shell("cmd notification disallow_listener "+LISTENER);main(AndroidMediaMonitor::stop);s.newSession();s.running(true);s.status("test","synthetic AUX",true);value(1000,6291908);source(3);value(132,12);s.auxSource(3);
   main(()->{AndroidMediaSettings.enabled(c,true);AndroidMediaMonitor.refresh(c);});check(!AndroidMediaMonitor.current().usable(),"permission absent safely empty");
   shell("cmd notification allow_listener "+LISTENER);await(()->AndroidMediaSettings.access(c),"listener access granted");main(()->AndroidMediaMonitor.refresh(c));
   media(0,"a","First Song","Test Artist",3);await(()->AndroidMediaMonitor.current().title.equals("First Song"),"real cross-UID metadata delivered");check(AndroidMediaMonitor.current().playing,"playing state read");
   main(()->{TrackOverlay.enabled(c,true);VolumeOverlay.options(c,true,true);track=new TrackOverlay(c);volume=new VolumeOverlay(c);track.start();volume.start();});
   await(()->card().contains("First Song")&&card().contains("Test Artist"),"Android announcement on AUX");await(()->s.snapshot().overlayText.contains("AUX · First Song · Test Artist"),"volume shows Android title and artist");
   Object first=field(track,"view");long deadline=(Long)field(track,"until");media(0,"a","First Song","Updated Artist",3);await(()->card().contains("Updated Artist"),"late artist refreshes live card");check(first==field(track,"view")&&(Long)field(track,"until")==deadline,"metadata correction does not reannounce or extend");
   media(0,"a","Corrected Title","Updated Artist",3);await(()->card().contains("Corrected Title"),"same media ID refreshes title");check((Long)field(track,"until")==deadline,"same-ID correction does not extend");
   media(0,"a","Corrected Title","Updated Artist",2);await(()->!s.overlaySnapshot().trackOverlayVisible,"pause hides announcement");media(0,"a","Corrected Title","Updated Artist",3);SystemClock.sleep(900);check(!s.overlaySnapshot().trackOverlayVisible,"resume same track does not announce");
   media(0,"b","Second Song","Artist B",3);await(()->card().contains("Second Song"),"next Android track announces");
   main(()->((View)field(track,"view")).performClick());await(()->getUiAutomation().getRootInActiveWindow()!=null&&"com.crewbeat.hondabox.mediafixture".contentEquals(getUiAutomation().getRootInActiveWindow().getPackageName()),"tap opens actual Android player activity");
   for(int source:new int[]{1,2,4}){source(source);await(()->!s.overlaySnapshot().trackOverlayVisible,"known OEM source hides Android card "+source);check(!AndroidMediaRoute.allowed(s.overlaySnapshot()),"OEM source priority "+source);await(()->"".equals(field(field(track,"androidChanges"),"announced")),"announcement detector observes OEM source "+source);await(()->!s.snapshot().overlayText.contains("Second Song"),"OEM volume does not use Android title "+source);}
   source(0);await(()->card().contains("Second Song"),"unknown source uses Android");s.status("test","offline FYT",false);await(()->s.snapshot().overlayText.contains("Android · Second Song"),"CAN disconnect still allows Android metadata");
   s.status("test","Civic",true);value(1000,15729093);value(98,123456);check(AndroidMediaRoute.allowed(s.overlaySnapshot()),"Civic distance is not a source");
   main(()->AndroidMediaSettings.enabled(c,false));await(()->!s.overlaySnapshot().trackOverlayVisible&&!AndroidMediaMonitor.current().usable(),"setting disables and clears Android data");main(()->AndroidMediaSettings.enabled(c,true));await(()->card().contains("Second Song"),"setting re-enables active session");
   media(1,"paused","Old paused title","Other Artist",2);SystemClock.sleep(400);check(AndroidMediaMonitor.current().title.equals("Second Song"),"paused second session cannot steal active player");media(1,"new","Other Player Song","Other Artist",3);await(()->AndroidMediaMonitor.current().title.equals("Other Player Song"),"new playing session selected");await(()->card().contains("Other Player Song"),"session switch announces");
   long until=(Long)field(track,"until");media(1,"new","Other Player Song","Other Artist",3);SystemClock.sleep(800);check((Long)field(track,"until")==until,"repeated playback callback does not announce");
   shell("am start-foreground-service -n com.crewbeat.hondabox.mediafixture/.Player --ei slot 1 --ez release true");await(()->AndroidMediaMonitor.current().title.equals("Second Song"),"destroyed session removed and active player recovered");
   shell("am force-stop com.crewbeat.hondabox.mediafixture");await(()->!AndroidMediaMonitor.current().usable()&&!s.overlaySnapshot().trackOverlayVisible,"player exit clears stale track");
   media(0,"ru","Песня","Исполнитель",3);await(()->card().contains("Песня · Исполнитель"),"Unicode metadata preserved");
   shell("cmd notification disallow_listener "+LISTENER);await(()->!AndroidMediaMonitor.current().usable()&&!s.overlaySnapshot().trackOverlayVisible,"permission revoke clears stale metadata");
   for(String lang:new String[]{"en","ru"}){I18n.language(c,lang);settings=startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();check(settings.findViewById(android.R.id.content).findViewWithTag("android-metadata-enabled") instanceof CheckBox,"independent toggle "+lang);check(settings.findViewById(android.R.id.content).findViewWithTag("android-metadata-access") instanceof Button,"permission button "+lang);main(()->settings.finish());settings=null;}
   main(()->{track.close();volume.close();});s.newSession();s.running(false);
   shell("cmd notification allow_listener "+LISTENER);await(()->AndroidMediaSettings.access(c),"access restored for service test");
   main(()->BackgroundSettings.open(c));await(()->s.overlaySnapshot().running&&AndroidMediaMonitor.current().title.equals("Песня"),"real ProbeService starts metadata monitor");
   main(()->BackgroundSettings.stop(c));await(()->!s.overlaySnapshot().running&&!AndroidMediaMonitor.current().usable(),"service stop removes session callbacks and data");
   out.putString("result","PASS: "+checks+" real Android media/session/settings/overlay checks; no CAN writes");
  }catch(Throwable e){code=1;out.putString("failure",android.util.Log.getStackTraceString(e));}
  finally{if(prefs!=null){main(()->{if(track!=null)track.close();if(volume!=null)volume.close();if(settings!=null)settings.finish();AndroidMediaMonitor.stop();});s.newSession();s.running(false);try{shell("am force-stop com.crewbeat.hondabox.mediafixture");shell("cmd notification "+(access?"allow_listener ":"disallow_listener ")+LISTENER);}catch(Exception ignored){}restore(c.getSharedPreferences("overlay",0),prefs);restore(c.getSharedPreferences("background",0),bg);I18n.language(c,language);ThemeSettings.mode(c,theme);}}
  finish(code,out);
 }
}
