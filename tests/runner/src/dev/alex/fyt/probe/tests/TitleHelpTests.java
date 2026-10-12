package dev.alex.fyt.probe.tests;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;
import dev.alex.fyt.probe.*;
/** Real player views and physical popup taps; in-memory data only. */
public final class TitleHelpTests extends Instrumentation {
 private final ProbeState state=ProbeState.INSTANCE;private Activity activity;private int checks;
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 private Object field(String name)throws Exception{java.lang.reflect.Field f=AudioPlayerActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
 private void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 private void touch(View target){int[] p=new int[2];runOnMainSync(()->target.getLocationOnScreen(p));long now=SystemClock.uptimeMillis();float x=p[0]+target.getWidth()/2f,y=p[1]+target.getHeight()/2f;
  for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent e=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(e,true),"physical tap delivered");e.recycle();}waitForIdleSync();SystemClock.sleep(100);
 }
 private void image(Context c,String name)throws Exception{waitForIdleSync();SystemClock.sleep(500);android.graphics.Bitmap b=getUiAutomation().takeScreenshot();try(java.io.OutputStream out=new java.io.FileOutputStream(new java.io.File(c.getExternalFilesDir(null),name))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
 private void close(){if(activity!=null){runOnMainSync(()->activity.finish());waitForIdleSync();activity=null;}SystemClock.sleep(250);}
 private void setup(int source){state.newSession();state.running(true);state.status("test","local playback data",true);state.event(1000,new int[]{6291908},null,null);state.event(98,new int[]{source},null,null);}
 private void stableDots(ImageButton info,TextView label){int x=info.getLeft();java.util.HashSet<String> phases=new java.util.HashSet<>();long end=SystemClock.uptimeMillis()+2200;while(SystemClock.uptimeMillis()<end){waitForIdleSync();phases.add(label.getText().toString());check(info.getLeft()==x,"TITLE dots never move the info button");SystemClock.sleep(100);}check(phases.size()==3,"all three real TITLE animation phases observed");}
 @Override public void onStart(){Context c=getTargetContext();String theme=ThemeSettings.mode(c),language=I18n.language(c);Bundle result=new Bundle();int outcome=Activity.RESULT_OK;
  try{c.stopService(new Intent(c,ProbeService.class));SystemClock.sleep(300);
   for(String appearance:new String[]{ThemeSettings.DAY,ThemeSettings.NIGHT})for(String lang:new String[]{"en","ru"})for(AudioMode mode:new AudioMode[]{AudioMode.CD,AudioMode.USB}){
    ThemeSettings.mode(c,appearance);I18n.language(c,lang);setup(mode.source);activity=startActivitySync(AudioPlayerActivity.intent(c,mode,false));waitForIdleSync();SystemClock.sleep(250);
    ImageButton info=(ImageButton)field("titleInfo");TextView label=(TextView)field("titleField");check(info.isShown(),"info visible on "+mode);check(info.getContentDescription().toString().equals(I18n.t(R.string.player_title_help_description)),"localized accessible label");
    check(info.getLeft()>=label.getRight()&&info.getLeft()-label.getRight()<12*c.getResources().getDisplayMetrics().density,"info follows TITLE label");if(lang.equals("en"))stableDots(info,label);if(lang.equals("en")&&mode==AudioMode.USB)image(c,"title-info-"+appearance+"-TEST.png");touch(info);AlertDialog dialog=(AlertDialog)field("titleHelp");if(dialog==null||!dialog.isShowing())image(c,"title-help-failed-TEST.png");check(dialog!=null&&dialog.isShowing(),"tip opens: "+mode+" "+appearance+" "+lang+" actual="+field("mode")+" guide="+field("playerHelp"));TextView message=dialog.findViewById(android.R.id.message);check(message.getText().toString().equals(I18n.t(mode==AudioMode.CD?R.string.player_title_help_cd:R.string.player_title_help_usb)),"source-specific localized tip");
    if(lang.equals("en")&&mode==AudioMode.USB)image(c,"title-help-"+appearance+"-TEST.png");touch(dialog.getButton(AlertDialog.BUTTON_POSITIVE));check(!dialog.isShowing(),"close button dismisses tip");check(!state.snapshot().commandBusy,"help sends no command");close();
   }
   setup(1);activity=startActivitySync(AudioPlayerActivity.intent(c,AudioMode.RADIO,false));waitForIdleSync();check(!((View)field("titleInfo")).isShown(),"radio has no TITLE help");close();
   setup(2);int[] marker={0x2e,6,3,0x32,0x22,0x22,0x80};state.event(1019,marker,null,null);check(state.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.IPOD,"local marker identifies iPod");activity=startActivitySync(AudioPlayerActivity.intent(c,AudioMode.USB,false));waitForIdleSync();SystemClock.sleep(250);check(((View)field("titleInfo")).isShown(),"iPod has help icon");check(((TextView)field("titleField")).getText().toString().equals("TITLE"),"iPod keeps plain TITLE");touch((View)field("titleInfo"));AlertDialog ipodTip=(AlertDialog)field("titleHelp");check(ipodTip.isShowing()&&((TextView)ipodTip.findViewById(android.R.id.message)).getText().toString().equals(I18n.t(R.string.player_title_help_ipod)),"iPod-specific TITLE tip");touch(ipodTip.getButton(AlertDialog.BUTTON_POSITIVE));check(!ipodTip.isShowing(),"iPod tip closes");state.event(1019,new int[]{46,7,5,5,102,105,108,101,78},null,null);SystemClock.sleep(350);check(Integer.valueOf(5).equals(state.overlaySnapshot().currentTitleField()),"file-name TITLE field is received for iPod");check(((TextView)field("titleField")).getText().toString().equals("TITLE"),"file-name field never labels iPod header");
   result.putString("result","PASS: "+checks+" TITLE help UI checks; local data only");
  }catch(Throwable e){outcome=1;result.putString("failure",android.util.Log.getStackTraceString(e));}
  finally{close();state.newSession();state.running(false);I18n.language(c,language);ThemeSettings.mode(c,theme);}
  finish(outcome,result);
 }
}
