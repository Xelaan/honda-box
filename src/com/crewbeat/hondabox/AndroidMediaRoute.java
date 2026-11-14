package com.crewbeat.hondabox;
/** Android metadata only owns AUX or a genuinely unknown vehicle source, never missing OEM titles. */
public final class AndroidMediaRoute {
 public static boolean allowed(ProbeState.Snapshot s){
  if(s.connected&&s.accord()&&Boolean.FALSE.equals(s.bsjAudioEnabled))return false;
  if(AudioMode.reported(s)!=null)return false;
  if(!s.connected||!s.honda())return true;
  Integer n=s.audioSource();return n==null||n==0||n<0||n>4||n==s.auxSource;
 }
 public static String label(ProbeState.Snapshot s){return s.connected&&s.honda()&&s.audioSource()!=null&&s.audioSource()==s.auxSource?s.auxName:"Android";}
 private AndroidMediaRoute(){}
}
