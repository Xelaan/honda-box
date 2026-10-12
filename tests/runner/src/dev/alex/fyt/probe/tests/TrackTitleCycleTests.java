package dev.alex.fyt.probe.tests;
import android.app.*;import android.os.*;import dev.alex.fyt.probe.*;
/** Actual BSJ parser/model and track detector; no vehicle connection or writes. */
public final class TrackTitleCycleTests extends Instrumentation {
 private int checks;
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 private void expect(String label,String actual,String wanted){checks++;if(!java.util.Objects.equals(actual,wanted))throw new AssertionError(label+": "+actual+" != "+wanted);}
 private void frame(ProbeState s,int type,int... payload){int[] a=new int[payload.length+4];a[0]=46;a[1]=type;a[2]=payload.length;int sum=type+payload.length;for(int i=0;i<payload.length;i++){a[i+3]=payload[i];sum+=payload[i];}a[a.length-1]=255-(sum&255);s.event(1019,a,null,null);}
 private void text(ProbeState s,int kind,String value){int[] a=new int[value.length()+1];a[0]=kind;for(int i=0;i<value.length();i++)a[i+1]=value.charAt(i);frame(s,7,a);}
 private ProbeState state(int source){ProbeState s=new ProbeState();s.newSession();s.running(true);s.status("test","local BSJ",true);s.event(1000,new int[]{6291908},null,null);s.event(98,new int[]{source},null,null);return s;}
 @Override public void onStart(){Bundle result=new Bundle();int outcome=Activity.RESULT_OK;String lang=I18n.language(getTargetContext());
  try{I18n.language(getTargetContext(),"en");ProbeState s=state(2);frame(s,6,0x32,0x22,0x22);if(s.overlaySnapshot().mediaDevice!=ProbeState.MediaDevice.IPOD)throw new AssertionError("iPod fixture");TrackChange d=new TrackChange();text(s,5,"Song");expect("generic iPod baseline",d.update(s.overlaySnapshot(),0,false),null);
   for(int i=0;i<4;i++){text(s,5,new String[]{"Artist","Album","Song","Artist"}[i]);expect("generic iPod metadata queues no track",d.update(s.overlaySnapshot(),1000+i*2000,false),null);expect("generic iPod metadata settles quietly",d.update(s.overlaySnapshot(),2500+i*2000,false),null);}
   text(s,7,"Actual song");expect("specific field baseline",d.update(s.overlaySnapshot(),10000,false),null);text(s,8,"Artist");expect("artist field quiet",d.update(s.overlaySnapshot(),11000,false),null);text(s,6,"Album");expect("album field quiet",d.update(s.overlaySnapshot(),12000,false),null);text(s,7,"Actual song");expect("TITLE returns quietly",d.update(s.overlaySnapshot(),13000,false),null);
   text(s,7,"Next song");expect("new title pending",d.update(s.overlaySnapshot(),14000,false),null);text(s,8,"Next artist");expect("pending title does not announce artist",d.update(s.overlaySnapshot(),15300,false),null);text(s,7,"Next song");expect("correct pending title announced",d.update(s.overlaySnapshot(),15500,false),"Next song");expect("no title duplicate",d.update(s.overlaySnapshot(),17500,false),null);
   for(int source:new int[]{2,4}){s=state(source);d=new TrackChange();int disc=source==2?1:0,family=source==2?1:2;frame(s,5,disc,0,1,family,0,1,0,1);text(s,7,"Original");expect("counter initial baseline",d.update(s.overlaySnapshot(),0,false),null);text(s,7,"Alternate text");for(int i=0;i<4;i++){frame(s,5,disc,0,1,family,0,1,0,2+i);expect("same active counter keeps text quiet",d.update(s.overlaySnapshot(),3000+i*1500,false),null);}frame(s,5,disc,0,1,family,0,2,0,0);expect("new track number pending",d.update(s.overlaySnapshot(),10000,false),null);text(s,7,"Actual new song");expect("new track number still announced",d.update(s.overlaySnapshot(),10700,false),"Actual new song");}
   s=state(4);d=new TrackChange();text(s,4,"Folder one");expect("folder baseline",d.update(s.overlaySnapshot(),0,false),null);text(s,4,"Folder two");expect("folder does not identify next track",d.update(s.overlaySnapshot(),2000,false),null);expect("folder stays quiet",d.update(s.overlaySnapshot(),3500,false),null);text(s,5,"first.mp3");expect("USB file baseline",d.update(s.overlaySnapshot(),4000,false),null);text(s,5,"second.mp3");expect("USB file change pending",d.update(s.overlaySnapshot(),5000,false),null);expect("USB file change announces",d.update(s.overlaySnapshot(),6300,false),"second.mp3");
   result.putString("result","PASS: "+checks+" actual parser/model track detection checks");
  }catch(Throwable e){outcome=1;result.putString("failure",android.util.Log.getStackTraceString(e));}finally{I18n.language(getTargetContext(),lang);}
  finish(outcome,result);
 }
}
