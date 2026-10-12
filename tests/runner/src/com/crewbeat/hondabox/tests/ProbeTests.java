package com.crewbeat.hondabox.tests;
import android.app.*;
import android.content.*;
import android.os.*;
import com.crewbeat.hondabox.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import android.view.*;
import android.widget.*;
import android.graphics.Bitmap;

public class ProbeTests extends Instrumentation {
    private String phase; private int checks;
    interface Condition {boolean ok();}
    @Override public void onCreate(Bundle args){super.onCreate(args);phase=args.getString("phase","live");start();}
    private ProbeState.Snapshot state(){return ProbeState.INSTANCE.snapshot();}
    private void await(String label,Condition condition)throws Exception{long end=SystemClock.uptimeMillis()+16000;while(SystemClock.uptimeMillis()<end){if(condition.ok()){checks++;return;}SystemClock.sleep(100);}throw new AssertionError(label+" | "+state().status+" | "+state().detail);}
    private void check(String label,boolean ok){if(!ok)throw new AssertionError(label);checks++;}
    private void command(Intent i){getTargetContext().sendBroadcast(i.setComponent(new ComponentName("com.syu.ms","app.ControlReceiver")));}
    private void startProbe(){Context c=getTargetContext();Intent i=new Intent(c,ProbeService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
    private void stopProbe(){BackgroundSettings.stop(getTargetContext());}
    private JSONArray receipts(boolean reset) throws Exception {
        CountDownLatch latch = new CountDownLatch(1); AtomicReference<String> result = new AtomicReference<>();
        getTargetContext().sendOrderedBroadcast(new Intent().setComponent(new ComponentName("com.syu.ms","app.ControlReceiver"))
            .putExtra(reset?"resetCommands":"getCommands",true),null,new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){result.set(getResultData());latch.countDown();}},null,Activity.RESULT_OK,null,null);
        if(!latch.await(5,TimeUnit.SECONDS))throw new AssertionError("fixture receipt timeout");
        return new JSONArray(result.get());
    }
    private void audio(String action){getTargetContext().startService(new Intent(getTargetContext(),ProbeService.class).setAction(ProbeService.COMMAND).putExtra("command",action));}
    private Button button(View view,String text) {
        if(view instanceof Button && ((Button)view).getText().toString().equals(text))return (Button)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){Button found=button(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}
        return null;
    }
    private void uiTest(Activity activity)throws Exception {
        receipts(true);runOnMainSync(()->{Button b=button(activity.findViewById(android.R.id.content),"CD");check("CD button exists",b!=null);check("connected CD button enabled",b.isEnabled());b.performClick();});
        await("UI click starts command",()->state().commandBusy);await("UI click completes transport",()->!state().commandBusy);
        JSONArray r=receipts(false);check("UI click sent exactly source plan",r.length()==2);step(r,0,0,0,13);step(r,1,7,1,1);
        runOnMainSync(()->{Button b=button(activity.findViewById(android.R.id.content),"CD");b.requestRectangleOnScreen(new android.graphics.Rect(0,0,b.getWidth(),b.getHeight()),true);});
        SystemClock.sleep(600);Bitmap screen=getUiAutomation().takeScreenshot();check("UI screenshot available",screen!=null);
        File output=new File(getTargetContext().getExternalFilesDir(null),"probe-v04-controls-TEST.png");
        try(OutputStream out=new FileOutputStream(output)){screen.compress(Bitmap.CompressFormat.PNG,100,out);}screen.recycle();
    }
    private boolean contains(View view,String value){
        if(view instanceof TextView&&((TextView)view).getText().toString().contains(value))return true;
        if(view.getContentDescription()!=null&&view.getContentDescription().toString().contains(value))return true;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(contains(((ViewGroup)view).getChildAt(i),value))return true;
        return false;
    }
    private void screenshot(String name)throws Exception {waitForIdleSync();SystemClock.sleep(500);Bitmap screen=getUiAutomation().takeScreenshot();check("screenshot "+name,screen!=null);try(OutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),name))){screen.compress(Bitmap.CompressFormat.PNG,100,out);}screen.recycle();}
    private void click(Activity a,String name){runOnMainSync(()->{Button b=button(a.findViewById(android.R.id.content),name);check("button "+name,b!=null);b.performClick();});}
    private void editable(android.view.accessibility.AccessibilityNodeInfo root,java.util.List<android.view.accessibility.AccessibilityNodeInfo> list){if(root.isEditable())list.add(root);for(int i=0;i<root.getChildCount();i++){android.view.accessibility.AccessibilityNodeInfo child=root.getChild(i);if(child!=null)editable(child,list);}}
    private void editCoordinates(String lat,String lon){java.util.List<android.view.accessibility.AccessibilityNodeInfo> fields=new java.util.ArrayList<>();editable(getUiAutomation().getRootInActiveWindow(),fields);check("two editable coordinates",fields.size()==2);
        for(int i=0;i<2;i++){Bundle args=new Bundle();args.putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,i==0?lat:lon);check("coordinate text entered",fields.get(i).performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,args));}}
    private void clickAccessibility(String label){java.util.List<android.view.accessibility.AccessibilityNodeInfo> nodes=getUiAutomation().getRootInActiveWindow().findAccessibilityNodeInfosByText(label);check("dialog button found",!nodes.isEmpty());check("dialog button clicked",nodes.get(0).performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK));}
    private void themeTests(Activity diagnostic)throws Exception {
        Context c=getTargetContext();receipts(true);SourceOverlay.enabled(c,false);overlayOptions(false,false);
        c.getSharedPreferences("theme",Context.MODE_PRIVATE).edit().remove("lat").remove("lon").putBoolean("gps",false).apply();
        runOnMainSync(()->ThemeSettings.mode(c,ThemeSettings.DAY));
        Activity panel=startActivitySync(new Intent(c,AudioPlayerActivity.class).putExtra("mode","RADIO").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        await("live panel source",()->contains(panel.findViewById(android.R.id.content),"FM2"));
        check("day palette",!ThemeSettings.resolve(c).palette.night);SystemClock.sleep(700);screenshot("probe-v051-day-TEST.png");
        check("theme controls only in main",button(panel.findViewById(android.R.id.content),"Ночь")==null);click(diagnostic,"Ночь");check("forced night persists",ThemeSettings.mode(c).equals(ThemeSettings.NIGHT)&&ThemeSettings.resolve(c).palette.night);SystemClock.sleep(700);screenshot("probe-v051-night-TEST.png");
        Activity settings=startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK));click(settings,"Место");await("location dialog visible",()->{android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();return root!=null&&!root.findAccessibilityNodeInfosByText("Местоположение для темы").isEmpty();});
        editCoordinates("100","0");clickAccessibility("Сохранить");SystemClock.sleep(250);check("invalid latitude leaves dialog open",!getUiAutomation().getRootInActiveWindow().findAccessibilityNodeInfosByText("Местоположение для темы").isEmpty());
        editCoordinates("0","0");clickAccessibility("Сохранить");await("manual zero coordinates accepted",()->ThemeSettings.hasLocation(c)&&ThemeSettings.latitude(c)==0&&ThemeSettings.longitude(c)==0);
        check("manual entry requires no location permission",c.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED&&!ThemeSettings.gps(c));
        runOnMainSync(()->panel.recreate());SystemClock.sleep(700);check("manual choice survives recreation",ThemeSettings.resolve(c).palette.night);
        // Exercise the astronomical preference and cache at precise transitions using a public sample location.
        ThemeSettings.location(c,40.7128,-74.006,false);ThemeSettings.mode(c,ThemeSettings.AUTO);
        java.util.TimeZone zone=java.util.TimeZone.getTimeZone("America/New_York");
        java.text.SimpleDateFormat date=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",java.util.Locale.US);date.setTimeZone(zone);
        long noon=date.parse("2026-10-07 12:00").getTime();ThemeSettings.State auto=ThemeSettings.resolve(c,noon,zone);
        check("auto day uses coordinates",!auto.palette.night&&auto.solar!=null&&auto.description.contains("закат"));
        long sunset=auto.solar.sunset;check("before sunset remains day",!ThemeSettings.resolve(c,sunset-1,zone).palette.night);
        check("at sunset cache expires",ThemeSettings.resolve(c,sunset,zone).palette.night);
        ThemeSettings.mode(c,ThemeSettings.DAY);check("manual day overrides astronomical night",!ThemeSettings.resolve(c,sunset+1,zone).palette.night);
        ThemeSettings.mode(c,ThemeSettings.NIGHT);check("manual night overrides astronomical day",ThemeSettings.resolve(c,noon,zone).palette.night);
        ThemeSettings.location(c,0,0,false);check("zero coordinates saved",ThemeSettings.hasLocation(c)&&ThemeSettings.latitude(c)==0&&ThemeSettings.longitude(c)==0&&!ThemeSettings.gps(c));
        JSONObject export=new JSONObject(ProbeState.INSTANCE.exportJson());check("coordinates excluded from diagnostic export",!export.has("latitude")&&!export.has("longitude")&&!export.has("theme"));
        Activity main=startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK));
        click(main,"День");check("shared day in diagnostics",!ThemeSettings.resolve(c).palette.night);
        click(main,"Источник при переключении · 3 секунды");
        check("source checkbox saved",SourceOverlay.enabled(c));await("source window attached",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("FM2"));
        overlayOptions(true,true);await("both windows visible",()->state().overlayVisible&&state().sourceOverlayVisible);
        c.startActivity(new Intent().setComponent(new ComponentName("com.syu.ms","app.OverlayTargetActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        await("separate app foreground",()->{android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();return root!=null&&"com.syu.ms".contentEquals(root.getPackageName());});
        command(new Intent().putExtra("source",2).putExtra("volume",23));await("background CD and volume",()->state().sourceOverlayText.equals("CD")&&state().overlayText.startsWith("23"));
        SystemClock.sleep(500);screenshot("probe-v051-overlays-day-TEST.png");
        ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(700);screenshot("probe-v051-overlays-night-TEST.png");
        command(new Intent().putExtra("source",4));await("USB source",()->state().sourceOverlayText.equals("USB/iPod"));
        command(new Intent().putExtra("source",3));await("AUX uncertainty retained",()->state().sourceOverlayText.equals("AUX"));
        command(new Intent().putExtra("source",0).putExtra("band",5));await("unknown source hides indicator",()->!state().sourceOverlayVisible);
        check("theme and overlays send no audio writes",receipts(false).length()==0);
        command(new Intent().putExtra("kill",true));await("disconnection removes stale source",()->!state().sourceOverlayVisible);
        await("rebind refreshes source window",()->state().sourceOverlayText.equals("FM2"));
        stopProbe();await("both overlay windows removed on stop",()->!state().overlayVisible&&!state().sourceOverlayVisible&&!state().running);
        SourceOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);ThemeSettings.mode(c,ThemeSettings.AUTO);
        c.getSharedPreferences("theme",Context.MODE_PRIVATE).edit().remove("lat").remove("lon").putBoolean("gps",false).apply();ThemeSettings.mode(c,ThemeSettings.AUTO);
        check("no coordinates fallback is explicit",ThemeSettings.resolve(c).solar==null&&ThemeSettings.resolve(c).description.contains("тема Android"));
    }
    private void windowTests(Activity main)throws Exception{
        Context c=getTargetContext();receipts(true);SourceOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);AudioSourceController.enabled(c,false);
        android.accessibilityservice.AccessibilityServiceInfo serviceInfo=getUiAutomation().getServiceInfo();serviceInfo.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getUiAutomation().setServiceInfo(serviceInfo);
        click(main,"День");click(main,"Показывать климат при изменении показаний");check("climate enabled in main",ClimateOverlay.enabled(c));
        SystemClock.sleep(1200);check("no popup on initial subscription",!state().climateOverlayVisible);check("different temperatures highlight inferred DUAL",state().dual().equals("вкл")&&state().dualInferred());
        check("default optional zero not a measured state",!state().optionalKnown(12)&&!state().optionalKnown(16)&&!state().optionalKnown(17));
        c.startActivity(new Intent().setComponent(new ComponentName("com.syu.ms","app.OverlayTargetActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        await("another app remains foreground",()->getUiAutomation().getRootInActiveWindow()!=null&&"com.syu.ms".contentEquals(getUiAutomation().getRootInActiveWindow().getPackageName()));
        if(phase.endsWith("denied")){
            command(new Intent().putExtra("left",48));await("climate permission rejection",()->state().climateOverlayStatus.contains("Нет разрешения"));
            check("no denied windows",!state().climateOverlayVisible);await("CAN still updates",()->state().temperature(27).equals("24.0°"));check("no automatic writes on denied permission",receipts(false).length()==0);stopProbe();await("denied cleanup",()->!state().running);ClimateOverlay.enabled(c,false);return;
        }
        for(String name:new String[]{"climate_air_windshield","climate_fan","climate_rear","climate_v2_recirculation","climate_v2_face","climate_v2_face_feet","climate_v2_feet","climate_v2_feet_windshield","climate_v2_windshield"}){
            int id=c.getResources().getIdentifier(name,"drawable",c.getPackageName());android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inSampleSize=4;Bitmap b=android.graphics.BitmapFactory.decodeResource(c.getResources(),id,options);
            check("generated icon decodes "+name,b!=null);check("transparent icon margins "+name,(b.getPixel(0,0)>>>24)==0&&(b.getPixel(b.getWidth()-1,b.getHeight()-1)>>>24)==0);int count=0;for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++)if((b.getPixel(x,y)>>>24)>24)count++;check("icon contains visible glyph "+name,count>100);b.recycle();
        }
        command(new Intent().putExtra("volume",20));SystemClock.sleep(500);check("volume alone does not open climate",!state().climateOverlayVisible);
        command(new Intent().putExtra("left",48));await("changed temperature opens bottom window",()->state().climateOverlayVisible&&state().climateOverlayText.startsWith("24.0° / 23.0°"));check("bottom window under half screen",state().climateOverlayHeight>100&&state().climateOverlayHeight<=240);
        android.view.accessibility.AccessibilityNodeInfo under=getUiAutomation().getRootInActiveWindow();check("climate does not take focus",under!=null&&"com.syu.ms".contentEquals(under.getPackageName()));
        java.util.List<android.view.accessibility.AccessibilityNodeInfo> targets=under.findAccessibilityNodeInfosByText("Касание проходит");check("underlying target exists",!targets.isEmpty());android.graphics.Rect bounds=new android.graphics.Rect();targets.get(0).getBoundsInScreen(bounds);int before=Integer.parseInt(fixtureResult("getTouches"));long at=SystemClock.uptimeMillis();
        android.view.MotionEvent down=android.view.MotionEvent.obtain(at,at,0,bounds.centerX(),bounds.centerY(),0),up=android.view.MotionEvent.obtain(at,at+60,1,bounds.centerX(),bounds.centerY(),0);check("climate touch down",getUiAutomation().injectInputEvent(down,true));check("climate touch up",getUiAutomation().injectInputEvent(up,true));down.recycle();up.recycle();SystemClock.sleep(250);check("opaque climate does not click underlying app",Integer.parseInt(fixtureResult("getTouches"))==before);await("tap dismisses climate",()->!state().climateOverlayVisible);command(new Intent().putExtra("left",49));await("next climate change opens again",()->state().climateOverlayVisible);
        screenshot("probe-v06-climate-day-TEST.png");long revision=state().climateRevision;
        for(int i=0;i<5;i++){command(new Intent().putExtra("volume",21));SystemClock.sleep(1050);}check("duplicate callbacks do not count climate change",state().climateRevision==revision);check("duplicates do not extend popup",!state().climateOverlayVisible);
        command(new Intent().putExtra("dual",1).putExtra("recirc",1).putExtra("rear",1).putExtra("front",1).putExtra("flowAuto",1).putExtra("blowAuto",1));await("optional fields observed",()->state().dual().equals("вкл")&&state().optionalKnown(12)&&state().optionalKnown(16)&&state().optionalKnown(17)&&state().climateOverlayVisible);
        ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(400);screenshot("probe-v06-climate-night-TEST.png");command(new Intent().putExtra("dual",0));await("DUAL off after support evidence",()->state().dual().equals("выкл"));
        overlayOptions(true,true);await("volume coexists above climate",()->state().overlayVisible&&state().overlayBottomPx>state().climateOverlayHeight);await("climate expires",()->!state().climateOverlayVisible);await("volume returns to bottom",()->state().overlayBottomPx==24);overlayOptions(false,false);
        command(new Intent().putExtra("nullTemp",true));await("unknown temp is not zero",()->state().climateOverlayVisible&&state().climateOverlayText.startsWith("— /"));command(new Intent().putExtra("nullTemp",false).putExtra("left",-2).putExtra("right",-3));await("LOW and HI supported",()->state().climateOverlayText.startsWith("LOW / HI"));
        ClimateOverlay.enabled(c,false);await("disabled climate hides",()->!state().climateOverlayVisible);ClimateOverlay.enabled(c,true);SystemClock.sleep(1100);check("reenable does not replay old change",!state().climateOverlayVisible);
        command(new Intent().putExtra("kill",true));await("disconnect removes climate",()->!state().connected&&!state().climateOverlayVisible);await("reconnect restores clean baseline",()->state().connected&&state().source().equals("FM2")&&state().number(21)!=null);SystemClock.sleep(1100);check("reconnect does not open climate",!state().climateOverlayVisible);check("optional data cleared on reconnect",state().dualInferred()&&!state().optionalKnown(12));
        command(new Intent().putExtra("fan",6));await("fresh change after reconnect",()->state().climateOverlayVisible);stopProbe();await("all windows removed on stop",()->!state().running&&!state().climateOverlayVisible&&!state().overlayVisible);ClimateOverlay.enabled(c,false);AudioSourceController.enabled(c,true);ThemeSettings.mode(c,ThemeSettings.AUTO);
    }
    private android.view.accessibility.AccessibilityNodeInfo playerNode(String text){android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root==null)return null;java.util.List<android.view.accessibility.AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText(text);for(android.view.accessibility.AccessibilityNodeInfo node:nodes)if(node.isClickable())return node;return nodes.isEmpty()?null:nodes.get(0);}
    private void playerTap(String label)throws Exception{await("player control ready "+label,()->playerNode(label)!=null&&playerNode(label).isEnabled());SystemClock.sleep(350);android.graphics.Rect r=new android.graphics.Rect();playerNode(label).getBoundsInScreen(r);long now=SystemClock.uptimeMillis();android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,0,r.centerX(),r.centerY(),0),up=android.view.MotionEvent.obtain(now,now+60,1,r.centerX(),r.centerY(),0);check("player physical down",getUiAutomation().injectInputEvent(down,true));SystemClock.sleep(60);check("player physical up",getUiAutomation().injectInputEvent(up,true));down.recycle();up.recycle();}
    private void launcher(String name){getTargetContext().startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(new ComponentName(name.equals("USB")?"com.crewbeat.hondabox.usb":"com.crewbeat.hondabox.launcher."+(name.equals("Radio")?"radio":name.equals("IPod")?"ipod":name.toLowerCase(java.util.Locale.US)),"com.crewbeat.hondabox.launcher.LauncherActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    private void playerMode(String name)throws Exception{await("normal player "+name,()->state().audioPlayerVisible&&state().audioPlayerMode.equals(name));await("player accessible heading "+name,()->{android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();return root!=null&&!root.findAccessibilityNodeInfosByText(name.equals("RADIO")?"Radio":name.equals("IPOD")?"iPod":name).isEmpty();});}
    private void playerTests(Activity main)throws Exception{
        Context c=getTargetContext();receipts(true);ClimateOverlay.enabled(c,false);SourceOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);AudioSourceController.enabled(c,true);ThemeSettings.mode(c,ThemeSettings.DAY);
        android.accessibilityservice.AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getUiAutomation().setServiceInfo(info);
        java.util.Set<Integer> icons=new java.util.HashSet<>();for(String name:new String[]{"Radio","CD","USB","IPod"}){android.content.pm.ActivityInfo activity=c.getPackageManager().getActivityInfo(new ComponentName(c.getPackageName(),"com.crewbeat.hondabox."+name),0);check("launcher exported "+name,activity.exported&&activity.enabled);check("launcher normal Activity "+name,activity.targetActivity.endsWith("AudioPlayerActivity"));icons.add(activity.getIconResource());}check("four separate icons",icons.size()==4);
        SystemClock.sleep(500);c.startActivity(new Intent().setComponent(new ComponentName("com.syu.ms","app.OverlayTargetActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));SystemClock.sleep(500);
        command(new Intent().putExtra("source",2).putExtra("track",12).putExtra("disc",2).putExtra("minute",3).putExtra("second",8).putExtra("title","TEST · CD track").putExtra("artist","Test artist").putExtra("album","Test album"));
        if(phase.endsWith("denied")){SystemClock.sleep(700);check("denied background launch does not take foreground",!state().audioPlayerVisible);check("reading survives denied automatic launch",state().source().equals("CD"));launcher("CD");playerMode("CD");check("manual launcher needs no overlay",!android.provider.Settings.canDrawOverlays(c));check("manual open sends no writes",receipts(false).length()==0);command(new Intent().putExtra("source",3));await("AUX hides manual player without overlay permission",()->!state().audioPlayerVisible);stopProbe();await("denied player cleanup",()->!state().running);return;}
        playerMode("CD");check("automatic opening sends no command",receipts(false).length()==0);check("CD decimal time",playerNode("03:08")!=null);check("CD disc shown",playerNode("Диск 2")!=null);SystemClock.sleep(300);screenshot("probe-v06-CD-day-TEST.png");
        boolean activityWindow=false;for(android.view.accessibility.AccessibilityWindowInfo window:getUiAutomation().getWindows())if(window.getType()==android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION&&window.getRoot()!=null&&!window.getRoot().findAccessibilityNodeInfosByText("TEST · CD track").isEmpty())activityWindow=true;check("CD is application window",activityWindow);
        playerTap("Следующий");await("normal CD next starts",()->state().commandBusy);await("normal CD next completes",()->!state().commandBusy);JSONArray r=receipts(false);check("CD next only key pair",r.length()==2);step(r,0,7,0,4,1);step(r,1,7,0,4,0);
        receipts(true);playerTap("Предыдущий");await("normal CD previous starts",()->state().commandBusy);await("normal CD previous completes",()->!state().commandBusy);r=receipts(false);check("CD previous only key pair",r.length()==2);step(r,0,7,0,3,1);step(r,1,7,0,3,0);
        ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(400);screenshot("probe-v06-CD-night-TEST.png");
        command(new Intent().putExtra("source",1).putExtra("frequency",241).putExtra("preset1",181).putExtra("preset2",241).putExtra("preset3",281).putExtra("preset4",301).putExtra("preset5",361).putExtra("preset6",401));playerMode("RADIO");await("live radio frequency",()->playerNode("99.50")!=null);check("radio band actual",playerNode("FM2  MHz")!=null);check("six real preset fields",playerNode("6  107.50")!=null);ThemeSettings.mode(c,ThemeSettings.DAY);SystemClock.sleep(350);screenshot("probe-v06-Radio-day-TEST.png");ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(350);screenshot("probe-v06-Radio-night-TEST.png");
        command(new Intent().putExtra("source",4).putExtra("track",0x12).putExtra("minute",0x03).putExtra("second",0x28).putExtra("title","TEST · USB track"));playerMode("USB");check("USB BCD time",playerNode("03:28")!=null);check("USB BCD track",playerNode("Трек 12")!=null);ThemeSettings.mode(c,ThemeSettings.DAY);SystemClock.sleep(350);screenshot("probe-v06-USB-day-TEST.png");ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(350);screenshot("probe-v06-USB-night-TEST.png");
        command(new Intent().putExtra("track",0x1a));await("invalid BCD not displayed as valid track",()->playerNode("Трек —")!=null);
        receipts(true);launcher("IPod");playerMode("IPOD");check("iPod does not pretend known source",playerNode("экран выбран вручную")!=null);check("unsupported iPod selection disabled",playerNode("Выберите iPod штатной кнопкой")!=null&&!playerNode("Выберите iPod штатной кнопкой").isEnabled());ThemeSettings.mode(c,ThemeSettings.DAY);SystemClock.sleep(350);screenshot("probe-v06-iPod-day-TEST.png");ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(350);screenshot("probe-v06-iPod-night-TEST.png");check("iPod launcher sends no guessed command",receipts(false).length()==0);
        command(new Intent().putExtra("source",3));await("AUX removes normal player",()->!state().audioPlayerVisible);check("AUX sends no playback command",receipts(false).length()==0);
        command(new Intent().putExtra("source",0).putExtra("band",5));SystemClock.sleep(500);check("unknown source does not auto open radio or CD",!state().audioPlayerVisible);
        launcher("Radio");playerMode("RADIO");check("radio launch does not change actual source",state().source().equals("Не передан"));playerTap("Включить Radio");await("source selection starts",()->{try{return receipts(false).length()>0;}catch(Exception e){return false;}});await("source selection completes",()->!state().commandBusy);r=receipts(false);check("explicit radio button sends only source plan",r.length()==2);step(r,0,0,0,13);step(r,1,7,1,0);check("selection does not optimistically report success",state().source().equals("Не передан"));
        command(new Intent().putExtra("source",1).putExtra("band",4));await("AM1 actual band",()->playerNode("AM1")!=null);check("uncaptured Honda AM frequency stays unknown",playerNode("99.50")==null);
        launcher("CD");playerMode("CD");check("CD icon opens its own screen",playerNode("Включить CD")!=null);launcher("USB");playerMode("USB");check("USB icon opens its own screen",playerNode("Включить USB")!=null);
        command(new Intent().putExtra("source",3));await("AUX hides independently launched USB",()->!state().audioPlayerVisible);
        // Reopening must use source transitions, including a rapid AUX -> CD cycle.
        command(new Intent().putExtra("source",2).putExtra("track",7).putExtra("minute",1).putExtra("second",40));playerMode("CD");check("reopen after AUX is normal CD screen",playerNode("01:40")!=null);
        ClimateOverlay.enabled(c,true);SystemClock.sleep(1000);command(new Intent().putExtra("left",45).putExtra("right",48).putExtra("dual",1).putExtra("recirc",1).putExtra("rear",1).putExtra("front",0));await("opaque climate over normal CD app",()->state().climateOverlayVisible&&state().audioPlayerVisible);ThemeSettings.mode(c,ThemeSettings.DAY);SystemClock.sleep(400);screenshot("probe-v06-CD-climate-day-TEST.png");ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(400);screenshot("probe-v06-CD-climate-night-TEST.png");
        stopProbe();await("player test service stopped",()->!state().running&&!state().climateOverlayVisible);ClimateOverlay.enabled(c,false);AudioSourceController.enabled(c,true);ThemeSettings.mode(c,ThemeSettings.AUTO);
    }
    private void animationTests()throws Exception{
        Context c=getTargetContext();AudioSourceController.enabled(c,false);ClimateOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);SourceOverlay.enabled(c,false);ThemeSettings.mode(c,ThemeSettings.DAY);receipts(true);
        launcher("CD");playerMode("CD");command(new Intent().putExtra("source",2).putExtra("track",7).putExtra("disc",2).putExtra("minute",1).putExtra("second",40).putExtra("title","TEST · climate preview").putExtra("left",44).putExtra("right",46).putExtra("fan",4).putExtra("dual",1).putExtra("recirc",1).putExtra("rear",1).putExtra("front",1).putExtra("animation",0));
        await("preview baseline data",()->state().source().equals("CD")&&Integer.valueOf(7).equals(state().number(101)));ClimateOverlay.enabled(c,true);SystemClock.sleep(1200);
        try(OutputStream out=new FileOutputStream(new File(c.getExternalFilesDir(null),"climate-animation-ready.txt"))){out.write("READY".getBytes(StandardCharsets.UTF_8));}
        long recordDeadline=SystemClock.uptimeMillis()+90000;while(!Integer.valueOf(1).equals(state().number(145))&&SystemClock.uptimeMillis()<recordDeadline)SystemClock.sleep(100);check("recording started",Integer.valueOf(1).equals(state().number(145)));
        command(new Intent().putExtra("left",45).putExtra("right",48).putExtra("fan",5));await("animated climate appears",()->state().climateOverlayVisible);SystemClock.sleep(1800);screenshot("probe-v06-animation-day-TEST.png");
        ThemeSettings.mode(c,ThemeSettings.NIGHT);command(new Intent().putExtra("fan",7));SystemClock.sleep(1800);screenshot("probe-v06-animation-night-TEST.png");
        long now=SystemClock.uptimeMillis();android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,0,1200,380,0),up=android.view.MotionEvent.obtain(now,now+60,1,1200,380,0);getUiAutomation().injectInputEvent(down,true);SystemClock.sleep(60);getUiAutomation().injectInputEvent(up,true);down.recycle();up.recycle();await("animated dismissal",()->!state().climateOverlayVisible);SystemClock.sleep(650);
        command(new Intent().putExtra("left",47).putExtra("fan",3).putExtra("dual",0));await("animated reopening",()->state().climateOverlayVisible);SystemClock.sleep(5300);check("timed animated exit",!state().climateOverlayVisible);check("animation sends no CAN command",receipts(false).length()==0);
        stopProbe();await("animation cleanup",()->!state().running);ClimateOverlay.enabled(c,false);AudioSourceController.enabled(c,true);ThemeSettings.mode(c,ThemeSettings.AUTO);
    }
    private void step(JSONArray a,int index,int module,int cmd,int...args)throws Exception {
        JSONObject s=a.getJSONObject(index);check("fixture wire valid",s.getBoolean("valid"));check("correct module/cmd",s.getInt("module")==module&&s.getInt("cmd")==cmd);
        JSONArray ints=s.getJSONArray("args");boolean match=ints.length()==args.length;for(int i=0;match&&i<args.length;i++)match=ints.getInt(i)==args[i];check("correct wire args",match);
    }
    private void audioTests()throws Exception {
        receipts(true);
        for(AudioCommand a:new AudioCommand[]{AudioCommand.RADIO,AudioCommand.CD,AudioCommand.USB}){
            receipts(true);audio(a.name());await("command started",()->state().commandBusy);await("command finished",()->!state().commandBusy);
            JSONArray r=receipts(false);check("only route and source commands",r.length()==2);step(r,0,0,0,13);step(r,1,7,1,a.mode);
            check("IPC never claims hardware execution",state().commandStatus.contains("Выполнение магнитолой не подтверждено"));
        }
        for(AudioCommand a:new AudioCommand[]{AudioCommand.PREVIOUS,AudioCommand.NEXT}){
            receipts(true);audio(a.name());await("key command started",()->state().commandBusy);await("key command finished",()->!state().commandBusy);
            JSONArray r=receipts(false);check("press and release only",r.length()==2);step(r,0,7,0,a.key,1);step(r,1,7,0,a.key,0);
            long delay=r.getJSONObject(1).getLong("atMs")-r.getJSONObject(0).getLong("atMs");check("bounded press duration",delay>=120&&delay<1500);
        }
        receipts(true);audio("BAD_COMMAND");await("invalid action rejected",()->state().commandStatus.contains("Неизвестная"));check("invalid action sends nothing",receipts(false).length()==0);
        command(new Intent().putExtra("profile",999));await("unknown command profile",()->state().number(1000)!=null&&state().number(1000)==999);
        audio("NEXT");await("profile guard",()->state().commandStatus.contains("только для проверенной"));check("unknown profile sends nothing",receipts(false).length()==0);
        command(new Intent().putExtra("profile",6291908));await("profile restored",()->state().honda());
        receipts(true);audio("NEXT");audio("NEXT");await("double action started",()->state().commandBusy);await("double action finished",()->!state().commandBusy);check("rapid duplicate ignored",receipts(false).length()==2);
        receipts(true);audio("PREVIOUS");long end=SystemClock.uptimeMillis()+2000;
        while(SystemClock.uptimeMillis()<end&&(state().number(146)==null||state().number(146)!=3))SystemClock.sleep(5);
        check("observed press before stop",state().number(146)!=null&&state().number(146)==3);stopProbe();await("stop releases pending key",()->!state().commandBusy&&!state().running);
        JSONArray stopped=receipts(false);check("stop emitted release",stopped.length()==2);step(stopped,1,7,0,3,0);
        check("cancellation recorded",state().commandStatus.contains("прервана"));
        receipts(true);startProbe();await("reconnected for command audit",()->state().connected&&state().count>0);SystemClock.sleep(500);
        check("no command replay on new session",receipts(false).length()==0);
        JSONObject export=new JSONObject(ProbeState.INSTANCE.exportJson());JSONArray c=export.getJSONArray("commands");
        check("structured command history",c.length()>=7&&c.getJSONObject(0).has("before")&&c.getJSONObject(0).has("after"));
    }
    private String fixtureResult(String key)throws Exception {
        CountDownLatch latch=new CountDownLatch(1);AtomicReference<String> value=new AtomicReference<>();
        getTargetContext().sendOrderedBroadcast(new Intent().setComponent(new ComponentName("com.syu.ms","app.ControlReceiver")).putExtra(key,true),null,
            new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){value.set(getResultData());latch.countDown();}},null,Activity.RESULT_OK,null,null);
        if(!latch.await(5,TimeUnit.SECONDS))throw new AssertionError("fixture timeout");return value.get();
    }
    private void overlayOptions(boolean enabled,boolean pinned)throws Exception {
        runOnMainSync(()->VolumeOverlay.options(getTargetContext(),enabled,pinned));
        getTargetContext().startService(new Intent(getTargetContext(),ProbeService.class).setAction(ProbeService.OVERLAY));
    }
    private void overlayTests(Activity activity)throws Exception {
        receipts(true);
        runOnMainSync(()->{
            Button enabled=button(activity.findViewById(android.R.id.content),"Показывать при изменении громкости");
            Button pinned=button(activity.findViewById(android.R.id.content),"Постоянный индикатор · проверка работы в фоне");
            check("overlay controls exist",enabled instanceof CheckBox&&pinned instanceof CheckBox);
            ((CheckBox)enabled).setChecked(true);((CheckBox)pinned).setChecked(true);
        });
        check("UI stored enabled preference",VolumeOverlay.enabled(getTargetContext()));check("UI stored pinned preference",VolumeOverlay.pinned(getTargetContext()));
        if("overlay-denied".equals(phase)){
            check("permission denied",!android.provider.Settings.canDrawOverlays(getTargetContext()));
            await("denial diagnosed",()->state().overlayStatus.contains("Нет разрешения"));check("no denied window",!state().overlayVisible);
            click(activity,"Источник при переключении · 3 секунды");await("source denial diagnosed",()->state().sourceOverlayStatus.contains("Нет разрешения"));check("source denied window absent",!state().sourceOverlayVisible);SourceOverlay.enabled(getTargetContext(),false);
            command(new Intent().putExtra("volume",20));await("CAN survives denied overlay",()->state().volume().equals("20"));
            check("no volume write on denial",receipts(false).length()==0);overlayOptions(false,false);return;
        }
        check("permission granted",android.provider.Settings.canDrawOverlays(getTargetContext()));
        await("overlay attached",()->state().overlayVisible&&state().overlayText.startsWith("18"));
        getTargetContext().startActivity(new Intent().setComponent(new ComponentName("com.syu.ms","app.OverlayTargetActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        SystemClock.sleep(700);command(new Intent().putExtra("volume",23));await("volume updates over another app",()->state().overlayVisible&&state().overlayText.startsWith("23"));
        // Accessibility sees the separate app below the non-touchable overlay.
        await("another app actually foreground",()->{android.view.accessibility.AccessibilityNodeInfo current=getUiAutomation().getRootInActiveWindow();return current!=null&&"com.syu.ms".contentEquals(current.getPackageName());});
        android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
        java.util.List<android.view.accessibility.AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText("Касание проходит");
        check("underlying touch target found",!nodes.isEmpty());android.graphics.Rect bounds=new android.graphics.Rect();nodes.get(0).getBoundsInScreen(bounds);
        int before=Integer.parseInt(fixtureResult("getTouches"));long at=SystemClock.uptimeMillis();
        android.view.MotionEvent down=android.view.MotionEvent.obtain(at,at,android.view.MotionEvent.ACTION_DOWN,bounds.centerX(),bounds.centerY(),0);
        android.view.MotionEvent up=android.view.MotionEvent.obtain(at,at+60,android.view.MotionEvent.ACTION_UP,bounds.centerX(),bounds.centerY(),0);
        check("touch down injected",getUiAutomation().injectInputEvent(down,true));check("touch up injected",getUiAutomation().injectInputEvent(up,true));down.recycle();up.recycle();
        SystemClock.sleep(300);check("touch passed through overlay",Integer.parseInt(fixtureResult("getTouches"))==before+1);
        Bitmap shot=getUiAutomation().takeScreenshot();check("overlay screenshot",shot!=null);
        try(OutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"probe-v04-overlay-TEST.png"))){shot.compress(Bitmap.CompressFormat.PNG,100,out);}shot.recycle();
        long rawBefore=state().rawCount;
        command(new Intent().putExtra("raw",new int[]{0x2e,3,3,-1,-128,0,0x79}));await("raw arrives in background",()->state().rawCount>rawBefore);
        JSONObject exported=new JSONObject(ProbeState.INSTANCE.exportJson());JSONArray chunks=exported.getJSONObject("rawBsj").getJSONArray("chunks");
        check("signed raw bytes normalized",chunks.getJSONObject(chunks.length()-1).getString("hex").equals("2E 03 03 FF 80 00 79"));
        check("raw timestamp and session",chunks.getJSONObject(0).has("atMs")&&chunks.getJSONObject(0).has("session"));
        check("structured overlay diagnostics",exported.getJSONObject("overlay").getBoolean("visible"));
        overlayOptions(true,false);SystemClock.sleep(3300);await("transient indicator hides",()->!state().overlayVisible);
        command(new Intent().putExtra("volume",24));await("changed volume reopens",()->state().overlayVisible&&state().overlayText.startsWith("24"));
        SystemClock.sleep(3300);await("second popup times out",()->!state().overlayVisible);
        command(new Intent().putExtra("volume",24));SystemClock.sleep(600);check("duplicate value does not reopen",!state().overlayVisible);
        getTargetContext().startService(new Intent(getTargetContext(),ProbeService.class).setAction(ProbeService.OVERLAY).putExtra("demo",true));
        await("demo clearly labelled",()->state().overlayVisible&&state().overlayText.contains("ТЕСТ")&&state().overlayText.startsWith("17"));
        check("demo preserves OEM value",state().volume().equals("24"));check("overlay never sends hardware commands",receipts(false).length()==0);
        overlayOptions(false,false);await("disable removes overlay",()->!state().overlayVisible);
        overlayOptions(true,true);await("pinned restored",()->state().overlayVisible);
        command(new Intent().putExtra("kill",true));await("disconnect clears stale volume",()->!state().connected&&state().overlayText.startsWith("—"));
        await("overlay resumes after rebind",()->state().connected&&state().overlayVisible&&state().overlayText.startsWith("18"));
        stopProbe();await("stop removes actual overlay",()->!state().running&&!state().overlayVisible);
        overlayOptions(false,false); // No foreground restart is requested by these options.
    }
    private void rawLimits()throws Exception {
        ProbeState p=ProbeState.INSTANCE;p.newSession();p.status("Test","",true);
        p.event(1019,new int[]{0},null,null);p.event(1019,null,null,null);check("initial sentinel not packet",state().rawCount==0);
        p.event(1019,new int[]{-1,0x12},null,null);p.event(1019,new int[]{-1,0x12},null,null);
        JSONObject raw=new JSONObject(p.exportJson()).getJSONObject("rawBsj");check("duplicate packets preserved",raw.getJSONArray("chunks").length()==2);
        check("unsigned hex",raw.getJSONArray("chunks").getJSONObject(0).getString("hex").equals("FF 12"));
        for(int i=0;i<4096;i++)p.event(1019,new int[]{0x2e,1},null,null);
        raw=new JSONObject(p.exportJson()).getJSONObject("rawBsj");check("count limit",raw.getJSONArray("chunks").length()==4096);check("oldest dropped recorded",raw.getLong("droppedChunks")==2);
        int[] big=new int[8193];java.util.Arrays.fill(big,0xff);p.event(1019,big,null,null);
        raw=new JSONObject(p.exportJson()).getJSONObject("rawBsj");JSONObject last=raw.getJSONArray("chunks").getJSONObject(4095);
        check("oversized chunk explicitly truncated",last.getBoolean("truncated")&&last.getInt("capturedBytes")==8192&&last.getInt("originalInts")==8193);
        for(int i=0;i<128;i++)p.event(1019,big,null,null);
        raw=new JSONObject(p.exportJson()).getJSONObject("rawBsj");check("byte limit",raw.getInt("retainedBytes")<=1048576);check("byte eviction reported",raw.getLong("droppedChunks")>4000);
        p.newSession();p.event(1019,new int[]{0x2e,2},null,null);raw=new JSONObject(p.exportJson()).getJSONObject("rawBsj");
        JSONArray chunks=raw.getJSONArray("chunks");check("reconnect keeps prior raw",chunks.length()>1);check("session identifier advances",chunks.getJSONObject(chunks.length()-1).getLong("session")>chunks.getJSONObject(0).getLong("session"));
    }
    private void replay() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream in = getContext().getAssets().open("recorded-session.json")) { byte[] b = new byte[4096]; int n; while ((n = in.read(b)) != -1) bytes.write(b,0,n); }
        JSONObject report = new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
        ProbeState.INSTANCE.newSession(); ProbeState.INSTANCE.status("Replay", "Recorded head-unit callbacks",true);
        JSONArray log = report.getJSONArray("log");
        boolean climateSeen = false;
        for (int i=0;i<log.length();i++) {
            String l = log.getString(i); int p = l.indexOf("event "); if (p < 0) continue;
            int split=l.indexOf(' ',p+6), code=Integer.parseInt(l.substring(p+6,split)); JSONObject v=new JSONObject(l.substring(split+1));
            JSONArray a=v.optJSONArray("ints"), f=v.optJSONArray("floats"), s=v.optJSONArray("strings");
            int[] ints=a==null?null:new int[a.length()]; float[] floats=f==null?null:new float[f.length()]; String[] strings=s==null?null:new String[s.length()];
            if(a!=null)for(int k=0;k<ints.length;k++)ints[k]=a.getInt(k);
            if(f!=null)for(int k=0;k<floats.length;k++)floats[k]=(float)f.getDouble(k);
            if(s!=null)for(int k=0;k<strings.length;k++)strings[k]=s.isNull(k)?null:s.getString(k);
            ProbeState.INSTANCE.event(code,ints,floats,strings);
            if ("22.0°".equals(state().temperature(27)) && state().number(21) != null && state().number(21) > 0) climateSeen=true;
        }
        check("all recorded callbacks replayed",state().count==report.getLong("callbacksThisSession"));
        check("real source98 zero stays unknown",state().source().equals("Не передан"));
        check("real band5 Honda AM2",state().radioBand().equals("AM2"));
        check("received nonzero temperatures",climateSeen);
        check("climate off zero does not display 0 degrees",state().temperature(27).equals("—")&&state().temperature(28).equals("—"));
        check("recorded CD data rendered",state().media().contains("Трек [101]: 9")&&state().media().contains("02:21")&&state().media().contains("Диск [113]: 1"));
        check("menu control characters cleaned",state().menu().equals("—  /  BASS      +1  /  —"));
        check("unknown source shows both time encodings",state().media().contains("DEC 02:21 / BCD 02:15"));
        ProbeState.INSTANCE.mark("CD"); ProbeState.INSTANCE.event(98,new int[]{2},null,null);
        check("CD decimal time",state().media().contains("02:21 (CD)"));
        ProbeState.INSTANCE.event(98,new int[]{4},null,null);ProbeState.INSTANCE.event(101,new int[]{0x21},null,null);
        check("USB BCD time and track",state().media().contains("02:15 (BCD USB)")&&state().media().contains("Трек [101]: 21 (BCD USB"));
        ProbeState.INSTANCE.event(100,new int[]{0x1a},null,null);check("invalid BCD does not invent a time",state().media().contains("некорректный BCD"));
        JSONObject mark = new JSONObject(ProbeState.INSTANCE.exportJson()).getJSONArray("markers").getJSONObject(0);
        check("manual label preserves measured source",mark.getString("label").equals("CD")&&mark.getJSONObject("values").getJSONObject("98").getJSONArray("ints").getInt(0)==0);
        check("manual label identified as observation",mark.getString("origin").contains("manual"));
        ProbeState.INSTANCE.event(115,new int[]{4},null,null); check("Honda AM1",state().radioBand().equals("AM1"));
        ProbeState.INSTANCE.event(1000,new int[]{3604932},null,null);check("other profile keeps stock MW mapping",state().radioBand().equals("MW"));
        ProbeState.INSTANCE.event(103,null,null,new String[]{null});check("null string safe",state().text(103).equals("—"));
        ProbeState.INSTANCE.status("Stopped","",false);check("all expanded UI hides stale data",state().climate().equals("—")&&state().media().equals("—")&&state().radioBand().equals("—"));
    }
    private int[] packet(int command,int... payload){int[] frame=new int[payload.length+4];frame[0]=46;frame[1]=command;frame[2]=payload.length;int sum=command+payload.length;for(int i=0;i<payload.length;i++){frame[i+3]=payload[i];sum+=payload[i];}frame[frame.length-1]=(~sum)&255;return frame;}
    private void raw(int... bytes){command(new Intent().putExtra("raw",bytes).putExtra("source",0));}
    private void hardwareTests()throws Exception{
        Context c=getTargetContext();receipts(true);SourceOverlay.enabled(c,true);ClimateOverlay.enabled(c,true);VolumeOverlay.options(c,false,false);AudioSourceController.enabled(c,true);ThemeSettings.mode(c,ThemeSettings.DAY);
        // Replay the exact captured chunks, including fragmented long packets and duplicates.
        JSONObject capture;try(InputStream in=getContext().getAssets().open("recorded-bsj-v06.json")){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;)bytes.write(b,0,n);capture=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));}
        ProbeState replay=new ProbeState();replay.newSession();replay.event(1000,new int[]{6291908},null,null);replay.event(98,new int[]{0},null,null);replay.status("Replay","",true);
        JSONArray chunks=capture.getJSONArray("chunks");final java.util.List<int[]> parsed=new java.util.ArrayList<>();BsjFrames frames=new BsjFrames((cmd,payload)->{if(cmd==3)parsed.add(payload);});
        for(int i=0;i<chunks.length();i++){JSONObject chunk=chunks.getJSONObject(i);String[] parts=chunk.getString("hex").split(" ");int[] ints=new int[parts.length];for(int k=0;k<parts.length;k++)ints[k]=Integer.parseInt(parts[k],16);frames.accept(ints,chunk.getLong("atMs"));replay.event(1019,ints,null,null);}
        check("real raw source frames recovered",parsed.size()>250);check("real last CD/iPod wire source2",replay.snapshot().audioSource()==2);check("stock source98 still zero",replay.snapshot().number(98)==0);check("decoding does not create command writes",new JSONObject(replay.exportJson()).getJSONArray("commands").length()==0);
        // Source0 stock fixture, real validated BSJ frames delivered through the IPC callback.
        command(new Intent().putExtra("source",0));await("stock source not set",()->state().number(98)==0);SystemClock.sleep(1200);
        c.startActivity(new Intent().setComponent(new ComponentName("com.syu.ms","app.OverlayTargetActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        raw(46,3,2);SystemClock.sleep(350);check("fragment does not invent source",state().audioSource()==0);
        raw(0x80,8,0x72);await("fragmented BSJ radio source",()->state().audioSource()==1&&state().sourceOverlayText.equals("FM2"));
        long sequence=state().sourceSequence;raw(packet(3,0x80,8));SystemClock.sleep(350);check("duplicate source does not transition",state().sourceSequence==sequence);
        int[] corrupt=packet(3,0x81,8);corrupt[5]^=1;raw(corrupt);SystemClock.sleep(350);check("bad checksum cannot switch source",state().audioSource()==1);
        raw(packet(3,0x81,8));playerMode("CD");check("raw CD autoopen uses no commands",receipts(false).length()==0);check("CD uses BSJ while stock98 remains0",state().audioSource()==2&&state().number(98)==0);
        raw(packet(5,1,0,0,1,0,0x21,3,0x59));await("real CD BCD track and time",()->state().cdNumber(101).equals("21")&&state().cdTime().equals("03:59"));SystemClock.sleep(350);screenshot("probe-v061-raw-CD-TEST.png");
        raw(packet(5,1,0,0,1,0,0x21,3,0x1a));await("invalid raw BCD not displayed",()->state().cdTime().equals("—"));
        raw(packet(3,0x83,8));playerMode("USB");raw(packet(5,1,0,0,1,1,0x23,0x12,0x34));await("four digit BCD track",()->state().cdNumber(101).equals("123")&&state().cdTime().equals("12:34"));
        raw(packet(3,0x82,8));await("raw AUX closes normal player",()->!state().audioPlayerVisible);raw(packet(3,0x80,8));playerMode("RADIO");
        raw(packet(4,2,1,0x45,4,1));await("raw station current frequency",()->Integer.valueOf(325).equals(state().displayNumber(119))&&state().radioBand().equals("FM2"));
        check("recirculation remains unknown when unsupported",!state().optionalKnown(12)&&!state().optionalKnown(16)&&!state().optionalKnown(17));
        command(new Intent().putExtra("raw",new int[]{0}).putExtra("source",0).putExtra("power",1).putExtra("fan",0).putExtra("left",0).putExtra("right",0));await("fan off overrides stuck power bit",()->Integer.valueOf(0).equals(state().climatePower()));check("unknown temperatures do not infer dual",state().dual().equals("—"));
        command(new Intent().putExtra("left",44).putExtra("right",44).putExtra("fan",1));await("equal temperatures clear inferred dual",()->state().dualInferred()&&state().dual().equals("выкл"));
        command(new Intent().putExtra("right",46));await("different temperatures highlight dual",()->state().dual().equals("вкл"));
        for(int fan=0;fan<=7;fan++){final int n=fan;command(new Intent().putExtra("fan",n));await("fan scale "+n,()->state().number(21)==n&&state().fanSegments()==n);}
        command(new Intent().putExtra("fan",5));await("climate visible with new icons",()->state().climateOverlayVisible);SystemClock.sleep(400);screenshot("probe-v061-climate-day-TEST.png");ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(400);screenshot("probe-v061-climate-night-TEST.png");
        check("all automatic updates remain read only",receipts(false).length()==0);JSONObject report=new JSONObject(ProbeState.INSTANCE.exportJson());check("export retains raw and derived separately",report.getJSONObject("lastValues").getJSONObject("98").getJSONArray("ints").getInt(0)==0&&report.getJSONObject("bsjDecoded").getInt("source")==1);
        command(new Intent().putExtra("profile",999).putExtra("raw",packet(3,0x81,8)));await("other profile cannot use BSJ source",()->state().number(1000)==999&&state().bsjSource==null);command(new Intent().putExtra("profile",6291908).putExtra("raw",new int[]{0}));await("profile reset removes old derived values",()->state().bsjSource==null&&state().bsjValues.isEmpty());
        stopProbe();await("hardware test cleanup",()->!state().running);SourceOverlay.enabled(c,false);ClimateOverlay.enabled(c,false);ThemeSettings.mode(c,ThemeSettings.AUTO);
    }

    private android.view.accessibility.AccessibilityNodeInfo menuNode(String text){for(android.view.accessibility.AccessibilityWindowInfo w:getUiAutomation().getWindows()){android.view.accessibility.AccessibilityNodeInfo root=w.getRoot();if(root==null)continue;for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text))if(node.getText()!=null&&node.getText().toString().contains(text))return node;}return null;}
    private void menuListTests()throws Exception {
        android.accessibilityservice.AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getUiAutomation().setServiceInfo(info);Context c=getTargetContext();I18n.language(c,"en");ThemeSettings.mode(c,ThemeSettings.DAY);SourceSettings.aux(c,-1);CarMenuOverlay.enabled(c,true);SourceOverlay.enabled(c,false);AudioSourceController.enabled(c,false);ClimateOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);stopProbe();receipts(true);command(new Intent().putExtra("mediaState",0).putExtra("source",4).putExtra("raw",new int[]{0}));startProbe();await("menu list connection",()->state().connected);SystemClock.sleep(1000);raw(packet(3,0xC3,7));raw(menuPacket("","02: ARTISTS >",""));await("list overlay visible",()->state().carMenuOverlayVisible);await("full iPod category list drawn",()->menuNode("PLAYLISTS")!=null&&menuNode("ALBUMS")!=null&&menuNode("SONGS")!=null);check("received numbered Artists item highlighted",menuNode("02: ARTISTS >")!=null&&menuNode("02: ARTISTS >").isSelected());screenshot("honda-box-v083-ipod-list-TEST.png");
        raw(menuPacket("","SHUFFLE ALL",""));await("iPod play mode list",()->menuNode("SHUFFLE ALL")!=null&&menuNode("SHUFFLE ALL").isSelected()&&menuNode("SHUFFLE OFF")!=null);check("menu classification uses only actual packet",state().mediaDevice==ProbeState.MediaDevice.IPOD);screenshot("honda-box-v083-play-mode-list-TEST.png");
        raw(menuPacket("AUDIO SETTINGS","BASS      +1",""));await("audio settings list",()->menuNode("TREBLE")!=null&&menuNode("FADER")!=null&&menuNode("BALANCE")!=null);check("received bass value preserved",menuNode("BASS      +1")!=null&&menuNode("BASS      +1").isSelected());screenshot("honda-box-v083-audio-settings-list-TEST.png");
        raw(menuPacket("","01: MUSIC >",""));await("dynamic row received",()->state().menuRow(1).equals("01: MUSIC >"));MenuListModel model=MenuListModel.from(state());check("unknown dynamic list not fabricated",model.items.size()==1&&model.items.get(0).equals("01: MUSIC >"));check("menu list renders without CAN commands",receipts(false).length()==0);stopProbe();await("menu list cleanup",()->!state().running);CarMenuOverlay.enabled(c,false);ThemeSettings.mode(c,ThemeSettings.AUTO);I18n.language(c,"ru");
    }

    private int[] menuPacket(String a,String b,String c){int[] payload=new int[49];payload[0]=0x20;String[] rows={a,b,c};for(int i=0;i<3;i++){byte[] bytes=rows[i].getBytes(StandardCharsets.UTF_8);for(int k=0;k<Math.min(16,bytes.length);k++)payload[1+i*16+k]=bytes[k]&255;}return packet(9,payload);}
    private android.view.accessibility.AccessibilityWindowInfo menuWindow(){for(android.view.accessibility.AccessibilityWindowInfo w:getUiAutomation().getWindows())if(w.getRoot()!=null&&!w.getRoot().findAccessibilityNodeInfosByText("CAR MENU").isEmpty())return w;return null;}
    private void tapPoint(int x,int y){long now=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(now,now,0,x,y,0),up=MotionEvent.obtain(now,now+60,1,x,y,0);check("menu touch down",getUiAutomation().injectInputEvent(down,true));SystemClock.sleep(60);check("menu touch up",getUiAutomation().injectInputEvent(up,true));down.recycle();up.recycle();}
    private void closeMenu()throws Exception{await("menu close accessible",()->menuWindow()!=null&&!menuWindow().getRoot().findAccessibilityNodeInfosByText("×").isEmpty());android.graphics.Rect bounds=new android.graphics.Rect();menuWindow().getRoot().findAccessibilityNodeInfosByText("×").get(0).getBoundsInScreen(bounds);tapPoint(bounds.centerX(),bounds.centerY());await("close hides overlay",()->!state().carMenuOverlayVisible);}
    private void menuTests(Activity main)throws Exception{
        Context c=getTargetContext();receipts(true);AudioSourceController.enabled(c,false);ClimateOverlay.enabled(c,false);SourceOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);ThemeSettings.mode(c,ThemeSettings.DAY);
        click(main,"Показывать штатное меню автомобиля");check("main menu option persists",CarMenuOverlay.enabled(c));check("menu demo control exists",button(main.findViewById(android.R.id.content),"Тест меню · 15 секунд")!=null);
        android.accessibilityservice.AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getUiAutomation().setServiceInfo(info);
        SystemClock.sleep(1100);check("initial menu is not replayed",!state().carMenuOverlayVisible);
        c.startActivity(new Intent().setComponent(new ComponentName("com.syu.ms","app.OverlayTargetActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));SystemClock.sleep(350);
        command(new Intent().putExtra("menu1","TEST · AUDIO").putExtra("menu2","  BASS      +1\u0000\u0000").putExtra("menu3","TREBLE      C").putExtra("menuShow",1));await("menu text normalized",()->state().menuRow(1).equals("BASS      +1"));
        if(phase.endsWith("denied")){
            await("menu permission refusal",()->state().carMenuOverlayStatus.contains("Нет разрешения"));check("denied menu absent",!state().carMenuOverlayVisible&&menuWindow()==null);check("data reading survives denied menu",state().connected&&state().hasMenuRows());
            raw(menuPacket("TEST · RAW","BALANCE      C",""));await("denied raw menu still read",()->state().menuRow(1).equals("BALANCE      C"));
            c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY).putExtra("carMenuDemo",true));SystemClock.sleep(350);check("denied demo absent",!state().carMenuOverlayVisible);check("denied menu writes nothing",receipts(false).length()==0);stopProbe();await("denied menu stop cleans up",()->!state().running&&!state().carMenuOverlayVisible);CarMenuOverlay.enabled(c,false);ThemeSettings.mode(c,ThemeSettings.AUTO);return;
        }
        await("native menu overlays another app",()->state().carMenuOverlayVisible&&menuWindow()!=null);check("menu is system overlay",menuWindow().getType()==android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM);
        check("all three native rows preserved",state().menuRow(0).equals("TEST · AUDIO")&&state().menuRow(2).equals("TREBLE      C"));SystemClock.sleep(450);screenshot("probe-v062-menu-day-TEST.png");
        android.graphics.Rect box=new android.graphics.Rect();menuWindow().getBoundsInScreen(box);Bitmap screen=getUiAutomation().takeScreenshot();check("menu opaque day surface",screen.getPixel(box.left+25,box.top+25)==ThemeSettings.LIGHT.card);screen.recycle();
        ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(450);screenshot("probe-v062-menu-night-TEST.png");screen=getUiAutomation().takeScreenshot();check("menu night surface updates",screen.getPixel(box.left+25,box.top+25)==ThemeSettings.DARK.card);screen.recycle();
        check("no theme controls in menu",menuWindow().getRoot().findAccessibilityNodeInfosByText("Место").isEmpty());check("menu display writes nothing",receipts(false).length()==0);
        long revision=state().menuRevision;SystemClock.sleep(3300);command(new Intent().putExtra("menu2","  BASS      +1\u0000\u0000"));SystemClock.sleep(1400);check("duplicates do not renew menu",!state().carMenuOverlayVisible&&state().menuRevision==revision);
        command(new Intent().putExtra("menu2","TREBLE       +2"));await("new native line reopens menu",()->state().carMenuOverlayVisible);command(new Intent().putExtra("menuShow",0));await("native close signal removes menu",()->!state().carMenuOverlayVisible);
        command(new Intent().putExtra("menuShow",1));await("native reopen signal shows current lines",()->state().carMenuOverlayVisible);closeMenu();check("closing sends no vehicle command",receipts(false).length()==0);
        command(new Intent().putExtra("menu2","TREBLE       +2"));SystemClock.sleep(350);check("dismissed menu stays closed on duplicates",!state().carMenuOverlayVisible);
        // A complete validated09 packet updates all three lines atomically and bypasses native fields.
        int[] rawMenu=menuPacket("TEST · RAW","FADER         C","");raw(java.util.Arrays.copyOfRange(rawMenu,0,18));SystemClock.sleep(350);check("partial raw menu cannot show",!state().carMenuOverlayVisible);
        raw(java.util.Arrays.copyOfRange(rawMenu,18,rawMenu.length));await("fragmented raw menu opens",()->state().carMenuOverlayVisible&&state().menuRow(1).equals("FADER         C"));
        check("raw text leaves native callback intact",state().text(136).equals("TREBLE       +2"));check("raw menu metadata retained",state().displayNumber(138)==0x20);
        revision=state().menuRevision;raw(rawMenu);SystemClock.sleep(350);check("duplicate raw is not menu change",state().menuRevision==revision);
        int[] corrupt=menuPacket("BAD","WRONG","PAYLOAD");corrupt[corrupt.length-1]^=1;raw(corrupt);SystemClock.sleep(350);check("bad checksum cannot change menu",state().menuRow(1).equals("FADER         C"));
        raw(packet(9,0x20,65));SystemClock.sleep(350);check("wrong menu length rejected",state().menuRow(1).equals("FADER         C"));
        raw(packet(3,0xc0,18));SystemClock.sleep(300);raw(packet(3,0x80,18));await("raw close signal removes menu",()->!state().carMenuOverlayVisible&&state().displayNumber(139)==0);
        raw(packet(3,0xc0,18));await("raw reopen signal shows menu",()->state().carMenuOverlayVisible);raw(menuPacket("","",""));await("all blank lines hide",()->!state().carMenuOverlayVisible&&!state().hasMenuRows());
        // The exact1379 captured chunks include64 full OEM menu packets.
        JSONObject capture;try(InputStream in=getContext().getAssets().open("recorded-bsj-v06.json")){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;)bytes.write(b,0,n);capture=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));}
        ProbeState replay=new ProbeState();replay.newSession();replay.event(1000,new int[]{6291908},null,null);replay.status("Replay","",true);final int[] found={0};BsjFrames parser=new BsjFrames((cmd,payload)->{if(cmd==9&&payload.length==49)found[0]++;});JSONArray chunks=capture.getJSONArray("chunks");
        for(int i=0;i<chunks.length();i++){JSONObject chunk=chunks.getJSONObject(i);String[] parts=chunk.getString("hex").split(" ");int[] ints=new int[parts.length];for(int k=0;k<parts.length;k++)ints[k]=Integer.parseInt(parts[k],16);parser.accept(ints,chunk.getLong("atMs"));replay.event(1019,ints,null,null);}
        check("recorded menu packet count",found[0]==64);check("recorded BASS line decoded",replay.snapshot().menuRow(1).equals("BASS      +1"));check("raw replay writes no commands",new JSONObject(replay.exportJson()).getJSONArray("commands").length()==0);
        CarMenuOverlay.enabled(c,false);await("menu option disables window",()->!state().carMenuOverlayVisible);CarMenuOverlay.enabled(c,true);SystemClock.sleep(1100);check("reenable does not replay old menu",!state().carMenuOverlayVisible);
        raw(menuPacket("TEST · COEXIST","BALANCE       C",""));await("fresh menu after reenable",()->state().carMenuOverlayVisible);
        ClimateOverlay.enabled(c,true);SystemClock.sleep(1100);command(new Intent().putExtra("raw",new int[]{0}).putExtra("left",47));await("climate coexists with menu",()->state().climateOverlayVisible&&state().carMenuOverlayVisible);overlayOptions(true,true);await("volume coexists with both",()->state().overlayVisible&&state().overlayBottomPx>state().climateOverlayHeight);SystemClock.sleep(350);screenshot("probe-v062-menu-climate-TEST.png");
        JSONObject export=new JSONObject(ProbeState.INSTANCE.exportJson());check("menu diagnostics exported",export.getJSONObject("carMenuOverlay").getBoolean("visible"));check("raw and original menu export separate",export.getJSONObject("bsjDecoded").getJSONObject("values").has("136")&&export.getJSONObject("lastValues").getJSONObject("136").getJSONArray("strings").getString(0).equals("TREBLE       +2"));
        overlayOptions(false,false);ClimateOverlay.enabled(c,false);raw(menuPacket("TEST · PROFILE","BASS          0",""));await("menu shown before profile change",()->state().carMenuOverlayVisible);command(new Intent().putExtra("profile",999));await("unsupported profile removes menu",()->!state().carMenuOverlayVisible);command(new Intent().putExtra("profile",6291908).putExtra("raw",new int[]{0}));SystemClock.sleep(1100);check("profile return does not revive menu",!state().carMenuOverlayVisible);
        command(new Intent().putExtra("menu2","BASS         +3"));await("fresh menu after profile reset",()->state().carMenuOverlayVisible);command(new Intent().putExtra("kill",true));await("disconnect hides menu",()->!state().connected&&!state().carMenuOverlayVisible);await("menu service reconnects",()->state().connected&&state().number(139)!=null);SystemClock.sleep(1100);check("reconnect suppresses old menu",!state().carMenuOverlayVisible);
        command(new Intent().putExtra("menu2","BASS         +4"));await("menu after reconnect",()->state().carMenuOverlayVisible);stopProbe();await("stop removes menu",()->!state().running&&!state().carMenuOverlayVisible);check("all menu paths send zero commands",receipts(false).length()==0);
        startProbe();await("restarted for local demo",()->state().connected);CarMenuOverlay.enabled(c,false);c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY).putExtra("carMenuDemo",true));await("demo has explicit test label",()->state().carMenuOverlayVisible&&state().carMenuOverlayStatus.contains("Тест меню"));check("demo does not replace real fields",!state().menuRow(0).equals("AUDIO SETTINGS"));SystemClock.sleep(350);screenshot("probe-v062-menu-demo-TEST.png");closeMenu();stopProbe();await("demo stopped",()->!state().running&&!state().carMenuOverlayVisible);
        CarMenuOverlay.enabled(c,false);AudioSourceController.enabled(c,true);ThemeSettings.mode(c,ThemeSettings.AUTO);
    }

    private void sourcePopupTests()throws Exception {
        Context c=getTargetContext();receipts(true);VolumeOverlay.options(c,false,false);ClimateOverlay.enabled(c,false);CarMenuOverlay.enabled(c,false);AudioSourceController.enabled(c,false);
        SourceOverlay.enabled(c,true);c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY));
        c.startActivity(new Intent().setComponent(new ComponentName("com.syu.ms","app.OverlayTargetActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        await("initial source popup",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("FM2"));
        await("initial source auto hides",()->!state().sourceOverlayVisible);
        check("hidden source waits for a change",state().sourceOverlayStatus.contains("Ожидает смены"));
        command(new Intent().putExtra("volume",19).putExtra("source",1));SystemClock.sleep(700);
        check("same source and volume do not reopen",!state().sourceOverlayVisible);
        command(new Intent().putExtra("source",2));await("CD opens transient popup",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("CD"));
        long start=SystemClock.elapsedRealtime();SystemClock.sleep(1900);command(new Intent().putExtra("source",2).putExtra("volume",20));
        ThemeSettings.mode(c,ThemeSettings.NIGHT);c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY));
        await("duplicates and theme do not prolong",()->!state().sourceOverlayVisible);
        check("hide deadline stays within four seconds",SystemClock.elapsedRealtime()-start<4000);
        command(new Intent().putExtra("source",4));await("USB reopens",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("USB/iPod"));
        SystemClock.sleep(1700);command(new Intent().putExtra("source",3));await("AUX replaces label",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("AUX"));
        SystemClock.sleep(1500);check("new source gets a fresh deadline",state().sourceOverlayVisible);
        await("AUX also auto hides",()->!state().sourceOverlayVisible);
        command(new Intent().putExtra("source",1).putExtra("band",1));await("radio FM1",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("FM1"));
        await("radio auto hides",()->!state().sourceOverlayVisible);
        command(new Intent().putExtra("band",2));await("band switch reopens",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("FM2"));
        command(new Intent().putExtra("source",0));await("unknown source dismissed",()->!state().sourceOverlayVisible);
        command(new Intent().putExtra("source",2));await("known source restored",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("CD"));
        SourceOverlay.enabled(c,false);c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY));
        await("disable hides immediately",()->!state().sourceOverlayVisible);
        command(new Intent().putExtra("source",4));SystemClock.sleep(500);check("disabled stays hidden",!state().sourceOverlayVisible);
        SourceOverlay.enabled(c,true);c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY));
        await("enabled shows current briefly",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("USB/iPod"));
        stopProbe();await("disconnect removes source immediately",()->!state().running&&!state().sourceOverlayVisible);
        check("popup sends no vehicle commands",receipts(false).length()==0);
        startProbe();await("reconnect with fresh source popup",()->state().connected&&state().sourceOverlayVisible&&state().sourceOverlayText.equals("USB/iPod"));
        ThemeSettings.mode(c,ThemeSettings.DAY);
        c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY).putExtra("climateDemo",true));
        await("climate preview visible",()->state().climateOverlayVisible);SystemClock.sleep(600);screenshot("probe-v070-icons-day-TEST.png");
        ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(700);screenshot("probe-v070-icons-night-TEST.png");
        runOnMainSync(()->{
            ThemeSettings.mode(c,ThemeSettings.DAY);ClimatePopupView panel=new ClimatePopupView(c);panel.content(state(),true);
            float density=c.getResources().getDisplayMetrics().density;int width=Math.round(600*density),height=Math.round(240*density);
            panel.layout(0,0,width,height);Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);panel.draw(new android.graphics.Canvas(bitmap));
            try(OutputStream out=new FileOutputStream(new File(c.getExternalFilesDir(null),"probe-v070-icons-compact-TEST.png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}catch(IOException e){throw new AssertionError(e);}finally{bitmap.recycle();}
        });
        check("compact panel rendered",new File(c.getExternalFilesDir(null),"probe-v070-icons-compact-TEST.png").length()>0);
        stopProbe();await("all previews removed",()->!state().running&&!state().sourceOverlayVisible&&!state().climateOverlayVisible);
        SourceOverlay.enabled(c,false);ThemeSettings.mode(c,ThemeSettings.AUTO);AudioSourceController.enabled(c,true);
    }

    private void climateImageTests()throws Exception {
        Context c=getTargetContext();receipts(true);VolumeOverlay.options(c,false,false);SourceOverlay.enabled(c,false);CarMenuOverlay.enabled(c,false);AudioSourceController.enabled(c,false);ClimateOverlay.enabled(c,false);
        c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY));
        c.startActivity(new Intent().setComponent(new ComponentName("com.syu.ms","app.OverlayTargetActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ThemeSettings.mode(c,ThemeSettings.DAY);
        c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY).putExtra("climateDemo",true));
        await("climate preview visible",()->state().climateOverlayVisible);SystemClock.sleep(600);screenshot("probe-v070-icons-day-TEST.png");
        ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(700);screenshot("probe-v070-icons-night-TEST.png");
        runOnMainSync(()->{
            ThemeSettings.mode(c,ThemeSettings.DAY);ClimatePopupView panel=new ClimatePopupView(c);panel.content(state(),true);
            float density=c.getResources().getDisplayMetrics().density;int width=Math.round(600*density),height=Math.round(240*density);
            panel.layout(0,0,width,height);Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);panel.draw(new android.graphics.Canvas(bitmap));
            try(OutputStream out=new FileOutputStream(new File(c.getExternalFilesDir(null),"probe-v070-icons-compact-TEST.png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}catch(IOException e){throw new AssertionError(e);}finally{bitmap.recycle();}
        });
        check("compact panel rendered",new File(c.getExternalFilesDir(null),"probe-v070-icons-compact-TEST.png").length()>0);
        stopProbe();await("all previews removed",()->!state().running&&!state().sourceOverlayVisible&&!state().climateOverlayVisible);
        SourceOverlay.enabled(c,false);ThemeSettings.mode(c,ThemeSettings.AUTO);AudioSourceController.enabled(c,true);
    }

    private JSONObject routes() {
        try { return new JSONObject(ProbeState.INSTANCE.exportJson()).getJSONObject("audioRoutes"); }
        catch(Exception e) { throw new AssertionError(e); }
    }
    private Integer routeNumber(String key) {
        JSONObject value=routes().optJSONObject("lastValues").optJSONObject(key);
        if(value==null||value.optJSONArray("ints")==null||value.optJSONArray("ints").length()==0)return null;
        return value.optJSONArray("ints").optInt(0);
    }
    private JSONArray routeSubscriptions() {
        try {
            CountDownLatch latch=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();
            getTargetContext().sendOrderedBroadcast(new Intent().setComponent(new ComponentName("com.syu.ms","app.ControlReceiver"))
                .putExtra("getRoutes",true),null,new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){result.set(getResultData());latch.countDown();}},null,Activity.RESULT_OK,null,null);
            if(!latch.await(5,TimeUnit.SECONDS))throw new AssertionError("route receipt timeout");
            return new JSONArray(result.get());
        }catch(Exception e){throw new AssertionError(e);}
    }
    private void routeTests()throws Exception {
        receipts(true);
        ProbeState partial=new ProbeState();partial.newSession();partial.event(1000,new int[]{6291908},null,null);partial.status("Partial","",true);
        check("partial connection does not fabricate power",partial.snapshot().climatePower()==null);
        check("partial climate can render",partial.snapshot().climate().contains("Питание: —"));
        partial.event(21,new int[]{8},null,null);check("invalid fan with absent power is safe",partial.snapshot().climatePower()==null);
        partial.event(21,new int[]{0},null,null);check("known zero fan means power off",Integer.valueOf(0).equals(partial.snapshot().climatePower()));
        partial.event(21,new int[]{7},null,null);check("known fan means power on",Integer.valueOf(1).equals(partial.snapshot().climatePower()));
        await("MAIN initial value",()->Integer.valueOf(13).equals(routeNumber("0:0")));
        check("IPOD stub does not become disconnected",routeNumber("5:0")==null);
        await("thirteen optional subscriptions including MAIN screen",()->routeSubscriptions().length()==13);
        JSONObject root=new JSONObject(ProbeState.INSTANCE.exportJson());
        check("module zero does not pollute CAN code zero",!root.getJSONObject("lastValues").has("0"));
        check("MAIN13 does not choose USB",AudioMode.reported(state())==AudioMode.RADIO);
        check("separate modules exported",routes().optJSONArray("modules").length()==2);
        check("silent device remains unknown",ProbeState.INSTANCE.audioRoutes.summary().contains("iPod state: —"));
        command(new Intent().putExtra("routeMain",7).putExtra("ipodState",3).putExtra("ipodExists",1).putExtra("ipodTitle","TEST IPOD TRACK"));
        await("MAIN change recorded",()->Integer.valueOf(7).equals(routeNumber("0:0")));
        await("device callback namespace",()->Integer.valueOf(3).equals(routeNumber("5:0"))&&Integer.valueOf(1).equals(routeNumber("5:15")));
        await("string metadata",()->routes().optJSONObject("lastValues").optJSONObject("5:8")!=null);
        check("iPod metadata preserved",routes().optJSONObject("lastValues").getJSONObject("5:8").getJSONArray("strings").getString(0).equals("TEST IPOD TRACK"));
        check("headunit channel is not OEM subtype",AudioMode.reported(state())==AudioMode.RADIO&&state().source().equals("FM2"));
        check("MAIN channel visible",ProbeState.INSTANCE.audioRoutes.summary().contains("iPod [7]"));
        ProbeState.INSTANCE.mark("iPod playing test");root=new JSONObject(ProbeState.INSTANCE.exportJson());
        JSONObject marker=root.getJSONArray("markers").getJSONObject(root.getJSONArray("markers").length()-1);
        check("marker has same-time MAIN reading",marker.getJSONObject("audioRoutes").getJSONObject("lastValues").getJSONObject("0:0").getJSONArray("ints").getInt(0)==7);
        check("marker declares manual origin",marker.getString("origin").startsWith("manual"));
        check("module five not CAN5",state().number(5)==null);
        check("read subscriptions do not issue commands",receipts(false).length()==0);
        stopProbe();await("routes stopped",()->!state().running&&!routes().optBoolean("live"));
        await("all route subscriptions released",()->routeSubscriptions().length()==0);
        long before=routes().getLong("receivedEvents");command(new Intent().putExtra("lateRoute",true));SystemClock.sleep(400);
        check("late callback after stop ignored",routes().getLong("receivedEvents")==before);
        check("stop keeps last samples",routeNumber("0:0")==7);
        command(new Intent().putExtra("noRouteModules",true));SystemClock.sleep(300);startProbe();
        await("CAN works without optional modules",()->state().connected&&state().honda()&&state().number(132)!=null);
        await("unavailable modules diagnosed",()->routes().optJSONArray("modules").length()==2&&routes().optJSONArray("modules").optJSONObject(0).optString("status").startsWith("Unavailable"));
        check("optional failure does not fabricate route",routes().optJSONObject("lastValues").length()==0);
        check("new session has no stale samples",routes().getLong("callbacksThisSession")==0&&routeNumber("0:0")==null);
        check("history retains previous session",routes().getLong("receivedEvents")==before);
        command(new Intent().putExtra("lateRoute",true));SystemClock.sleep(400);
        check("old callback after reconnect ignored",routes().getLong("receivedEvents")==before);
        check("CAN source unchanged after optional failure",state().source().equals("FM2"));
        stopProbe();await("stop unavailable session",()->!state().running&&!state().connected);
        command(new Intent().putExtra("noRouteModules",false).putExtra("routeMain",13));SystemClock.sleep(300);
        startProbe();await("optional modules recovered",()->Integer.valueOf(13).equals(routeNumber("0:0")));
        check("session tagged on callback",routes().optJSONObject("lastValues").getJSONObject("0:0").getLong("session")==state().sessionId);
        stopProbe();await("final cleanup",()->!state().running&&!routes().optBoolean("live"));
        await("final subscriptions removed",()->routeSubscriptions().length()==0);
        check("all diagnostic paths write zero commands",receipts(false).length()==0);

        AudioRouteCapture local=new AudioRouteCapture();local.reset(42);local.start();
        local.event(0,0,new int[96],new float[]{Float.NaN,Float.POSITIVE_INFINITY,1},new String[]{new String(new char[600]).replace('\0','x'),"2","3","4","5"});
        JSONObject value=local.json().getJSONObject("lastValues").getJSONObject("0:0");
        check("integer array bounded",value.getJSONArray("ints").length()==32&&value.getInt("originalInts")==96);
        check("strings array bounded",value.getJSONArray("strings").length()==4&&value.getInt("originalStrings")==5);
        check("text bounded",value.getJSONArray("strings").getString(0).length()==256);
        check("nonfinite floats serialize safely",value.getJSONArray("floats").isNull(0)&&value.getJSONArray("floats").isNull(1));
        check("truncation disclosed",value.getBoolean("truncated"));
        local.event(0,98,new int[]{4},null,null);local.event(7,0,new int[]{7},null,null);
        check("unrequested modules and fields rejected",local.json().getLong("receivedEvents")==1);
        for(int i=0;i<1100;i++)local.event(0,0,new int[]{i},null,null);
        check("event ring bounded",local.json().getJSONArray("events").length()==1024);
        check("dropped events counted",local.json().getLong("droppedEvents")==77);
        check("history session identified",local.json().getJSONArray("events").getJSONObject(0).getLong("session")==42);
        JSONObject frozen=local.markerContext();local.event(0,0,new int[]{7},null,null);
        check("manual snapshot immutable",frozen.getJSONObject("lastValues").getJSONObject("0:0").getJSONArray("ints").getInt(0)==1099);
        local.stop();local.event(0,0,new int[]{99},null,null);
        check("stopped recorder rejects callbacks",local.json().getJSONObject("lastValues").getJSONObject("0:0").getJSONArray("ints").getInt(0)==7);
        local.reset(43);check("reset clears latest and counter",local.json().getJSONObject("lastValues").length()==0&&local.json().getLong("callbacksThisSession")==0);
        check("reset retains bounded historical capture",local.json().getJSONArray("events").length()==1024);
    }

    private int[] mediaPacket(int kind,String text){byte[] bytes=text.getBytes(StandardCharsets.UTF_8);int[] p=new int[bytes.length+2];p[0]=kind;for(int i=0;i<bytes.length;i++)p[i+1]=bytes[i]&255;return packet(7,p);}
    private void deviceFixes()throws Exception {
        checks+=new MediaTests().run(getContext().getAssets());Context c=getTargetContext();SourceSettings.aux(c,-1);SourceOverlay.enabled(c,true);VolumeOverlay.options(c,false,false);ClimateOverlay.enabled(c,false);AudioSourceController.enabled(c,true);ThemeSettings.mode(c,ThemeSettings.DAY);
        startProbe();await("device fix live",()->state().connected&&state().honda());receipts(true);command(new Intent().putExtra("source",0).putExtra("raw",new int[]{0}));await("baseline no source",()->state().audioSource()==0);
        launcher("CD");playerMode("CD");raw(packet(3,0x82,7));playerMode("USB");await("source3 overlay",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("USB/iPod"));
        raw(packet(5,0,0,1,2,0,2,0,0x41));raw(mediaPacket(5,"Название песни"));raw(mediaPacket(3,"Русский исполнитель"));await("raw Cyrillic title on player",()->playerNode("Название песни")!=null);await("raw artist on player",()->playerNode("Русский исполнитель")!=null);screenshot("probe-v071-USB-Cyrillic-TEST.png");
        launcher("IPod");playerMode("IPOD");await("iPod shares actual metadata",()->playerNode("Название песни")!=null&&playerNode("Русский исполнитель")!=null);screenshot("probe-v071-iPod-text-TEST.png");
        await("source popup expires",()->!state().sourceOverlayVisible);raw(packet(3,0x83,7));await("same-label different-code popup reopens",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("USB/iPod"));playerMode("IPOD");check("manual iPod survives shared-family transition",state().audioPlayerMode.equals("IPOD"));
        Activity main=startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));click(main,"Запомнить текущий режим как AUX");await("AUX dialog",()->playerNode("Да, это AUX")!=null);clickAccessibility("Да, это AUX");await("calibrated AUX persisted",()->SourceSettings.aux(c)==4&&state().auxSource==4);await("confirmed AUX hides player",()->!state().audioPlayerVisible);check("source label is calibrated AUX",state().source().equals("AUX"));
        raw(packet(3,0x82,7));playerMode("USB");check("source3 still external with AUX4",state().source().equals("USB/iPod"));check("UI calibration sends no commands",receipts(false).length()==0);
        SourceSettings.aux(c,-1);command(new Intent().putExtra("recirc",0));await("unpublished recirc stays unknown",()->!state().optionalKnown(12));runOnMainSync(()->{ClimatePopupView view=new ClimatePopupView(c);view.content(state(),false);check("hidden recirculation is omitted from panel description",!view.getContentDescription().toString().contains("Рециркуляция"));});
        command(new Intent().putExtra("recirc",1));await("real recirc callback can reflect on",()->state().optionalKnown(12)&&state().flag(12).equals("вкл"));command(new Intent().putExtra("recirc",0));await("observed recirc off reflects",()->state().flag(12).equals("выкл"));
        stopProbe();await("device fixes stop",()->!state().connected&&!state().running);SourceSettings.aux(c,-1);SourceOverlay.enabled(c,false);
    }

    private void analyzerUiTests()throws Exception {
        Context c=getTargetContext();receipts(true);SourceOverlay.enabled(c,false);ClimateOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);AudioSourceController.enabled(c,false);
        ProbeState.INSTANCE.analyzer.clear(System.currentTimeMillis());ThemeSettings.mode(c,ThemeSettings.DAY);
        Activity analyzer=startActivitySync(new Intent(c,AnalyzerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        await("analyzer connected",()->contains(analyzer.findViewById(android.R.id.content),"Подключено"));
        for(String label:BsjAnalyzer.LABELS)check("all marker buttons "+label,button(analyzer.findViewById(android.R.id.content),label)!=null);
        click(analyzer,"ENGINE_IDLE");raw(packet(0x45,255,128,1));await("parsed unknown from IPC",()->!ProbeState.INSTANCE.analyzer.snapshot().frames.isEmpty());
        click(analyzer,"Кадры");await("unknown grouped in UI",()->contains(analyzer.findViewById(android.R.id.content),"45/3"));await("unsigned payload shown",()->contains(analyzer.findViewById(android.R.id.content),"FF 80 01"));
        screenshot("probe-v070-analyzer-frames-TEST.png");click(analyzer,"Watch");await("watch u8 current",()->contains(analyzer.findViewById(android.R.id.content),"u8  255"));check("watch signed byte",contains(analyzer.findViewById(android.R.id.content),"s8  −1")||contains(analyzer.findViewById(android.R.id.content),"s8  -1"));
        screenshot("probe-v070-analyzer-watch-TEST.png");click(analyzer,"Метки");await("window complete displayed",()->contains(analyzer.findViewById(android.R.id.content),"готово"));
        click(analyzer,"BRAKE_ON");raw(packet(0x45,0,2,3));SystemClock.sleep(5500);click(analyzer,"Сравнить");await("window byte comparison",()->contains(analyzer.findViewById(android.R.id.content),"BYTE | A"));screenshot("probe-v070-analyzer-diff-TEST.png");
        JSONObject export=new JSONObject(ProbeState.INSTANCE.exportJson());check("new export v2",export.getInt("exportVersion")==2);check("old raw retained",export.getJSONObject("rawBsj").getJSONArray("chunks").length()>0);check("saved marker windows",export.getJSONArray("experimentWindows").length()==2);check("no false signal from markers",export.getJSONArray("candidateSignals").length()==0);
        runOnMainSync(()->ThemeSettings.mode(c,ThemeSettings.NIGHT));SystemClock.sleep(1600);check("shared night preserves view",contains(analyzer.findViewById(android.R.id.content),"BSJ Experiment Analyzer"));screenshot("probe-v070-analyzer-night-TEST.png");
        click(analyzer,"Метки");command(new Intent().putExtra("profile",999).putExtra("raw",new int[]{0}));await("wrong profile disables experiments",()->!button(analyzer.findViewById(android.R.id.content),"ENGINE_IDLE").isEnabled());check("model marker rejected with wrong profile",ProbeState.INSTANCE.markExperiment("ENGINE_IDLE")==-1);
        stopProbe();await("analyzer disconnected",()->!ProbeState.INSTANCE.analyzer.snapshot().active);check("analyzer sends no CAN commands",receipts(false).length()==0);
        runOnMainSync(analyzer::finish);ThemeSettings.mode(c,ThemeSettings.DAY);
    }

    private void v072Fixes()throws Exception{
        checks+=new V072Models().run();Context c=getTargetContext();SourceSettings.aux(c,-1);AudioSourceController.enabled(c,true);SourceOverlay.enabled(c,false);ClimateOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);ThemeSettings.mode(c,ThemeSettings.DAY);ProbeState.INSTANCE.carMenuMemory.clear();getTargetContext().getSharedPreferences("carMenuMemory",0).edit().clear().commit();ProbeState.INSTANCE.radioPresets.clear();c.getSharedPreferences("radioPresets",0).edit().clear().commit();
        startProbe();await("v072 connected",()->state().connected&&state().mainChannel!=null);receipts(true);
        command(new Intent().putExtra("source",1).putExtra("band",2).putExtra("frequency",241).putExtra("preset1",181).putExtra("preset2",241).putExtra("preset3",281).putExtra("preset4",301).putExtra("preset5",361).putExtra("preset6",401));launcher("Radio");playerMode("RADIO");await("six presets visible without selecting slots",()->playerNode("6  107.50")!=null&&playerNode("1  96.50")!=null);
        command(new Intent().putExtra("source",2));playerMode("CD");command(new Intent().putExtra("source",1));playerMode("RADIO");check("old received preset remains visible after source return",playerNode("6  107.50")!=null);screenshot("probe-v072-presets-TEST.png");
        await("preset cache saved",()->c.getSharedPreferences("radioPresets",0).getString("received","").contains("401"));check("preset learning has zero vehicle writes",receipts(false).length()==0);
        command(new Intent().putExtra("routeMain",0).putExtra("routeDelayMs",650));await("non-OEM route before request",()->Integer.valueOf(0).equals(state().mainChannel));audio("CD");await("route request recorded",()->{try{return receipts(false).length()==1;}catch(Exception e){return false;}});SystemClock.sleep(250);check("no premature CAN mode request",receipts(false).length()==1);
        await("mode only after MAIN13 ack",()->{try{return receipts(false).length()==2;}catch(Exception e){return false;}});JSONArray sent=receipts(false);step(sent,0,0,0,13);step(sent,1,7,1,1);check("async route settles first",sent.getJSONObject(1).getLong("atMs")-sent.getJSONObject(0).getLong("atMs")>=600);raw(packet(3,0x81,7));await("fresh source observation reported",()->!state().commandBusy&&state().commandStatus.contains("Получено состояние CD"));
        receipts(true);command(new Intent().putExtra("routeMain",0).putExtra("routeDelayMs",-1));await("blocked route ready",()->Integer.valueOf(0).equals(state().mainChannel));audio("USB");await("route timeout prevents mode command",()->!state().commandBusy&&state().commandStatus.contains("CAN-команда выбора источника не отправлена"));check("failed route has no CAN command",receipts(false).length()==1);
        command(new Intent().putExtra("routeMain",13).putExtra("routeDelayMs",0));await("OEM route restored",()->Integer.valueOf(13).equals(state().mainChannel));launcher("CD");playerMode("CD");await("active source can be reselected",()->playerNode("Включить CD")!=null&&playerNode("Включить CD").isEnabled());
        Activity main=startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));check("stock comparison button exists",button(main.findViewById(android.R.id.content),"Штатный выбор Radio / CD / USB")!=null);
        AnalyzerActivity analyzer=(AnalyzerActivity)startActivitySync(new Intent(c,AnalyzerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));raw(packet(0x7f,88,67,45,71,66,74,82,45,48,55,50,50,86,55,48,52));await("adapter identity in analyzer",()->contains(analyzer.findViewById(android.R.id.content),"XC-GBJR-0722V704"));SystemClock.sleep(1500);await("raw staleness visible",()->contains(analyzer.findViewById(android.R.id.content),"NO DATA"));click(analyzer,"ENGINE_IDLE");await("empty marker visibly flagged",()->contains(analyzer.findViewById(android.R.id.content),"INVALID_NO_RAW_DATA"));screenshot("probe-v072-raw-no-data-TEST.png");
        long session=state().sessionId;runOnMainSync(analyzer::finish);launcher("CD");playerMode("CD");check("screen navigation preserves capture session",state().sessionId==session);stopProbe();await("v072 stopped",()->!state().running);SourceSettings.aux(c,-1);command(new Intent().putExtra("routeDelayMs",0));ThemeSettings.mode(c,ThemeSettings.AUTO);
    }

    private MainActivity home()throws Exception{return (MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK));}
    private void homeLanguage()throws Exception{
        Context c=getTargetContext();SourceSettings.aux(c,-1);AudioSourceController.enabled(c,false);SourceOverlay.enabled(c,false);ClimateOverlay.enabled(c,false);CarMenuOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);ThemeSettings.mode(c,ThemeSettings.DAY);I18n.language(c,"ru");
        MainActivity main=home();await("home starts reading",()->state().connected&&main.findViewById(R.id.home_radio).isEnabled());receipts(true);long session=state().sessionId;
        check("Russian home heading",contains(main.findViewById(android.R.id.content),"Выберите источник"));check("three source choices",contains(main.findViewById(android.R.id.content),"Radio")&&contains(main.findViewById(android.R.id.content),"CD")&&contains(main.findViewById(android.R.id.content),"USB / iPod"));check("diagnostic controls moved out of home",button(main.findViewById(android.R.id.content),"Сохранить журнал")==null&&button(main.findViewById(android.R.id.content),"Тест климата · 15 секунд")==null);check("reported radio highlighted",main.findViewById(R.id.home_radio).isSelected());screenshot("honda-v080-home-ru-day-TEST.png");
        runOnMainSync(()->main.findViewById(R.id.home_settings).performClick());await("settings navigation",()->{android.view.accessibility.AccessibilityNodeInfo node=getUiAutomation().getRootInActiveWindow();return node!=null&&!node.findAccessibilityNodeInfosByText("Сохранить журнал").isEmpty();});
        SettingsActivity settings=(SettingsActivity)startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));check("settings preserve raw fields",button(settings.findViewById(android.R.id.content),"Все полученные поля CAN")!=null);check("settings preserve stock comparison",button(settings.findViewById(android.R.id.content),"Штатный выбор Radio / CD / USB")!=null);click(settings,"English");await("settings English applied",()->contains(settings.findViewById(android.R.id.content),"Language")&&button(settings.findViewById(android.R.id.content),"Save log")!=null);check("English selected and persisted",I18n.language(c).equals("en"));check("sun theme explanation translated",ThemeSettings.resolve(c).description.equals("Day · selected manually"));check("stored status translates without reconnect",I18n.stored("Подключение…").equals("Connecting…"));check("direction and DUAL semantics survive localization",state().climate().contains("Face: on")&&state().dual().equals("on"));check("session preserved by language switch",state().sessionId==session);check("language and settings send no audio commands",receipts(false).length()==0);screenshot("honda-v080-settings-en-TEST.png");
        click(settings,"Night");check("manual night preserved",ThemeSettings.mode(c).equals(ThemeSettings.NIGHT));runOnMainSync(settings::finish);MainActivity english=home();await("English home on return",()->contains(english.findViewById(android.R.id.content),"Select a source"));check("settings label English",button(english.findViewById(android.R.id.content),"Settings")!=null);check("new activity uses persisted language",I18n.language(c).equals("en"));screenshot("honda-v080-home-en-night-TEST.png");
        int[] ids={R.id.home_radio,R.id.home_cd,R.id.home_usb};int[] wire={0,1,3};String[] names={"RADIO","CD","USB"};
        for(int i=0;i<3;i++){receipts(true);MainActivity target=home();await("choice ready "+names[i],()->target.findViewById(R.id.home_radio).isEnabled());final int id=ids[i];runOnMainSync(()->target.findViewById(id).performClick());playerMode(names[i]);await("one selection plan "+names[i],()->{try{return receipts(false).length()==2;}catch(Exception e){return false;}});JSONArray sent=receipts(false);step(sent,0,0,0,13);step(sent,1,7,1,wire[i]);command(new Intent().putExtra("source",wire[i]+1));await("selection ends "+names[i],()->!state().commandBusy);check("tap sends no duplicate plan "+names[i],receipts(false).length()==2);}
        launcher("USB");playerMode("USB");command(new Intent().putExtra("title","Русское название").putExtra("artist","Исполнитель").putExtra("dual",0).putExtra("left",44).putExtra("right",44));await("actual Cyrillic title unchanged in English",()->playerNode("Русское название")!=null&&state().cdArtist().equals("Исполнитель"));check("actual artist unchanged in English",state().cdArtist().equals("Исполнитель"));check("English player labels",playerNode("Select USB")!=null&&playerNode("Settings")!=null);check("DUAL equal temperatures remains off",state().dual().equals("off"));screenshot("honda-v080-player-en-TEST.png");
        AnalyzerActivity analyzer=(AnalyzerActivity)startActivitySync(new Intent(c,AnalyzerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));check("analyzer labels English",button(analyzer.findViewById(android.R.id.content),"Markers")!=null&&button(analyzer.findViewById(android.R.id.content),"Frames")!=null);click(analyzer,"Frames");await("stable analyzer tab IDs English",()->{android.view.accessibility.AccessibilityNodeInfo node=getUiAutomation().getRootInActiveWindow();return node!=null&&!node.findAccessibilityNodeInfosByText("Frames: unknown types only").isEmpty();});I18n.language(c,"ru");await("analyzer switches language preserving frame tab",()->{android.view.accessibility.AccessibilityNodeInfo node=getUiAutomation().getRootInActiveWindow();return node!=null&&!node.findAccessibilityNodeInfosByText("Кадры: только неизвестные типы").isEmpty();});check("stored English status reverses to Russian",I18n.stored("Connecting…").equals("Подключение…"));runOnMainSync(analyzer::finish);
        receipts(true);command(new Intent().putExtra("profile",999));MainActivity invalid=home();await("wrong profile disables selector",()->!invalid.findViewById(R.id.home_cd).isEnabled());runOnMainSync(()->invalid.findViewById(R.id.home_cd).performClick());SystemClock.sleep(500);check("wrong profile emits no commands",receipts(false).length()==0);stopProbe();await("home stopped",()->!state().running&&!state().connected);await("stopped choices disabled",()->!invalid.findViewById(R.id.home_usb).isEnabled());check("home retains settings without CAN",invalid.findViewById(R.id.home_settings).isEnabled());ThemeSettings.mode(c,ThemeSettings.AUTO);I18n.language(c,"ru");
    }

    private void auxFixes()throws Exception{
        Context c=getTargetContext();stopProbe();await("stopped before AUX migration test",()->!state().running);android.content.SharedPreferences prefs=c.getSharedPreferences("overlay",0);prefs.edit().remove("confirmedAuxSource").commit();check("fresh install defaults AUX3",SourceSettings.aux(c)==3);prefs.edit().putInt("confirmedAuxSource",-1).commit();check("old unmapped sentinel defaults AUX3",SourceSettings.aux(c)==3);SourceSettings.aux(c,5);check("explicit owner override preserved",SourceSettings.aux(c)==5);SourceSettings.aux(c,-1);check("reset restores AUX3 and removes override",SourceSettings.aux(c)==3&&!prefs.contains("confirmedAuxSource")&&state().auxSource==3);
        I18n.language(c,"en");ThemeSettings.mode(c,ThemeSettings.DAY);SourceOverlay.enabled(c,true);AudioSourceController.enabled(c,true);VolumeOverlay.options(c,false,false);ClimateOverlay.enabled(c,false);CarMenuOverlay.enabled(c,false);command(new Intent().putExtra("raw",new int[]{0}).putExtra("source",4).putExtra("profile",6291908));receipts(true);startProbe();await("AUX test connects with USB4",()->state().connected&&Integer.valueOf(4).equals(state().audioSource()));playerMode("USB");check("USB4 not AUX",!AudioMode.aux(state()));
        command(new Intent().putExtra("raw",packet(3,0x82,7)));await("MODE4 to3 labels AUX",()->state().source().equals("AUX"));await("AUX popup is visible",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("AUX"));await("AUX hides ordinary player",()->!state().audioPlayerVisible);check("AUX has no automatic player mode",AudioMode.reported(state())==null);screenshot("honda-box-v083-AUX-TEST.png");
        command(new Intent().putExtra("raw",packet(3,0x83,7)));playerMode("USB");await("return to USB4 labels shared source",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("USB/iPod"));
        SourceSettings.aux(c,5);command(new Intent().putExtra("raw",packet(3,0x84,7)));await("manual override still labels and hides AUX5",()->state().source().equals("AUX")&&!state().audioPlayerVisible);SourceSettings.aux(c,-1);command(new Intent().putExtra("raw",packet(3,0x83,7)));playerMode("USB");command(new Intent().putExtra("raw",packet(3,0x82,7)));await("default AUX3 works after reset",()->state().source().equals("AUX")&&!state().audioPlayerVisible);check("source detection sends no CAN commands",receipts(false).length()==0);stopProbe();await("AUX test stopped",()->!state().running);SourceOverlay.enabled(c,false);I18n.language(c,"ru");ThemeSettings.mode(c,ThemeSettings.AUTO);
    }

    private int[] menuPacket(String... rows){int[] p=new int[49];p[0]=32;for(int i=0;i<Math.min(3,rows.length);i++){byte[] b=rows[i].getBytes(java.nio.charset.StandardCharsets.UTF_8);for(int k=0;k<Math.min(16,b.length);k++)p[1+i*16+k]=b[k]&255;}return packet(9,p);}
    private void playbackMemoryTests()throws Exception {
        Context c=getTargetContext();I18n.language(c,"en");ThemeSettings.mode(c,ThemeSettings.DAY);SourceSettings.aux(c,-1);AudioSourceController.enabled(c,true);SourceOverlay.enabled(c,true);ClimateOverlay.enabled(c,false);CarMenuOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);ProbeState.INSTANCE.carMenuMemory.clear();c.getSharedPreferences("carMenuMemory",0).edit().clear().commit();
        stopProbe();receipts(true);command(new Intent().putExtra("mediaState",0).putExtra("raw",new int[]{0}).putExtra("source",2));startProbe();playerMode("CD");raw(packet(3,0x81,7));raw(packet(5,1,0,0,1,0,9,2,0x56));raw(mediaPacket(5,"Led Zeppelin - Whole Lotta Love"));await("full unsplit text retained",()->playerNode("Led Zeppelin - Whole Lotta Love")!=null);check("no artist or album fields",playerNode("Artist ·")==null&&playerNode("Album ·")==null);raw(mediaPacket(5,""));await("TITLE off does not erase playing track text",()->playerNode("Led Zeppelin - Whole Lotta Love")!=null);SystemClock.sleep(22000);check("text survives idle longer than old expiry",playerNode("Led Zeppelin - Whole Lotta Love")!=null);raw(mediaPacket(5,"Additional display part>"));await("new text still received after idle",()->playerNode("Additional display part")!=null);
        await("original source popup expired",()->!state().sourceOverlayVisible);raw(menuPacket("","Playlists",""));check("one category does not identify iPod",state().mediaDevice==ProbeState.MediaDevice.UNKNOWN);raw(menuPacket("","02: ARTISTS >",""));await("iPod menu switches same carrier to shared screen",()->state().mediaDevice==ProbeState.MediaDevice.IPOD&&state().audioPlayerMode.equals("USB"));check("wire CD source retained",state().audioSource()==2);await("same-code iPod device popup",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("iPod · menu"));await("source menu cache shown",()->playerNode("Previously received menu")!=null&&playerNode("Playlists")!=null&&playerNode("ARTISTS")!=null);screenshot("honda-box-v083-ipod-menu-TEST.png");
        raw(packet(3,0x83,7));playerMode("USB");check("identity cleared on source change",state().mediaDevice==ProbeState.MediaDevice.UNKNOWN);check("CD-carrier pages not leaked to USB",state().cachedMenu.isEmpty());raw(menuPacket("","Folders",""));await("folder-menu USB identity",()->state().mediaDevice==ProbeState.MediaDevice.USB);raw(packet(5,0,0x70,1,2,0,2,0,0x41));raw(mediaPacket(4,"music"));raw(mediaPacket(5,"01 Angry Stupid>"));await("received random folder mode shown",()->playerNode("Random folder")!=null);check("USB menu isolated",state().cachedMenu.contains("Folders")&&!state().cachedMenu.contains("Playlists"));screenshot("honda-box-v083-usb-menu-TEST.png");
        CarMenuMemory restored=new CarMenuMemory();restored.restore(ProbeState.INSTANCE.carMenuMemory.json().toString());check("menu cache round trip source2",restored.summary(2).contains("Playlists")&&!restored.summary(2).contains("Folders"));check("menu cache round trip source4",restored.summary(4).contains("Folders")&&!restored.summary(4).contains("ARTISTS"));for(int i=0;i<20;i++)restored.record(6291908,4,new String[]{"PAGE "+i},i);check("bounded eight pages per source",restored.summary(4).split("\\n").length==8);
        raw(packet(5,0,9,0,0,0,0,0,0));await("No Data clears active text and device",()->state().mediaDevice==ProbeState.MediaDevice.UNKNOWN&&state().combinedMediaText().equals("—"));check("automatic inference and cache send no commands",receipts(false).length()==0);SystemClock.sleep(1300);check("menu persisted locally",c.getSharedPreferences("carMenuMemory",0).getString("received","").contains("Folders"));stopProbe();await("memory test stop",()->!state().running);startProbe();await("memory reconnect",()->state().connected);check("cached menu never restores live device identity",state().mediaDevice==ProbeState.MediaDevice.UNKNOWN);check("menu survives reconnect",ProbeState.INSTANCE.carMenuMemory.summary(2).contains("ARTISTS"));command(new Intent().putExtra("source",0).putExtra("raw",new int[]{0}));raw(menuPacket("","iPod",""));await("iPod menu works without source packet",()->state().audioSource()==0&&state().mediaDevice==ProbeState.MediaDevice.IPOD&&state().audioPlayerMode.equals("USB")&&state().sourceOverlayVisible);check("unknown-source transport controls stay guarded",playerNode("Next")!=null&&!playerNode("Next").isEnabled());stopProbe();await("memory test cleanup",()->!state().running);SourceOverlay.enabled(c,false);ThemeSettings.mode(c,ThemeSettings.AUTO);I18n.language(c,"ru");
    }

    private void sharedMediaTests()throws Exception {
        Context c=getTargetContext();I18n.language(c,"en");ThemeSettings.mode(c,ThemeSettings.DAY);SourceSettings.aux(c,-1);AudioSourceController.enabled(c,true);SourceOverlay.enabled(c,true);ClimateOverlay.enabled(c,false);CarMenuOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);
        stopProbe();receipts(true);command(new Intent().putExtra("mediaState",0).putExtra("raw",new int[]{0}).putExtra("source",4));startProbe();playerMode("USB");check("shared heading",playerNode("USB / iPod")!=null);
        command(new Intent().putExtra("title","Shared title").putExtra("artist","Shared artist"));await("shared metadata visible",()->playerNode("Shared title")!=null);launcher("IPod");playerMode("USB");check("iPod icon opens same media screen",playerNode("USB / iPod")!=null&&playerNode("Shared title")!=null);check("iPod hint explicitly manual",playerNode("icon selected manually")!=null);command(new Intent().putExtra("raw",packet(3,0x81,7)));command(new Intent().putExtra("raw",mediaPacket(5,"Led Zeppelin")));await("manual iPod shares media on CD carrier without relabeling bus",()->state().audioPlayerMode.equals("USB")&&playerNode("Led Zeppelin")!=null&&playerNode("Current source · CD")!=null);check("CD carrier not guessed to be iPod",state().source().equals("CD"));command(new Intent().putExtra("raw",packet(3,0x83,7)));command(new Intent().putExtra("title","Shared title"));await("shared title restored",()->playerNode("Shared title")!=null);screenshot("honda-box-v083-shared-ipod-TEST.png");launcher("USB");playerMode("USB");check("USB icon retains shared content",playerNode("Shared title")!=null);screenshot("honda-box-v083-shared-usb-TEST.png");
        command(new Intent().putExtra("raw",packet(3,0x80,7)));playerMode("RADIO");command(new Intent().putExtra("raw",packet(4,1,0,22,3,1)));await("stock ST Off Normal visible",()->playerNode("ST Off · Normal")!=null);command(new Intent().putExtra("raw",packet(4,1,0,22,0,0x84)));await("raw ST ON SEEK status updates",()->playerNode("ST ON · SEEK+")!=null);screenshot("honda-box-v083-radio-status-TEST.png");check("UI and reads send no CAN commands",receipts(false).length()==0);stopProbe();await("shared media cleanup",()->!state().running);SourceOverlay.enabled(c,false);ThemeSettings.mode(c,ThemeSettings.AUTO);I18n.language(c,"ru");
    }

    private android.view.accessibility.AccessibilityNodeInfo exactPlayerNode(String text){android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root==null)return null;for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text))if(node.getText()!=null&&node.getText().toString().equals(text))return node;return null;}
    private void cdDiscTests()throws Exception {
        Context c=getTargetContext();I18n.language(c,"en");ThemeSettings.mode(c,ThemeSettings.DAY);SourceSettings.aux(c,-1);AudioSourceController.enabled(c,true);SourceOverlay.enabled(c,false);ClimateOverlay.enabled(c,false);CarMenuOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);
        stopProbe();receipts(true);command(new Intent().putExtra("mediaState",0).putExtra("raw",new int[]{0}).putExtra("source",2));startProbe();await("CD preview service connected",()->state().connected);startActivitySync(new Intent(c,AudioPlayerActivity.class).putExtra("mode","CD").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));playerMode("CD");command(new Intent().putExtra("raw",packet(3,0x81,7)));command(new Intent().putExtra("raw",packet(5,3,0,0,0,0,5,2,0x16)));
        await("received disc3 selected",()->exactPlayerNode("Disc 3")!=null&&exactPlayerNode("Disc 3").isSelected());
        for(int i=1;i<=6;i++)check("CD slot "+i+" displayed",exactPlayerNode("Disc "+i)!=null);check("disc1 not selected",!exactPlayerNode("Disc 1").isSelected());screenshot("honda-box-v083-cd-discs-day-TEST.png");
        command(new Intent().putExtra("raw",packet(5,6,0,0,0,0,5,2,0x17)));await("selected disc follows bus",()->exactPlayerNode("Disc 6").isSelected()&&!exactPlayerNode("Disc 3").isSelected());ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(500);screenshot("honda-box-v083-cd-discs-night-TEST.png");
        command(new Intent().putExtra("raw",packet(5,0,0,0,0,0,5,2,0x18)));await("placeholder retains selected disc",()->exactPlayerNode("Disc 6").isSelected());check("CD display sends no commands",receipts(false).length()==0);
        stopProbe();await("CD preview cleanup",()->!state().running);ThemeSettings.mode(c,ThemeSettings.AUTO);I18n.language(c,"ru");
    }

    private void boxFixes()throws Exception {
        checks+=new BoxTests().run();checks+=new MediaTests().run(getContext().getAssets());Context c=getTargetContext();I18n.language(c,"en");ThemeSettings.mode(c,ThemeSettings.DAY);SourceSettings.aux(c,-1);AudioSourceController.enabled(c,true);ClimateOverlay.enabled(c,false);SourceOverlay.enabled(c,true);CarMenuOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);
        android.content.pm.PackageManager pm=c.getPackageManager();java.util.List<android.content.pm.ResolveInfo> icons=pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(c.getPackageName()),0);check("one primary launcher for DUDU",icons.size()==1&&icons.get(0).activityInfo.name.endsWith(".MainActivity"));check("launcher named Honda Box",icons.get(0).loadLabel(pm).toString().equals("Honda Box"));
        stopProbe();receipts(true);command(new Intent().putExtra("mediaState",0).putExtra("source",4).putExtra("raw",new int[]{0}).putExtra("profile",6291908));startProbe();await("USB/iPod opens on foreground initial connection",()->state().audioPlayerVisible&&state().audioPlayerMode.equals("USB"));await("initial USB/iPod source popup",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("USB/iPod"));
        command(new Intent().putExtra("title","A long complete title containing several words that must remain readable without horizontal clipping in the USB player").putExtra("artist","Artist").putExtra("album","Album"));await("long USB title received",()->playerNode("A long complete title containing several words that must remain readable without horizontal clipping in the USB player")!=null);screenshot("honda-box-v082-usb-long-TEST.png");
        command(new Intent().putExtra("source",1));playerMode("RADIO");command(new Intent().putExtra("raw",packet(4,4,0,22,3,1)));await("AM1 unit and physical frequency",()->playerNode("720 kHz")!=null);await("selected preset highlight",()->playerNode("3  720")!=null&&playerNode("3  720").isSelected());screenshot("honda-box-v082-radio-selected-TEST.png");ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(600);screenshot("honda-box-v082-radio-selected-night-TEST.png");ThemeSettings.mode(c,ThemeSettings.DAY);
        command(new Intent().putExtra("raw",packet(4,4,0,22,0,4)));await("manual tuning clears selected preset",()->playerNode("3  720")!=null&&!playerNode("3  720").isSelected());
        command(new Intent().putExtra("title","").putExtra("album","").putExtra("artist","").putExtra("raw",packet(3,0x81,7)));playerMode("CD");command(new Intent().putExtra("raw",packet(5,1,0,0,0,0,1,0,1)));command(new Intent().putExtra("raw",packet(7,5,68,97,122,101,100,32,97,110,100,32,67,111,110,102,117,62,0)));command(new Intent().putExtra("raw",packet(7,5,67,111,110,102,117,115,101,100,0)));await("CD visible continuation",()->playerNode("Dazed and Confused")!=null);screenshot("honda-box-v082-cd-continuation-TEST.png");
        command(new Intent().putExtra("raw",packet(3,0x83,7)));playerMode("USB");command(new Intent().putExtra("raw",packet(7,0,183,183,32,65,0)));await("placeholder boxes in visible player",()->playerNode("□□ A")!=null);screenshot("honda-box-v082-russian-placeholder-TEST.png");
        stopProbe();command(new Intent().putExtra("mediaState",0).putExtra("raw",new int[]{0}).putExtra("source",0).putExtra("title","Part received without source").putExtra("album","").putExtra("artist",""));startProbe();launcher("IPod");playerMode("USB");await("manual media screen retains received text with unknown source",()->playerNode("Part received without source")!=null);check("title never fabricates iPod source",Integer.valueOf(0).equals(state().audioSource()));
        check("automatic source/display handling sends no CAN commands",receipts(false).length()==0);stopProbe();await("Box test cleanup",()->!state().running);SourceOverlay.enabled(c,false);I18n.language(c,"ru");ThemeSettings.mode(c,ThemeSettings.AUTO);
    }
    private void backgroundTests()throws Exception{
        Context c=getTargetContext();stopProbe();await("stopped before boot test",()->!state().running);SourceSettings.aux(c,-1);SourceOverlay.enabled(c,false);ClimateOverlay.enabled(c,false);CarMenuOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);AudioSourceController.enabled(c,true);BackgroundSettings.autoStart(c,true);BackgroundSettings.activate(c);command(new Intent().putExtra("source",2).putExtra("profile",6291908).putExtra("routeScreen",0));receipts(true);
        runOnMainSync(()->new BootReceiver().onReceive(c,new Intent(Intent.ACTION_BOOT_COMPLETED)));await("boot receiver starts capture",()->state().connected&&state().source().equals("CD"));check("capture wants sticky recovery",BackgroundSettings.wanted(c));SystemClock.sleep(900);check("boot leaves CD player closed",!state().audioPlayerVisible);check("boot does not switch sources",receipts(false).length()==0);
        command(new Intent().putExtra("silent",true).putExtra("routeOnly",true).putExtra("routeScreen",0));await("screen off value observed",()->Integer.valueOf(0).equals(ProbeState.INSTANCE.audioRoutes.mainScreenOn()));SystemClock.sleep(2300);long previous=state().sessionId;command(new Intent().putExtra("routeOnly",true).putExtra("routeScreen",1));await("FYT wake resubscribes after stale data",()->state().sessionId>previous&&Integer.valueOf(1).equals(ProbeState.INSTANCE.audioRoutes.mainScreenOn()));command(new Intent().putExtra("silent",false).putExtra("source",2));await("callbacks restored after wake",()->state().connected&&state().source().equals("CD"));SystemClock.sleep(800);check("wake stays in background",!state().audioPlayerVisible);check("wake sends zero audio commands",receipts(false).length()==0);
        stopProbe();await("manual stop completes",()->!state().running);check("manual pause cancels wanted recovery",BackgroundSettings.paused(c)&&!BackgroundSettings.wanted(c));runOnMainSync(()->new BootReceiver().onReceive(c,new Intent(Intent.ACTION_BOOT_COMPLETED)));SystemClock.sleep(600);check("manual pause blocks boot",!state().running);BackgroundSettings.autoStart(c,false);BackgroundSettings.activate(c);runOnMainSync(()->new BootReceiver().onReceive(c,new Intent(Intent.ACTION_BOOT_COMPLETED)));SystemClock.sleep(600);check("disabled autostart blocks boot",!state().running);AudioSourceController.enabled(c,false);MainActivity main=home();await("opening resumes capture despite boot preference off",()->state().connected);check("explicit open clears pause",!BackgroundSettings.paused(c)&&BackgroundSettings.wanted(c));check("home still emits no automatic audio writes",receipts(false).length()==0);
        stopProbe();await("background test cleanup",()->!state().running);BackgroundSettings.autoStart(c,true);
    }

    private void launcher087()throws Exception{
        Context c=getTargetContext();check("service application name",c.getPackageManager().getApplicationLabel(c.getApplicationInfo()).toString().equals("Honda Box Service"));
        Intent serviceIcon=c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());check("main icon is headless entry",serviceIcon!=null&&serviceIcon.getComponent().getClassName().endsWith("BackgroundStartActivity"));
        ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);c.startActivity(new Intent().setComponent(new ComponentName("com.crewbeat.hondabox.launcher","com.crewbeat.hondabox.launcher.LauncherActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Activity screen=waitForMonitorWithTimeout(monitor,8000);removeMonitor(monitor);check("separate Box APK opens full UI",screen instanceof MainActivity);screenshot("honda-box-v087-launcher-ui-TEST.png");check("UI launcher did not send audio commands",receipts(false).length()==0);runOnMainSync(()->screen.finish());stopProbe();await("launcher test cleanup",()->!state().running);
    }

    private void revisionUi()throws Exception{
        Context c=getTargetContext();I18n.language(c,"en");ThemeSettings.mode(c,ThemeSettings.DAY);stopProbe();await("stopped",()->!state().running);receipts(true);
        SourceOverlay.enabled(c,false);CarMenuOverlay.enabled(c,true);ClimateOverlay.enabled(c,false);VolumeOverlay.options(c,true,false);AudioSourceController.enabled(c,true);TrackOverlay.enabled(c,true);
        c.getSharedPreferences("background",0).edit().putBoolean("headless",true).commit();
        command(new Intent().putExtra("profile",6291908).putExtra("silent",false).putExtra("source",2).putExtra("track",134).putExtra("disc",1).putExtra("raw",new int[]{0}));
        getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);SystemClock.sleep(1000);
        c.startActivity(new Intent(c,BackgroundStartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));await("headless connected",()->state().connected);SystemClock.sleep(5500);check("no player after headless start",!state().audioPlayerVisible);check("no Activity foreground",!ProbeApplication.foreground());check("background sends no audio commands",receipts(false).length()==0);
        android.accessibilityservice.AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getUiAutomation().setServiceInfo(info);
        raw(packet(3,0xC0,12));SystemClock.sleep(800);raw(menuPacket("","2\u0000REGIONAL OFF",""));await("RDS full line in overlay",()->state().carMenuOverlayVisible&&menuNode("2 REGIONAL OFF")!=null);screenshot("honda-box-v086-rds-TEST.png");
        check("five RDS entries",MenuListModel.from(state()).items.size()==5);
        raw(packet(3,0x81,13));raw(packet(5,1,0,0,1,0,5,0,33));raw(packet(7,4,77,97,103,105,99));SystemClock.sleep(900);raw(packet(5,0,0,0,0,0,0,0,0));check("CD retained during volume",state().cdNumber(101).equals("5"));
        AudioSourceController.enabled(c,false);getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);SystemClock.sleep(600);raw(packet(5,1,0,0,1,0,6,0,0));SystemClock.sleep(300);raw(packet(7,4,78,101,119,32,116,114,97,99,107));SystemClock.sleep(900);screenshot("honda-box-v086-track-volume-TEST.png");
        check("no commands during display tests",receipts(false).length()==0);stopProbe();await("UI cleanup",()->!state().running);
    }

    private void releaseUi0818()throws Exception{
        Context c=getTargetContext();I18n.language(c,"en");stopProbe();await("release stopped",()->!state().running);
        SourceSettings.hideOnAux(c,false);SourceOverlay.enabled(c,false);AudioSourceController.enabled(c,false);ClimateOverlay.enabled(c,false);ClimateStripOverlay.enabled(c,false);CarMenuOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);TrackOverlay.enabled(c,false);
        command(new Intent().putExtra("profile",6291908).putExtra("source",2).putExtra("mediaState",0).putExtra("silent",false).putExtra("raw",new int[]{0}));startProbe();await("release connected",()->state().connected);receipts(true);
        raw(packet(3,0x81,7));launcher("USB");playerMode("USB");
        raw(packet(6,0x32,0x22,0x22));await("marker iPod on shared screen",()->state().mediaDevice==ProbeState.MediaDevice.IPOD&&state().source().equals("iPod"));
        raw(mediaPacket(7,"River (feat. Ed >"));raw(mediaPacket(7,"Believe"));await("different titles not concatenated in player",()->playerNode("Believe")!=null&&playerNode("River (feat. Ed")==null);
        raw(packet(5,0,0x70,1,2,0,1,0,0x31));await("iPod timer stays iPod",()->state().mediaDevice==ProbeState.MediaDevice.IPOD&&state().cdTime().equals("00:31"));
        raw(mediaPacket(7,"Believe"));
        VolumeOverlay volume=new VolumeOverlay(c);java.lang.reflect.Method label=VolumeOverlay.class.getDeclaredMethod("sourceTitle",ProbeState.Snapshot.class);label.setAccessible(true);
        check("volume includes current selected TITLE",((String)label.invoke(volume,state())).equals("iPod · Believe"));
        getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);await("player backgrounded",()->!state().audioPlayerVisible);
        TrackOverlay card=new TrackOverlay(c);runOnMainSync(()->{card.start();card.demo();});await("track card shown",()->state().trackOverlayVisible);
        java.lang.reflect.Field v=TrackOverlay.class.getDeclaredField("view");v.setAccessible(true);runOnMainSync(()->{try{View view=(View)v.get(card);check("announcement is clickable",view!=null&&view.performClick());}catch(Exception e){throw new RuntimeException(e);}});
        playerMode("USB");check("announcement opens current iPod screen",state().mediaDevice==ProbeState.MediaDevice.IPOD);runOnMainSync(card::close);
        raw(packet(3,0x82,7));SystemClock.sleep(700);check("AUX setting keeps player open",state().audioPlayerVisible&&AudioMode.aux(state()));SourceSettings.hideOnAux(c,true);raw(packet(3,0x82,8));await("AUX setting hides player",()->!state().audioPlayerVisible);
        check("release UI sends no CAN commands",receipts(false).length()==0);stopProbe();await("release cleanup",()->!state().running);TrackOverlay.enabled(c,true);AudioSourceController.enabled(c,true);I18n.language(c,"ru");
    }

    private void cleanMediaUi0810()throws Exception{
        Context c=getTargetContext();I18n.language(c,"en");AudioSourceController.enabled(c,true);SourceOverlay.enabled(c,true);VolumeOverlay.options(c,false,false);ClimateOverlay.enabled(c,false);CarMenuOverlay.enabled(c,false);
        stopProbe();command(new Intent().putExtra("profile",6291908).putExtra("source",2).putExtra("mediaState",0).putExtra("silent",false).putExtra("raw",new int[]{0}));startProbe();playerMode("CD");receipts(true);
        raw(packet(3,0x81,7));raw(mediaPacket(0,"CD album"));await("CD TITLE album label and selected text",()->playerNode("TITLE · Album")!=null&&playerNode("CD album")!=null);raw(mediaPacket(1,"CD track"));await("CD TITLE switches to track only",()->playerNode("TITLE · Track title")!=null&&playerNode("CD track")!=null&&playerNode("CD album")==null);
        raw(packet(3,0x81,7));raw(packet(5,0,0x70,1,2,0,1,1,0));playerMode("USB");await("USB source popup without new source packet",()->state().sourceOverlayVisible&&state().sourceOverlayText.equals("USB"));
        raw(mediaPacket(8,"Tim McMorris"));await("subtype8 artist visible on shared player",()->playerNode("Tim McMorris")!=null);
        raw(mediaPacket(7,"It's a Beautifu>"));raw(mediaPacket(6,"NO INFO"));await("TITLE selected album shown alone",()->playerNode("NO INFO")!=null&&playerNode("Tim McMorris")==null&&playerNode("It's a Beautifu")==null);
        raw(mediaPacket(7,"It's a Beautifu>"));await("TITLE switches back to song only",()->playerNode("It's a Beautifu")!=null&&playerNode("Tim McMorris")==null&&playerNode("NO INFO")==null);
        playerTap("USB ↔ iPod");raw(mediaPacket(8,"iPod artist"));await("iPod TITLE artist label",()->playerNode("TITLE · Artist")!=null&&playerNode("iPod artist")!=null);raw(mediaPacket(7,"iPod song"));await("iPod TITLE switches content and label",()->playerNode("TITLE · Track title")!=null&&playerNode("iPod song")!=null&&playerNode("iPod artist")==null);
        check("raw CD source still preserved",state().audioSource()==2);check("display-only inference sends no audio commands",receipts(false).length()==0);
        stopProbe();await("clean media UI cleanup",()->!state().running);I18n.language(c,"ru");VolumeOverlay.options(c,true,false);
    }

    private void manualMedia0811()throws Exception{
        Context c=getTargetContext();I18n.language(c,"en");AudioSourceController.enabled(c,true);SourceOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);CarMenuOverlay.enabled(c,false);ThemeSettings.mode(c,ThemeSettings.DAY);
        stopProbe();command(new Intent().putExtra("profile",6291908).putExtra("source",4).putExtra("mediaState",0).putExtra("silent",false).putExtra("raw",new int[]{0}).putExtra("title","Shared track"));startProbe();playerMode("USB");receipts(true);
        await("manual switch available",()->exactPlayerNode("USB ↔ iPod")!=null&&exactPlayerNode("USB ↔ iPod").isEnabled());playerTap("USB ↔ iPod");
        await("manual iPod title and presentation",()->exactPlayerNode("iPod")!=null&&state().manualMediaDeviceHint==ProbeState.MediaDevice.IPOD);
        check("manual selection preserves bus source",state().audioSource()==4&&state().mediaDevice==ProbeState.MediaDevice.UNKNOWN);
        check("manual selection keeps playback text",playerNode("Shared track")!=null);check("menu follows explicit iPod display",MenuListModel.from(state()).heading(state()).startsWith("iPod · "));
        ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(500);check("manual hint survives theme rebuild",exactPlayerNode("iPod")!=null&&state().manualMediaDeviceHint==ProbeState.MediaDevice.IPOD);
        raw(packet(3,0x81,7));await("manual shared display survives CD carrier",()->state().audioSource()==2&&state().audioPlayerMode.equals("USB")&&exactPlayerNode("iPod")!=null);
        playerTap("USB ↔ iPod");await("manual switch returns to USB",()->exactPlayerNode("USB")!=null&&state().manualMediaDeviceHint==ProbeState.MediaDevice.USB);
        check("menu follows explicit USB display",MenuListModel.from(state()).heading(state()).startsWith("USB · "));
        ProbeState.INSTANCE.status("test disconnected","",false);await("manual switch available offline",()->exactPlayerNode("USB ↔ iPod")!=null&&exactPlayerNode("USB ↔ iPod").isEnabled());playerTap("USB ↔ iPod");await("offline manual toggle works",()->exactPlayerNode("iPod")!=null);
        check("manual toggles never send audio commands",receipts(false).length()==0);stopProbe();await("manual display cleanup",()->!state().running);I18n.language(c,"ru");ThemeSettings.mode(c,ThemeSettings.DAY);VolumeOverlay.options(c,true,false);
    }

    private void cdVolumeUi0812()throws Exception{
        Context c=getTargetContext();I18n.language(c,"en");AudioSourceController.enabled(c,true);SourceOverlay.enabled(c,false);VolumeOverlay.options(c,true,false);CarMenuOverlay.enabled(c,false);ThemeSettings.mode(c,ThemeSettings.DAY);
        stopProbe();command(new Intent().putExtra("profile",6291908).putExtra("source",0).putExtra("volume",7).putExtra("mediaState",0).putExtra("silent",false).putExtra("raw",new int[]{0}));startProbe();await("CD volume service ready",()->state().connected);launcher("CD");playerMode("CD");receipts(true);
        raw(packet(5,2,0x30,1,1,0,0x59,1,0x38));raw(mediaPacket(7,"CD title before volume"));await("CD fully displayed before first source",()->playerNode("CD title before volume")!=null&&playerNode("01:38")!=null&&playerNode("59")!=null);
        command(new Intent().putExtra("volume",12).putExtra("track",0).putExtra("disc",0).putExtra("minute",0).putExtra("second",0).putExtra("raw",packet(3,0x81,12)));
        raw(packet(5,0,0,0,0,0,0,0,0));await("volume overlay visible",()->state().overlayVisible&&state().overlayText.startsWith("12"));
        check("CD title remains during volume",playerNode("CD title before volume")!=null);check("CD time remains during volume",playerNode("01:38")!=null);check("CD track and disc remain during volume",playerNode("59")!=null&&state().cdNumber(113).equals("2"));
        await("volume popup hides",()->!state().overlayVisible);check("CD remains after volume hides",playerNode("CD title before volume")!=null&&playerNode("01:38")!=null);
        check("display test sends no commands",receipts(false).length()==0);stopProbe();await("CD volume cleanup",()->!state().running);I18n.language(c,"ru");
    }

    private android.view.accessibility.AccessibilityNodeInfo stripNode0816(){for(android.view.accessibility.AccessibilityWindowInfo window:getUiAutomation().getWindows()){android.view.accessibility.AccessibilityNodeInfo found=stripFind0816(window.getRoot());if(found!=null)return found;}return null;}
    private android.view.accessibility.AccessibilityNodeInfo stripFind0816(android.view.accessibility.AccessibilityNodeInfo n){if(n==null)return null;CharSequence d=n.getContentDescription();if(d!=null&&(d.toString().startsWith("Collapse climate strip")||d.toString().startsWith("Expand climate strip")))return n;for(int i=0;i<n.getChildCount();i++){android.view.accessibility.AccessibilityNodeInfo found=stripFind0816(n.getChild(i));if(found!=null)return found;}return null;}
    private android.graphics.Rect stripBounds0816(){android.graphics.Rect r=new android.graphics.Rect();android.view.accessibility.AccessibilityNodeInfo n=stripNode0816();if(n!=null){n.refresh();n.getBoundsInScreen(r);}return r;}
    private void stripTap0816()throws Exception{await("tappable climate strip",()->stripNode0816()!=null&&stripNode0816().isClickable());final android.graphics.Rect[] previous={new android.graphics.Rect()};final int[] stable={0};await("touch geometry settled",()->{android.graphics.Rect now=stripBounds0816();stable[0]=now.equals(previous[0])?stable[0]+1:0;previous[0]=now;return stable[0]>=2;});android.graphics.Rect r=stripBounds0816();for(android.view.accessibility.AccessibilityWindowInfo w:getUiAutomation().getWindows())if(w.getId()==stripNode0816().getWindowId()){android.graphics.Rect outer=new android.graphics.Rect();w.getBoundsInScreen(outer);r.offset(outer.centerX()-r.centerX(),outer.centerY()-r.centerY());}long now=SystemClock.uptimeMillis();android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,0,r.centerX(),r.centerY(),0),up=android.view.MotionEvent.obtain(now,now+60,1,r.centerX(),r.centerY(),0);check("strip touch down",getUiAutomation().injectInputEvent(down,true));SystemClock.sleep(60);check("strip touch up",getUiAutomation().injectInputEvent(up,true));down.recycle();up.recycle();}
    private void foldStrip0816()throws Exception{
        Context c=getTargetContext();I18n.language(c,"en");ThemeSettings.mode(c,ThemeSettings.DAY);c.getSharedPreferences("overlay",0).edit().putBoolean("climateStripCollapsed",false).commit();ClimateStripOverlay.enabled(c,true);ClimateOverlay.enabled(c,true);UnknownOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);SourceOverlay.enabled(c,false);TrackOverlay.enabled(c,false);AudioSourceController.enabled(c,false);CarMenuOverlay.enabled(c,false);
        android.accessibilityservice.AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getUiAutomation().setServiceInfo(info);
        stopProbe();command(new Intent().putExtra("profile",6291908).putExtra("source",2).putExtra("fan",4).putExtra("left",44).putExtra("right",46).putExtra("ac",1).putExtra("face",1).putExtra("feet",1).putExtra("windshield",0).putExtra("silent",false).putExtra("raw",new int[]{0}));startProbe();Activity settings=startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));await("expanded strip visible",()->state().climateStripVisible&&!state().climateStripCollapsed&&stripNode0816()!=null);receipts(true);SystemClock.sleep(1000);android.graphics.Rect expanded=stripBounds0816();check("expanded geometry",expanded.width()==420&&expanded.height()==52);stripTap0816();await("strip collapses with reduced touch area",()->state().climateStripCollapsed&&stripNode0816()!=null&&stripBounds0816().width()==148&&stripBounds0816().height()==36);check("fold preference saved",c.getSharedPreferences("overlay",0).getBoolean("climateStripCollapsed",false));screenshot("honda-box-v0816-handle-day-TEST.png");ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(400);check("theme keeps fold state",state().climateStripCollapsed&&state().climateStripVisible);screenshot("honda-box-v0816-handle-night-TEST.png");
        command(new Intent().putExtra("fan",5));await("popup covers collapsed handle",()->state().climateOverlayVisible&&!state().climateStripVisible);await("handle returns expanded after popup",()->!state().climateOverlayVisible&&state().climateStripVisible&&!state().climateStripCollapsed);check("temporary expansion preserves folded preference",c.getSharedPreferences("overlay",0).getBoolean("climateStripCollapsed",false));SystemClock.sleep(1500);check("temporary strip remains open",!state().climateStripCollapsed);await("temporary strip folds after five visible seconds",()->state().climateStripVisible&&state().climateStripCollapsed&&stripBounds0816().width()==148);check("handle retains updated climate values",state().climateStripText.contains("fan=5"));stripTap0816();await("handle expands on physical tap",()->!state().climateStripCollapsed&&stripNode0816()!=null&&stripBounds0816().width()==420);check("expanded climate still valid",state().climateStripText.contains("22.0°")&&state().climateStripText.contains("23.0°"));screenshot("honda-box-v0816-expanded-night-TEST.png");
        stripTap0816();check("reverse animation click",stripNode0816().performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK));await("interrupted animation returns expanded",()->!state().climateStripCollapsed&&stripNode0816()!=null&&stripBounds0816().width()==420);stripTap0816();await("handle folded before recreation",()->state().climateStripCollapsed&&stripBounds0816().width()==148);ClimateOverlay.enabled(c,false);c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY));command(new Intent().putExtra("fan",0));await("off climate removes folded handle",()->!state().climateStripVisible);command(new Intent().putExtra("fan",4));await("recreated handle preserves collapsed choice",()->state().climateStripVisible&&state().climateStripCollapsed&&stripNode0816()!=null&&stripBounds0816().width()==148);check("folding sends no audio commands",receipts(false).length()==0);ClimateStripOverlay.enabled(c,false);c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY));await("option removes handle",()->!state().climateStripVisible);runOnMainSync(()->settings.finish());stopProbe();await("fold cleanup",()->!state().running);c.getSharedPreferences("overlay",0).edit().putBoolean("climateStripCollapsed",false).commit();I18n.language(c,"ru");ThemeSettings.mode(c,ThemeSettings.DAY);AudioSourceController.enabled(c,true);TrackOverlay.enabled(c,true);VolumeOverlay.options(c,true,false);
    }

    private android.view.accessibility.AccessibilityNodeInfo climateButtonNode(){for(android.view.accessibility.AccessibilityWindowInfo w:getUiAutomation().getWindows()){android.view.accessibility.AccessibilityNodeInfo found=findClimateButton(w.getRoot());if(found!=null)return found;}return null;}
    private android.view.accessibility.AccessibilityNodeInfo findClimateButton(android.view.accessibility.AccessibilityNodeInfo n){if(n==null)return null;if("Open full climate panel".contentEquals(n.getContentDescription()==null?"":n.getContentDescription()))return n;for(int i=0;i<n.getChildCount();i++){android.view.accessibility.AccessibilityNodeInfo found=findClimateButton(n.getChild(i));if(found!=null)return found;}return null;}
    private void stripMotion()throws Exception{
        Context c=getTargetContext();stopProbe();await("motion service stopped",()->!state().running);I18n.language(c,"en");ClimateStripOverlay.alwaysAutoHide(c,false);ClimateStripOverlay.buttonAutoHide(c,false);ClimateOverlay.enabled(c,false);
        java.util.concurrent.atomic.AtomicInteger updates=new java.util.concurrent.atomic.AtomicInteger();WindowManager real=(WindowManager)c.getSystemService(Context.WINDOW_SERVICE);
        WindowManager counted=(WindowManager)java.lang.reflect.Proxy.newProxyInstance(WindowManager.class.getClassLoader(),new Class[]{WindowManager.class},(proxy,method,args)->{if(method.getName().equals("updateViewLayout"))updates.incrementAndGet();try{return method.invoke(real,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}});
        Context wrapped=new ContextWrapper(c){@Override public Object getSystemService(String name){return Context.WINDOW_SERVICE.equals(name)?counted:super.getSystemService(name);}};
        java.lang.reflect.Field af=ClimateStripOverlay.class.getDeclaredField("animator"),lf=ClimateStripOverlay.class.getDeclaredField("layout"),ff=ClimateStripOverlay.class.getDeclaredField("fold"),bf=ClimateStripOverlay.class.getDeclaredField("buttonAnimator"),pf=ClimateStripOverlay.class.getDeclaredField("pendingLayout"),vf=ClimateStripOverlay.class.getDeclaredField("view");for(java.lang.reflect.Field f:new java.lang.reflect.Field[]{af,lf,ff,bf,pf,vf})f.setAccessible(true);
        java.lang.reflect.Method toggle=ClimateStripOverlay.class.getDeclaredMethod("toggle");toggle.setAccessible(true);
        for(boolean night:new boolean[]{false,true})for(boolean button:new boolean[]{false,true}){
            ThemeSettings.mode(c,night?ThemeSettings.NIGHT:ThemeSettings.DAY);ClimateStripOverlay.expandButton(c,button);ClimateStripOverlay panel=new ClimateStripOverlay(wrapped,new ClimateOverlay(c));
            runOnMainSync(()->{panel.start();panel.demo();});SystemClock.sleep(600);
            try{for(int pass=0;pass<2;pass++){
                updates.set(0);java.util.List<int[]> geometry=new java.util.ArrayList<>();java.util.List<Float> progress=new java.util.ArrayList<>();java.util.concurrent.atomic.AtomicBoolean separateButtonMotion=new java.util.concurrent.atomic.AtomicBoolean();CountDownLatch done=new CountDownLatch(1);
                runOnMainSync(()->{try{toggle.invoke(panel);}catch(Exception e){throw new RuntimeException(e);}Choreographer.FrameCallback sample=new Choreographer.FrameCallback(){int frames;@Override public void doFrame(long at){try{if(af.get(panel)!=null){if(bf.get(panel)!=null)separateButtonMotion.set(true);WindowManager.LayoutParams lp=(WindowManager.LayoutParams)lf.get(panel);geometry.add(new int[]{lp.width,lp.height,lp.x,lp.y});progress.add(ff.getFloat(panel));}if(++frames<40&&(af.get(panel)!=null||pf.get(panel)!=null)){Choreographer.getInstance().postFrameCallback(this);}else done.countDown();}catch(Exception e){done.countDown();throw new RuntimeException(e);}}};Choreographer.getInstance().postFrameCallback(sample);});
                check("motion completes",done.await(3,TimeUnit.SECONDS));SystemClock.sleep(100);check("only boundary window relayouts",updates.get()<=2);check("intermediate motion frames",geometry.size()>=3);int[] first=geometry.get(0);boolean fixed=true,monotonic=true;for(int i=0;i<geometry.size();i++){fixed&=java.util.Arrays.equals(first,geometry.get(i));if(i>0)monotonic&=pass==0?progress.get(i)>=progress.get(i-1):progress.get(i)<=progress.get(i-1);}check("window geometry fixed throughout motion",fixed);check("fold progress not restarted by button",monotonic);check("button shares fold animator",!separateButtonMotion.get());check("animation window stays at bottom",first[3]==0&&first[1]==60);WindowManager.LayoutParams lp=(WindowManager.LayoutParams)lf.get(panel);check("resting touch area",lp.width==(pass==0?148:420+(button?104:0))&&lp.height==(pass==0?36:52));check("handle remains visible",((View)vf.get(panel)).getVisibility()==View.VISIBLE&&((View)vf.get(panel)).getAlpha()==1);int[] origin=new int[2];((View)vf.get(panel)).getLocationOnScreen(origin);check("resting window remains centered",Math.abs(origin[0]+lp.width/2-c.getResources().getDisplayMetrics().widthPixels/2)<=1);check("native move animation disabled",(WindowManager.LayoutParams.class.getField("privateFlags").getInt(lp)&0x40)!=0);
                System.out.println("motion night="+night+" button="+button+" fold="+(pass==0)+" frames="+geometry.size()+" windowUpdates="+updates.get());
            }}finally{runOnMainSync(panel::close);}
        }
        ClimateStripOverlay.expandButton(c,false);ClimateOverlay.enabled(c,true);I18n.language(c,"ru");ThemeSettings.mode(c,ThemeSettings.DAY);
    }

    private void miniClimateOptions()throws Exception{
        Context c=getTargetContext();I18n.language(c,"en");ThemeSettings.mode(c,ThemeSettings.DAY);ClimateStripOverlay.enabled(c,true);ClimateStripOverlay.alwaysAutoHide(c,true);ClimateStripOverlay.expandButton(c,true);ClimateStripOverlay.buttonAutoHide(c,true);ClimateOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);SourceOverlay.enabled(c,false);TrackOverlay.enabled(c,false);AudioSourceController.enabled(c,false);CarMenuOverlay.enabled(c,false);c.getSharedPreferences("overlay",0).edit().putBoolean("climateStripCollapsed",true).commit();
        android.accessibilityservice.AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getUiAutomation().setServiceInfo(info);
        stopProbe();command(new Intent().putExtra("profile",6291908).putExtra("source",2).putExtra("fan",4).putExtra("left",44).putExtra("right",46).putExtra("face",1).putExtra("feet",1).putExtra("windshield",0).putExtra("mediaState",0).putExtra("silent",false).putExtra("raw",new int[]{0}));startProbe();await("folded baseline",()->state().climateStripVisible&&state().climateStripCollapsed&&stripNode0816()!=null);receipts(true);Activity settings=startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));SystemClock.sleep(1200);await("initial callbacks settle to folded handle",()->state().climateStripCollapsed&&stripBounds0816().width()==148&&climateButtonNode()==null);check("button hidden before a change",climateButtonNode()==null);check("always hide preference present",contains(settings.findViewById(android.R.id.content),"Auto-hide compact climate after manual opening"));
        stripTap0816();await("tap expands",()->!state().climateStripCollapsed);await("manual tap shows timed full-panel button",()->climateButtonNode()!=null);await("manual opening auto folds and hides button",()->state().climateStripCollapsed&&stripBounds0816().width()==148&&climateButtonNode()==null);
        command(new Intent().putExtra("left",45));await("change shows strip and button",()->!state().climateStripCollapsed&&climateButtonNode()!=null);SystemClock.sleep(500);android.graphics.Rect compact=stripBounds0816(),button=new android.graphics.Rect();climateButtonNode().getBoundsInScreen(button);check("button and strip share overlay window",climateButtonNode().getWindowId()==stripNode0816().getWindowId());check("button outside strip to left",button.right<=compact.left-8);check("compact still screen centered",Math.abs(compact.centerX()-c.getResources().getDisplayMetrics().widthPixels/2)<=1);screenshot("mini-climate-button-day.png");check("button click",climateButtonNode().performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK));await("button opens disabled automatic popup",()->state().climateOverlayVisible);await("mini hidden below full popup",()->!state().climateStripVisible);screenshot("mini-climate-button-full.png");await("full popup closes",()->!state().climateOverlayVisible);await("mini returns after full popup",()->state().climateStripVisible&&climateButtonNode()!=null);await("same timer hides button and folds mini",()->state().climateStripCollapsed&&climateButtonNode()==null);
        ClimateStripOverlay.alwaysAutoHide(c,false);ClimateOverlay.enabled(c,false);c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY));await("visible handle before button-only timer",()->state().climateStripVisible&&stripNode0816()!=null);stripTap0816();await("independent button appears on open panel",()->!state().climateStripCollapsed&&climateButtonNode()!=null);await("button alone auto hides",()->state().climateStripVisible&&stripNode0816()!=null&&!state().climateStripCollapsed&&climateButtonNode()==null);check("panel stays centered after independent button fade",Math.abs(stripBounds0816().centerX()-c.getResources().getDisplayMetrics().widthPixels/2)<=1);stripTap0816();await("fold before persistent-button test",()->state().climateStripCollapsed&&stripBounds0816().width()==148);
        ClimateStripOverlay.alwaysAutoHide(c,false);ClimateStripOverlay.buttonAutoHide(c,false);c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY));check("no button beside folded handle",climateButtonNode()==null);stripTap0816();await("manual expanded without forced hide",()->!state().climateStripCollapsed&&climateButtonNode()!=null);SystemClock.sleep(5600);check("manual expansion persists with option off",!state().climateStripCollapsed);stripTap0816();await("fold hides persistent button",()->state().climateStripCollapsed&&climateButtonNode()==null&&stripBounds0816().width()==148);SystemClock.sleep(400);stripTap0816();await("unfold restores persistent button",()->!state().climateStripCollapsed&&climateButtonNode()!=null);SystemClock.sleep(400);ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(450);screenshot("mini-climate-button-night.png");ClimateStripOverlay.expandButton(c,false);c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY));await("button can be disabled",()->climateButtonNode()==null);await("button setting preserves strip center",()->stripNode0816()!=null&&Math.abs(stripBounds0816().centerX()-c.getResources().getDisplayMetrics().widthPixels/2)<=1);check("read-only controls send no CAN writes",receipts(false).length()==0);
        runOnMainSync(()->settings.finish());stopProbe();await("mini options cleanup",()->!state().running);c.getSharedPreferences("overlay",0).edit().putBoolean("climateStripCollapsed",false).commit();ClimateOverlay.enabled(c,true);TrackOverlay.enabled(c,true);AudioSourceController.enabled(c,true);VolumeOverlay.options(c,true,false);I18n.language(c,"ru");ThemeSettings.mode(c,ThemeSettings.DAY);
    }
    private void climateStrip0815()throws Exception{
        ProbeState model=new ProbeState();model.newSession();model.running(true);model.event(1000,new int[]{6291908},null,null);model.status("test","",true);model.event(10,new int[]{1},null,null);model.event(21,new int[]{0},null,null);check("zero fan hides strip despite raw power1",!ClimateStripOverlay.active(model.snapshot()));model.event(21,new int[]{4},null,null);model.event(10,new int[]{0},null,null);check("running fan shows strip despite raw power0",ClimateStripOverlay.active(model.snapshot()));model.event(27,new int[]{44},null,null);model.event(28,new int[]{46},null,null);
        int[] masks={1,2,6,4,5},glyphs={17,19,119,20,120};for(int i=0;i<masks.length;i++){model.event(18,new int[]{(masks[i]&1)!=0?1:0},null,null);model.event(19,new int[]{(masks[i]&2)!=0?1:0},null,null);model.event(20,new int[]{(masks[i]&4)!=0?1:0},null,null);check("shared airflow mode "+masks[i],ClimateAirflow.glyph(model.snapshot())==glyphs[i]);}
        model.event(18,new int[]{0},null,null);model.event(19,new int[]{0},null,null);model.event(20,new int[]{0},null,null);check("windshield fallback matches popup",ClimateAirflow.glyph(model.snapshot())==17);model.event(27,new int[]{0},null,null);model.event(28,new int[]{0},null,null);check("no fabricated direction without temperature",ClimateAirflow.glyph(model.snapshot())==-1);model.status("offline","",false);check("disconnected strip hidden",!ClimateStripOverlay.active(model.snapshot()));
        Context c=getTargetContext();I18n.language(c,"en");ThemeSettings.mode(c,ThemeSettings.DAY);UnknownOverlay.enabled(c,false);TrackOverlay.enabled(c,false);VolumeOverlay.options(c,false,false);SourceOverlay.enabled(c,false);CarMenuOverlay.enabled(c,false);AudioSourceController.enabled(c,false);ClimateStripOverlay.enabled(c,false);ClimateOverlay.enabled(c,true);
        stopProbe();command(new Intent().putExtra("profile",6291908).putExtra("source",2).putExtra("power",1).putExtra("fan",4).putExtra("left",44).putExtra("right",46).putExtra("ac",1).putExtra("windshield",0).putExtra("face",1).putExtra("feet",1).putExtra("silent",false).putExtra("raw",new int[]{0}));startProbe();await("strip fixture connected",()->state().connected&&state().number(21)==4);SystemClock.sleep(1000);receipts(true);Activity settings=startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));check("strip disabled by option",!state().climateStripVisible);click(settings,"Compact climate strip while climate is on");await("strip shows from setting",()->state().climateStripVisible);check("strip content ordered",state().climateStripText.equals("22.0° · airflow=6 · fan=4 · A/C=on · 23.0°"));screenshot("honda-box-v0815-strip-day-TEST.png");ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(500);await("night strip remains visible",()->state().climateStripVisible);screenshot("honda-box-v0815-strip-night-TEST.png");SystemClock.sleep(5500);check("strip does not auto hide",state().climateStripVisible);
        command(new Intent().putExtra("fan",5));await("popup covers strip",()->state().climateOverlayVisible&&!state().climateStripVisible);screenshot("honda-box-v0815-strip-covered-TEST.png");click(settings,"Compact climate strip while climate is on");click(settings,"Compact climate strip while climate is on");check("enabling during popup never covers popup",!state().climateStripVisible);await("strip returns after popup and exit animation",()->!state().climateOverlayVisible&&state().climateStripVisible);check("returned strip has changed fan",state().climateStripText.contains("fan=5"));
        ClimateOverlay.enabled(c,false);c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY));command(new Intent().putExtra("fan",0).putExtra("power",1));await("off climate hides strip",()->!state().climateStripVisible);command(new Intent().putExtra("fan",7).putExtra("power",0).putExtra("windshield",0).putExtra("face",0).putExtra("feet",0).putExtra("ac",0));await("on derived power restores strip",()->state().climateStripVisible&&state().climateStripText.contains("airflow=1")&&state().climateStripText.contains("A/C=off"));command(new Intent().putExtra("profile",999));await("unknown profile hides strip",()->!state().climateStripVisible);command(new Intent().putExtra("profile",6291908));await("supported profile restores strip",()->state().climateStripVisible);click(settings,"Compact climate strip while climate is on");await("unchecked strip removed",()->!state().climateStripVisible);check("strip sends no vehicle commands",receipts(false).length()==0);runOnMainSync(()->settings.finish());stopProbe();await("strip stop cleanup",()->!state().running&&!state().climateStripVisible);I18n.language(c,"ru");ThemeSettings.mode(c,ThemeSettings.DAY);AudioSourceController.enabled(c,true);TrackOverlay.enabled(c,true);VolumeOverlay.options(c,true,false);
    }

    private void logReset0814()throws Exception{
        ProbeState model=new ProbeState();model.newSession();model.running(true);model.event(1000,new int[]{6291908},null,null);model.event(98,new int[]{2},null,null);model.status("test","",true);model.event(1019,packet(5,2,0x30,1,1,0,0x59,1,0x38),null,null);model.event(1019,mediaPacket(7,"Keep CD title"),null,null);model.event(1019,packet(17,1,0),null,null);model.mark("old marker");model.markExperiment("ENGINE_IDLE");model.audioRoutes.start();model.audioRoutes.event(0,0,new int[]{13},null,null);long session=model.snapshot().sessionId,count=model.snapshot().count,sourceSeq=model.snapshot().sourceSequence;
        JSONObject old=new JSONObject(model.exportJson());check("old raw capture exists",old.getJSONObject("rawBsj").getJSONArray("chunks").length()==3);long oldCapture=old.getJSONObject("capture").getLong("id");model.clearLog();JSONObject cleared=new JSONObject(model.exportJson());
        check("reset retains live connection and session",model.snapshot().running&&model.snapshot().connected&&model.snapshot().sessionId==session);check("reset preserves sequence freshness",model.snapshot().count==count&&model.snapshot().sourceSequence==sourceSeq);check("reset preserves CD presentation",model.snapshot().cdNumber(101).equals("59")&&model.snapshot().cdTime().equals("01:38")&&model.snapshot().combinedMediaText().equals("Keep CD title"));
        check("raw history and counters empty",cleared.getJSONObject("rawBsj").getJSONArray("chunks").length()==0&&cleared.getJSONObject("rawBsj").getLong("receivedChunks")==0);check("parsed histories and experiment markers cleared",cleared.getJSONArray("parsedBsjFrames").length()==0&&cleared.getJSONArray("experimentMarkers").length()==0&&cleared.getJSONArray("experimentWindows").length()==0);check("diagnostic entries cleared",cleared.getJSONArray("unidentifiedBsj").length()==0);check("markers and commands cleared",cleared.getJSONArray("markers").length()==0&&cleared.getJSONArray("commands").length()==0);check("log contains only reset marker",cleared.getJSONArray("log").length()==1&&cleared.getJSONArray("log").getString(0).contains("LOG RESET"));check("audio route history cleared while channel retained",cleared.getJSONObject("audioRoutes").getJSONArray("events").length()==0&&model.audioRoutes.mainChannel()==13);check("new recording boundary exported",cleared.getJSONObject("capture").getLong("id")==oldCapture+1);
        model.event(1019,packet(17,2,0),null,null);JSONObject after=new JSONObject(model.exportJson());check("new packets immediately recorded",after.getJSONObject("rawBsj").getJSONArray("chunks").length()==1&&after.getJSONArray("parsedBsjFrames").length()==1&&after.getJSONArray("parsedBsjFrames").getJSONObject(0).getInt("type")==17);model.clearLog();check("multiple resets work",new JSONObject(model.exportJson()).getJSONObject("capture").getLong("id")==oldCapture+2);
        Context c=getTargetContext();I18n.language(c,"en");UnknownOverlay.enabled(c,false);AudioSourceController.enabled(c,false);stopProbe();command(new Intent().putExtra("profile",6291908).putExtra("source",2).putExtra("volume",7).putExtra("silent",false).putExtra("raw",packet(17,1,0)));startProbe();await("reset service connected",()->state().connected&&state().rawCount>0);receipts(true);Activity settings=startActivitySync(new Intent(c,DebugActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));long liveSession=state().sessionId;command(new Intent().putExtra("silent",true).putExtra("raw",new int[]{0}));SystemClock.sleep(500);click(settings,"Reset log");check("real reset button preserves service",state().running&&state().connected&&state().sessionId==liveSession);check("real reset button clears raw history",new JSONObject(ProbeState.INSTANCE.exportJson()).getJSONObject("rawBsj").getJSONArray("chunks").length()==0);
        command(new Intent().putExtra("silent",false).putExtra("raw",packet(17,2,0)));await("recording continues after reset",()->state().rawCount>0&&ProbeState.INSTANCE.analyzer.snapshot().frames.size()>0);check("reset never sends audio commands",receipts(false).length()==0);runOnMainSync(()->settings.finish());stopProbe();await("reset test cleanup",()->!state().running);I18n.language(c,"ru");AudioSourceController.enabled(c,true);
    }

    private boolean unknownWindow0813(){try(android.os.ParcelFileDescriptor fd=getUiAutomation().executeShellCommand("dumpsys window windows")){java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] bytes=new byte[8192];for(int n;(n=in.read(bytes))!=-1;)out.write(bytes,0,n);return out.toString("UTF-8").contains("Honda Box Unknown BSJ");}catch(Exception e){throw new RuntimeException(e);}}
    private void unknown0813()throws Exception{
        UnknownMessages m=new UnknownMessages();check("unknown 0C classified",UnknownMessages.reason(12,new int[17]).equals("Unknown type"));check("stock door status excluded",UnknownMessages.reason(17,new int[]{1,0})==null);check("known variable-length text excluded",UnknownMessages.reason(7,new int[]{0,65})==null);int[] knownText=new int[31];knownText[0]=8;check("known TITLE subtype excluded",UnknownMessages.reason(7,knownText)==null);int[] knownMenu=new int[49];knownMenu[0]=32;check("routine menu excluded",UnknownMessages.reason(9,knownMenu)==null);
        m.accept(0x45,new int[]{1,0},100);long rev=m.revision();m.accept(0x45,new int[]{1,0},200);check("repeats counted without new trigger",m.revision()==rev&&m.latest().get(0).repeats==2);m.accept(0x45,new int[]{2,0},300);check("changed payload triggers",m.revision()>rev&&m.latest().get(0).repeats==1);rev=m.revision();m.accept(0x45,new int[]{2,0},8400);check("same payload after idle triggers",m.revision()>rev);
        m.accept(5,new int[]{1,0,1,1,0,1,0,1},8500);rev=m.revision();m.accept(5,new int[]{1,0,1,1,0,1,0,2},8600);check("known elapsed time does not trigger diagnostic",m.revision()==rev);m.accept(5,new int[]{1,0,0,1,0,1,0,2},8700);check("media service fields excluded",m.revision()==rev);check("unknown raw frame exported",m.json().getJSONObject(0).getString("fullHex").startsWith("2E 45 02"));
        for(int t=32;t<90;t++)m.accept(t,new int[]{t},9000);check("diagnostics bounded",m.latest().size()==32);m.clear();check("session clears entries",m.latest().isEmpty());
        Context c=getTargetContext();I18n.language(c,"en");AudioSourceController.enabled(c,false);VolumeOverlay.options(c,false,false);SourceOverlay.enabled(c,false);ClimateOverlay.enabled(c,false);CarMenuOverlay.enabled(c,false);UnknownOverlay.enabled(c,false);
        stopProbe();command(new Intent().putExtra("profile",6291908).putExtra("source",2).putExtra("silent",false).putExtra("raw",new int[]{0}));startProbe();await("diagnostic service connected",()->state().connected);receipts(true);
        Activity settings=startActivitySync(new Intent(c,DebugActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));check("diagnostic setting present",contains(settings.findViewById(android.R.id.content),"Unknown BSJ messages"));raw(packet(0x45,1,0));SystemClock.sleep(400);check("disabled diagnostic window absent",!unknownWindow0813());
        UnknownOverlay.enabled(c,true);c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY));SystemClock.sleep(400);check("enabling does not replay old data",!unknownWindow0813());raw(packet(12,new int[17]));await("new unknown window visible",()->unknownWindow0813());check("unknown data reaches diagnostics",ProbeState.INSTANCE.unknownMessages.latest().get(0).type==12);
        screenshot("honda-box-v0813-unknown-TEST.png");await("diagnostic popup autohides",()->!unknownWindow0813());raw(packet(0x45,2,0));await("changed raw packet reopens window",()->unknownWindow0813());UnknownOverlay.enabled(c,false);c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.OVERLAY));await("switch off removes diagnostic window",()->!unknownWindow0813());check("no audio writes from diagnostics",receipts(false).length()==0);runOnMainSync(()->settings.finish());stopProbe();await("diagnostics cleanup",()->!state().running);I18n.language(c,"ru");AudioSourceController.enabled(c,true);VolumeOverlay.options(c,true,false);
    }

    private boolean trackWindow088(){try(android.os.ParcelFileDescriptor fd=getUiAutomation().executeShellCommand("dumpsys window windows")){java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] bytes=new byte[8192];for(int n;(n=in.read(bytes))!=-1;)out.write(bytes,0,n);return out.toString("UTF-8").contains("Honda Box New Track");}catch(Exception e){throw new RuntimeException(e);}}
    private void trackDemo088()throws Exception{
        Context c=getTargetContext();I18n.language(c,"ru");ThemeSettings.mode(c,ThemeSettings.DAY);VolumeOverlay.options(c,true,true);AudioSourceController.enabled(c,false);TrackOverlay.enabled(c,false);startProbe();await("service running",()->state().running);receipts(true);
        android.accessibilityservice.AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getUiAutomation().setServiceInfo(info);
        Activity settings=startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));check("demo button present",button(settings.findViewById(android.R.id.content),"Тест нового трека · 4 секунды")!=null);
        ProbeState.INSTANCE.status("test disconnected","",false);click(settings,"Тест нового трека · 4 секунды");await("demo visible above settings without CAN",()->trackWindow088());screenshot("honda-box-v089-track-demo-day-TEST.png");await("demo automatically hides",()->!trackWindow088());ThemeSettings.mode(c,ThemeSettings.NIGHT);SystemClock.sleep(600);click(settings,"Тест нового трека · 4 секунды");await("night demo appears",()->trackWindow088());screenshot("honda-box-v089-track-demo-night-TEST.png");await("night demo hides",()->!trackWindow088());ThemeSettings.mode(c,ThemeSettings.DAY);check("demo leaves normal overlay preference unchanged",!TrackOverlay.enabled(c));check("demo sends no vehicle commands",receipts(false).length()==0);runOnMainSync(()->settings.finish());TrackOverlay.enabled(c,true);VolumeOverlay.options(c,true,false);stopProbe();await("demo cleanup",()->!state().running);
    }

    @Override public void onStart(){Bundle result=new Bundle();try{
        I18n.language(getTargetContext(),"ru");SourceSettings.aux(getTargetContext(),3);DiagnosticSettings.enabled(getTargetContext(),!"cleanup".equals(phase));
        if("cleanup".equals(phase)){ClimateStripOverlay.enabled(getTargetContext(),true);ClimateOverlay.enabled(getTargetContext(),true);ClimateStripOverlay.alwaysAutoHide(getTargetContext(),false);ClimateStripOverlay.expandButton(getTargetContext(),false);ClimateStripOverlay.buttonAutoHide(getTargetContext(),false);BackgroundSettings.autoStart(getTargetContext(),true);stopProbe();await("cleanup stopped",()->!state().running&&!state().connected);SystemClock.sleep(500);ProbeState.INSTANCE.radioPresets.clear();getTargetContext().getSharedPreferences("radioPresets",0).edit().clear().commit();SourceSettings.aux(getTargetContext(),-1);ThemeSettings.mode(getTargetContext(),ThemeSettings.AUTO);I18n.language(getTargetContext(),"system");result.putString("result","PASS: emulator synthetic preferences cleared");finish(Activity.RESULT_OK,result);return;}
        if("cd-discs".equals(phase)){cdDiscTests();result.putString("result","PASS: "+checks+" checks (CD disc selection)");finish(Activity.RESULT_OK,result);return;}
        if("menu-list".equals(phase)){menuListTests();result.putString("result","PASS: "+checks+" checks (menu-list)");finish(Activity.RESULT_OK,result);return;}
        if("track-demo088".equals(phase)){trackDemo088();result.putString("result","PASS: "+checks+" track demo checks");finish(Activity.RESULT_OK,result);return;}
        if("icons0818".equals(phase)){checks+=new IconPreview0818Tests().run(getTargetContext());result.putString("result","PASS: "+checks+" icon rendering checks");finish(Activity.RESULT_OK,result);return;}
        if("ipod-model".equals(phase)){checks+=new MarkerModel0818Tests().run();result.putString("result","PASS: "+checks+" USB/iPod model checks");finish(Activity.RESULT_OK,result);return;}
        if("release0818".equals(phase)){checks+=new MarkerModel0818Tests().run();releaseUi0818();result.putString("result","PASS: "+checks+" release0818 checks");finish(Activity.RESULT_OK,result);return;}
        if("clean0810".equals(phase)){checks+=new CleanMedia0810Tests().run(getContext().getAssets());cleanMediaUi0810();result.putString("result","PASS: "+checks+" clean CD/iPod checks");finish(Activity.RESULT_OK,result);return;}
        if("strip-motion".equals(phase)){stripMotion();result.putString("result","PASS: "+checks+" compact climate motion checks");finish(Activity.RESULT_OK,result);return;}
        if("fold-strip0816".equals(phase)){foldStrip0816();result.putString("result","PASS: "+checks+" folding climate strip checks");finish(Activity.RESULT_OK,result);return;}
        if("mini-climate-options".equals(phase)){miniClimateOptions();result.putString("result","PASS: "+checks+" mini-climate options checks");finish(Activity.RESULT_OK,result);return;}
        if("climate-strip0815".equals(phase)){climateStrip0815();result.putString("result","PASS: "+checks+" climate strip checks");finish(Activity.RESULT_OK,result);return;}
        if("log-reset0814".equals(phase)){logReset0814();result.putString("result","PASS: "+checks+" log reset checks");finish(Activity.RESULT_OK,result);return;}
        if("unknown0813".equals(phase)){unknown0813();result.putString("result","PASS: "+checks+" unknown BSJ checks");finish(Activity.RESULT_OK,result);return;}
        if("cd-volume0812".equals(phase)){checks+=new CdVolume0812Tests().run();cdVolumeUi0812();result.putString("result","PASS: "+checks+" CD-volume checks");finish(Activity.RESULT_OK,result);return;}
        if("manual0811".equals(phase)){manualMedia0811();result.putString("result","PASS: "+checks+" manual USB/iPod display checks");finish(Activity.RESULT_OK,result);return;}
        if("menus087".equals(phase)){checks+=new Menu087Tests().run();result.putString("result","PASS: "+checks+" source-specific menu checks");finish(Activity.RESULT_OK,result);return;}
        if("revision-ui086".equals(phase)){revisionUi();launcher087();result.putString("result","PASS: "+checks+" revision UI checks");finish(Activity.RESULT_OK,result);return;}
        if("revision086".equals(phase)){checks+=new RevisionTests().run(getContext().getAssets());result.putString("result","PASS: "+checks+" revision086 checks");finish(Activity.RESULT_OK,result);return;}
        if("playback-memory".equals(phase)){playbackMemoryTests();result.putString("result","PASS: "+checks+" checks (playback-memory)");finish(Activity.RESULT_OK,result);return;}
        if("shared-media".equals(phase)){sharedMediaTests();result.putString("result","PASS: "+checks+" checks (shared-media)");finish(Activity.RESULT_OK,result);return;}
        if("background".equals(phase)){backgroundTests();result.putString("result","PASS: "+checks+" checks (boot, manual pause, wake subscription restoration)");finish(Activity.RESULT_OK,result);return;}
        if("prepare-boot".equals(phase)){SourceSettings.aux(getTargetContext(),-1);AudioSourceController.enabled(getTargetContext(),true);BackgroundSettings.autoStart(getTargetContext(),true);BackgroundSettings.activate(getTargetContext());getTargetContext().getSharedPreferences("background",0).edit().commit();result.putString("result","PASS: boot preferences prepared");finish(Activity.RESULT_OK,result);return;}
        if("home-language".equals(phase)){homeLanguage();result.putString("result","PASS: "+checks+" checks (home source selection, settings navigation, live language changes)");finish(Activity.RESULT_OK,result);return;}
        if("v072-fixes".equals(phase)){v072Fixes();result.putString("result","PASS: "+checks+" checks (route ordering, received presets, RAW quality)");finish(Activity.RESULT_OK,result);return;}
        if("aux-fixes".equals(phase)){auxFixes();result.putString("result","PASS: "+checks+" checks (AUX default, migration, override and MODE routing)");finish(Activity.RESULT_OK,result);return;}
        if("box-fixes".equals(phase)){boxFixes();result.putString("result","PASS: "+checks+" checks (Honda Box log, launcher, player and source fixes)");finish(Activity.RESULT_OK,result);return;}
        if("device-fixes".equals(phase)){deviceFixes();result.putString("result","PASS: "+checks+" checks (device log regressions, text, source and AUX calibration)");finish(Activity.RESULT_OK,result);return;}
        if("analyzer-model".equals(phase)){checks+=new AnalyzerTests().run(getContext().getAssets());result.putString("result","PASS: "+checks+" checks (BSJ analyzer models and recorded log)");finish(Activity.RESULT_OK,result);return;}
        if ("raw-limits".equals(phase)) { rawLimits();result.putString("result","PASS: "+checks+" checks (raw bounds)");finish(Activity.RESULT_OK,result);return; }
        if ("replay".equals(phase)) { replay(); result.putString("result","PASS: "+checks+" checks (recorded session replay)"); finish(Activity.RESULT_OK,result); return; }
        if(phase.startsWith("menus"))CarMenuOverlay.enabled(getTargetContext(),false);
        if(phase.startsWith("windows"))ClimateOverlay.enabled(getTargetContext(),false);
        Activity activity=startActivitySync(new Intent(getTargetContext(),SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK));
        startProbe();
        if("missing".equals(phase)){
            await("missing service diagnosed",()->state().status.equals("Служба FYT не найдена"));
            check("no fake zero volume",state().volume().equals("—"));check("no fake temperature",state().temperature(27).equals("—"));
            check("export is JSON",new JSONObject(ProbeState.INSTANCE.exportJson()).getBoolean("connected")==false);
            stopProbe();await("stopped",()->!state().running&&!state().connected);
        }else{
            await("real cross-process callbacks",()->state().honda() && state().number(132)!=null && state().values.containsKey(135) && state().values.containsKey(115));
            check("source/band",state().source().equals("FM2"));check("temperature half-degree",state().temperature(27).equals("22.0°")&&state().temperature(28).equals("23.0°"));
            check("volume",state().volume().equals("18"));check("string callback",state().text(135).equals("TEST MENU"));check("missing value stays absent",state().number(112)==null);
            check("full climate flags",state().climate().contains("A/C: вкл")&&state().climate().contains("AUTO: вкл")&&state().climate().contains("Лицо: вкл"));
            check("expanded range subscribed",state().number(96)!=null&&state().number(96)==321);
            check("media fields visible",state().media().contains("02:21")&&state().media().contains("Трек [101]: 9"));
            if(phase.startsWith("menus")){menuTests(activity);result.putString("result","PASS: "+checks+" checks ("+phase+")");finish(Activity.RESULT_OK,result);return;}
            if("analyzer-ui".equals(phase)){analyzerUiTests();result.putString("result","PASS: "+checks+" checks (read-only BSJ analyzer UI)");finish(Activity.RESULT_OK,result);return;}
            if("images".equals(phase)){climateImageTests();result.putString("result","PASS: "+checks+" checks (final climate images)");finish(Activity.RESULT_OK,result);return;}
            if("sources".equals(phase)){sourcePopupTests();result.putString("result","PASS: "+checks+" checks (source autohide and climate sizing)");finish(Activity.RESULT_OK,result);return;}
            if("routes".equals(phase)){routeTests();result.putString("result","PASS: "+checks+" checks (audio route diagnostics)");finish(Activity.RESULT_OK,result);return;}
            if("hardware".equals(phase)){hardwareTests();result.putString("result","PASS: "+checks+" checks (hardware log and raw source)");finish(Activity.RESULT_OK,result);return;}
            if("animation".equals(phase)){animationTests();result.putString("result","PASS: "+checks+" checks (animation)");finish(Activity.RESULT_OK,result);return;}
            if(phase.startsWith("players")){
                playerTests(activity);result.putString("result","PASS: "+checks+" checks ("+phase+")");finish(Activity.RESULT_OK,result);return;
            }
            if("windows".equals(phase)||"windows-denied".equals(phase)){
                windowTests(activity);result.putString("result","PASS: "+checks+" checks ("+phase+")");finish(Activity.RESULT_OK,result);return;
            }
            if("themes".equals(phase)){
                themeTests(activity);result.putString("result","PASS: "+checks+" checks (themes, panel, source and volume overlays)");finish(Activity.RESULT_OK,result);return;
            }
            if(phase.startsWith("overlay")){
                overlayTests(activity);stopProbe();await("overlay cleanup",()->!state().running&&!state().connected);
                result.putString("result","PASS: "+checks+" checks ("+phase+")");finish(Activity.RESULT_OK,result);return;
            }
            if("ui".equals(phase)){
                SystemClock.sleep(600);uiTest(activity);stopProbe();await("UI cleanup",()->!state().running&&!state().connected);
                result.putString("result","PASS: "+checks+" checks (UI button + screenshot)");finish(Activity.RESULT_OK,result);return;
            }
            if("audio".equals(phase)){
                check("connection sends no commands",receipts(false).length()==0);audioTests();stopProbe();await("audio cleanup",()->!state().running&&!state().connected);
                result.putString("result","PASS: "+checks+" checks (audio IPC)");finish(Activity.RESULT_OK,result);return;
            }
            runOnMainSync(()->activity.moveTaskToBack(true));
            command(new Intent().putExtra("source",2).putExtra("volume",21));await("background updates",()->state().source().equals("CD")&&state().volume().equals("21"));
            command(new Intent().putExtra("source",3));await("AUX kept uncertain",()->state().source().equals("AUX"));
            command(new Intent().putExtra("profile",999));await("unknown profile raw",()->state().source().equals("Код 3")&&state().temperature(27).equals("raw 44"));
            command(new Intent().putExtra("profile",6291908).putExtra("left",-2));await("LOW marker",()->state().temperature(27).equals("LOW"));
            command(new Intent().putExtra("nullTemp",true));await("null payload",()->state().temperature(27).equals("—"));
            command(new Intent().putExtra("nullTemp",false).putExtra("left",44));await("valid value restored",()->state().temperature(27).equals("22.0°"));
            JSONObject report=new JSONObject(ProbeState.INSTANCE.exportJson());check("raw export",report.getJSONObject("lastValues").getJSONObject("98").getJSONArray("ints").getInt(0)==3);
            stopProbe();await("stop clears live status",()->!state().running&&!state().connected);long count=state().count;
            command(new Intent().putExtra("source",4));SystemClock.sleep(600);check("no callbacks after stop",state().count==count);check("stale UI hidden",state().source().equals("—"));
            command(new Intent().putExtra("silent",true));SystemClock.sleep(300);startProbe();
            await("accepted bind is not data",()->state().status.equals("Подключено, данных нет")&&state().count==0);
            command(new Intent().putExtra("silent",false).putExtra("source",4));await("recover callbacks",()->state().source().equals("USB/iPod"));
            command(new Intent().putExtra("kill",true));await("binder death logged",()->state().log.contains("Связь прервана")||state().log.contains("CAN-модуль отключился"));
            await("automatic rebind",()->state().connected&&state().source().equals("FM2"));
            stopProbe();await("cleanup",()->!state().running&&!state().connected);
        }
        result.putString("result","PASS: "+checks+" checks ("+phase+")");finish(Activity.RESULT_OK,result);
    }catch(Throwable error){result.putString("result","FAIL: "+error);result.putString("debug", "profile="+state().number(1000)+" menu="+state().menu()+" overlay="+state().carMenuOverlayStatus+" frames="+state().bsjFramesAccepted+" visible="+state().carMenuOverlayVisible);result.putString("climateDebug","strip="+state().climateStripVisible+" folded="+state().climateStripCollapsed+" text="+state().climateStripText+" power="+state().climatePower()+" popup="+state().climateOverlayStatus+" forcedHide="+ClimateStripOverlay.alwaysAutoHide(getTargetContext()));result.putString("commandStatus",state().commandStatus);result.putString("state",state().log);stopProbe();finish(Activity.RESULT_CANCELED,result);}}
}
