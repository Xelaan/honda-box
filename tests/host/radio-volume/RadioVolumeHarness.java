package dev.alex.fyt.probe;
/** Tests FM/AM display state and station announcements around OEM volume blanks. */
public final class RadioVolumeHarness {
 private static int checks;
 private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 private static void frame(ProbeState s,int type,int... p){IpodMarkerStateHarness.frame(s,type,p);}
 public static void main(String[] args){
  for(int band=0;band<=5;band++)for(int stereo=0;stereo<=1;stereo++){
   ProbeState s=IpodMarkerStateHarness.state(1,band);int f=band<=2?10000:98;
   frame(s,4,band,f>>8,f&255,3,(stereo<<7)|1);
   TrackChange changes=new TrackChange();changes.update(s.overlaySnapshot(),0,false);
   frame(s,3,0x80,12);long epoch=s.overlaySnapshot().sourceSequence;
   for(int[] blank:new int[][]{{0,0,0,0,0},{band,0,0,3,0},{255,255,255,255,255}}){
    frame(s,4,blank);ProbeState.Snapshot a=s.overlaySnapshot();
    check(a.displayNumber(119)==f,"frequency retained");check(a.displayNumber(115)==band,"band retained");check(a.displayNumber(116)==3,"preset highlight retained");
    check(a.displayNumber(118)==stereo&&a.displayNumber(117)==1,"ST/Normal retained");check(a.preset(3)==f,"cached preset retained");check(a.sourceSequence==epoch,"blank does not announce band change");
    check(changes.update(a,2000,false)==null,"blank does not announce station change");
   }
   // FYT logical zeros cannot replace the retained validated raw state.
   for(int code:new int[]{115,116,117,118,119,122})s.event(code,new int[]{0},null,null);
   ProbeState.Snapshot a=s.overlaySnapshot();check(a.displayNumber(119)==f&&a.displayNumber(116)==3&&a.displayNumber(118)==stereo&&a.preset(3)==f,"logical zero callbacks retain raw station/status/cache");
   int next=f+10;frame(s,4,band,next>>8,next&255,4,(stereo<<7)|1);a=s.overlaySnapshot();
   check(a.displayNumber(119)==next&&a.displayNumber(116)==4,"real station/preset change accepted");changes.update(a,2100,false);check(changes.update(a,2800,false)!=null,"real station change announces");
   frame(s,4,band,next>>8,next&255,0,(stereo<<7)|4);a=s.overlaySnapshot();check(a.displayNumber(116)==0&&a.displayNumber(117)==4,"manual tuning still removes preset highlight");
   frame(s,3,0x81,12);check(AudioMode.reported(s.overlaySnapshot())==AudioMode.CD&&!s.overlaySnapshot().fresh(119),"real source change invalidates radio");
  }
  System.out.println("PASS: "+checks+" radio volume checks across FM/FM1/FM2/AM/AM1/AM2");
 }
}
