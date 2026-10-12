package dev.alex.fyt.probe;
/** Validated command03 bit7 transitions, independent of source/menu/volume and session state. */
public final class AudioPowerHarness {
 private static int checks;
 private static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
 private static void frame(ProbeState s,int type,int... p){IpodMarkerStateHarness.frame(s,type,p);}
 public static void main(String[] args)throws Exception {
  ProbeState s=IpodMarkerStateHarness.state(2,1);
  check(s.overlaySnapshot().bsjAudioEnabled==null,"no state guessed from source callback");
  frame(s,3,0x81,8);ProbeState.Snapshot a=s.overlaySnapshot();long source=a.sourceSequence;
  check(Boolean.TRUE.equals(a.bsjAudioEnabled)&&a.bsjAudioPowerRevision==0,"initial state is baseline");
  frame(s,3,0x81,9);frame(s,3,0x81,9);frame(s,3,0xC1,9);
  a=s.overlaySnapshot();check(a.bsjAudioPowerRevision==0,"volume repeats and menu bit do not create power events");check(a.sourceSequence==source,"power tracking does not alter source epoch");
  frame(s,3,0x01,0);a=s.overlaySnapshot();check(Boolean.FALSE.equals(a.bsjAudioEnabled)&&a.bsjAudioPowerRevision==1,"off detected");check(a.sourceSequence==source&&a.audioSource()==2,"off retains CD source");
  frame(s,3,0x01,0);check(s.overlaySnapshot().bsjAudioPowerRevision==1,"duplicate off ignored");
  frame(s,3,0x81,1);for(int volume=2;volume<=8;volume++)frame(s,3,0x81,volume);
  check(Boolean.TRUE.equals(s.snapshot().bsjAudioEnabled)&&s.snapshot().bsjAudioPowerRevision==2,"on detected once across ramp; full snapshot agrees");
  frame(s,3,0x81);check(s.overlaySnapshot().bsjAudioPowerRevision==2,"bad length ignored");
  s.event(1000,new int[]{1},null,null);frame(s,3,0x01,0);check(s.overlaySnapshot().bsjAudioEnabled==null&&s.overlaySnapshot().bsjAudioPowerRevision==0,"other profile ignores bit");
  s.event(1000,new int[]{6291908},null,null);frame(s,3,0x01,0);s.newSession();check(s.overlaySnapshot().bsjAudioEnabled==null&&s.overlaySnapshot().bsjAudioPowerRevision==0,"new session clears state");
  for(String path:args){
   s=IpodMarkerStateHarness.state(2,1);int transitions=0;Boolean prev=null;
   for(String line:java.nio.file.Files.readAllLines(java.nio.file.Paths.get(path))){String[] f=line.split(",");int type=Integer.parseInt(f[1]);int[] p=new int[f.length-3];for(int i=0;i<p.length;i++)p[i]=Integer.parseInt(f[i+3]);frame(s,type,p);Boolean power=s.overlaySnapshot().bsjAudioEnabled;if(prev!=null&&power!=null&&!prev.equals(power))transitions++;if(power!=null)prev=power;}
   int expected=path.contains("7812133")?3:1;check(transitions==expected&&s.overlaySnapshot().bsjAudioPowerRevision==expected,"recorded power cycles "+path);check(Boolean.TRUE.equals(s.overlaySnapshot().bsjAudioEnabled),"recorded cycle ends on");System.out.println(path+": "+transitions+" transitions, final on");
  }
  System.out.println("PASS: "+checks+" audio bit7 checks");
 }
}
