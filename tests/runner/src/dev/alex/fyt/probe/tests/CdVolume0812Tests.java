package dev.alex.fyt.probe.tests;
import dev.alex.fyt.probe.*;
final class CdVolume0812Tests {
 private int checks;private void check(String name,boolean ok){if(!ok)throw new AssertionError(name);checks++;}
 private void raw(ProbeState s,int type,int... p){int[] a=new int[p.length+4];a[0]=46;a[1]=type;a[2]=p.length;int sum=type+p.length;for(int i=0;i<p.length;i++){a[i+3]=p[i];sum+=p[i];}a[a.length-1]=(~sum)&255;s.event(1019,a,null,null);}
 private void text(ProbeState s,String t){int[] a=new int[t.length()+1];a[0]=7;for(int i=0;i<t.length();i++)a[i+1]=t.charAt(i);raw(s,7,a);}
 private ProbeState playing(){ProbeState s=new ProbeState();s.newSession();s.status("test","",true);s.event(1000,new int[]{6291908},null,null);s.event(98,new int[]{0},null,null);raw(s,5,2,0x30,1,1,0,0x59,1,0x38);text(s,"CD title before volume");return s;}
 int run(){
  ProbeState s=playing();long source=s.snapshot().sourceSequence,at=s.snapshot().bsjValues.get(101).at;check("CD visible before source announcement",s.snapshot().cdNumber(101).equals("59")&&s.snapshot().cdTime().equals("01:38"));
  raw(s,3,0x81,12);check("first volume source does not erase CD",s.snapshot().cdNumber(101).equals("59")&&s.snapshot().cdNumber(113).equals("2")&&s.snapshot().cdTime().equals("01:38")&&s.snapshot().combinedMediaText().equals("CD title before volume"));
  check("source epoch still announces CD",s.snapshot().sourceSequence>source&&AudioMode.reported(s.snapshot())==AudioMode.CD);check("retained data timestamps are untouched",s.snapshot().bsjValues.get(101).at==at);
  raw(s,5,0,0,0,0,0,0,0,0);s.event(101,new int[]{0},null,null);s.event(113,new int[]{0},null,null);s.event(99,new int[]{0},null,null);s.event(100,new int[]{0},null,null);
  check("volume blank and logical zeros retain CD",s.snapshot().cdNumber(101).equals("59")&&s.snapshot().cdTime().equals("01:38")&&s.snapshot().combinedMediaText().equals("CD title before volume"));
  raw(s,3,0xC1,13);check("menu show flag does not erase track",s.snapshot().cdNumber(101).equals("59"));raw(s,3,0x81,14);check("menu hide retains same track",s.snapshot().cdNumber(101).equals("59"));
  raw(s,5,2,0,1,1,0,0x60,0,0);check("actual new track still replaces title",s.snapshot().cdNumber(101).equals("60")&&s.snapshot().combinedMediaText().equals("—"));
  text(s,"Track sixty");raw(s,3,0x83,14);check("real CD to USB source change hides old media",s.snapshot().cdNumber(101).equals("—")&&s.snapshot().combinedMediaText().equals("—"));
  ProbeState logical=playing();logical.event(98,new int[]{2},null,null);check("first logical source preserves matching CD playback",logical.snapshot().cdNumber(101).equals("59")&&logical.snapshot().combinedMediaText().equals("CD title before volume"));
  ProbeState radio=playing();raw(radio,3,0x80,10);check("different first source does not retain CD",radio.snapshot().cdNumber(101).equals("—")&&radio.snapshot().combinedMediaText().equals("—"));
  ProbeState unknown=new ProbeState();unknown.newSession();unknown.status("test","",true);unknown.event(1000,new int[]{6291908},null,null);unknown.event(98,new int[]{0},null,null);text(unknown,"unidentified text");raw(unknown,3,0x81,10);check("text without compatible playback is not promoted",unknown.snapshot().combinedMediaText().equals("—"));
  return checks;
 }
}
