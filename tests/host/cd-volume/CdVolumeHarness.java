package dev.alex.fyt.probe;
public final class CdVolumeHarness {
 private static int checks;
 private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 private static void raw(ProbeState s,int type,int... p){IpodMarkerStateHarness.frame(s,type,p);}
 private static ProbeState playing(){ProbeState s=IpodMarkerStateHarness.state(2,5);raw(s,5,2,0x30,1,1,0,0x59,1,0x38);raw(s,7,7,83,111,110,103);raw(s,3,0x81,12);return s;}
 private static void retained(ProbeState s){ProbeState.Snapshot a=s.overlaySnapshot();check(a.cdNumber(113).equals("2"),"disc2 stays highlighted");check(a.cdNumber(101).equals("59"),"track stays");check(a.cdTime().equals("01:38"),"timer stays");check(a.combinedMediaText().equals("Song"),"selected title stays");}
 public static void main(String[] args){
  ProbeState s=playing();raw(s,5,0,0,1,1,0,0,0,0);retained(s);
  s=playing();raw(s,5,0,0,1,1,0,0x59,0,0);retained(s);
  raw(s,7,0,0,0,0);retained(s);check(s.overlaySnapshot().currentTitleField()==7,"blank volume text does not change TITLE field");
  raw(s,5,2,0,1,1,0,0x60,0,0);check(s.overlaySnapshot().cdNumber(101).equals("60"),"new track accepted during volume");check(s.overlaySnapshot().combinedMediaText().equals("—"),"new track clears previous title");
  raw(s,5,0,9,0,0,0,0,0,0);check(s.overlaySnapshot().cdNumber(113).equals("—"),"NO DATA clears disc during volume");
  s=playing();raw(s,3,0x83,12);check(s.overlaySnapshot().cdNumber(113).equals("—"),"real source change clears CD");
  s=playing();raw(s,5,0,0,1,2,0,1,0,9);check(s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.USB,"USB playback is not suppressed during volume");
  s=playing();raw(s,5,3,0,1,1,0,1,0,0);check(s.overlaySnapshot().cdNumber(113).equals("3"),"real disc change accepted during volume");
  for(boolean ipod:new boolean[]{false,true}){
   s=IpodMarkerStateHarness.state(4,5);raw(s,5,0,0,1,2,0,1,1,0x38);if(ipod){IpodMarkerStateHarness.ipodMenu(s);raw(s,5,0,0,1,2,0,1,1,0x38);}
   raw(s,7,7,83,111,110,103);raw(s,3,0x83,12);raw(s,5,0,0,1,2,0,0,0,0);raw(s,7,0,0,0);
   ProbeState.Snapshot a=s.overlaySnapshot();check(a.cdNumber(101).equals("1"),"USB/iPod track retained");check(a.cdTime().equals("01:38"),"USB/iPod timer retained");check(a.combinedMediaText().equals("Song")&&a.currentTitleField()==7,"USB/iPod selected TITLE retained");
   check(a.mediaDevice==(ipod?ProbeState.MediaDevice.IPOD:ProbeState.MediaDevice.USB),"USB/iPod identity retained");
   raw(s,5,0,10,0,0,0,0,0,0);check(s.overlaySnapshot().combinedMediaText().equals("—"),"USB/iPod disconnect still clears data");
  }
  s=IpodMarkerStateHarness.state(1,2);raw(s,4,2,0x27,0x10,3,1);raw(s,3,0x80,12);raw(s,4,0,0,0,0,0);
  check(s.overlaySnapshot().displayNumber(119)==10000&&s.overlaySnapshot().displayNumber(115)==2&&s.overlaySnapshot().displayNumber(116)==3,"radio station/band/preset retained during blank volume frame");
  raw(s,4,2,0x27,0x74,4,1);check(s.overlaySnapshot().displayNumber(119)==10100&&s.overlaySnapshot().displayNumber(116)==4,"real station change accepted during volume");
  System.out.println("PASS: "+checks+" media/radio volume checks");
 }
}
