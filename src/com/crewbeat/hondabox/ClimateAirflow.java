package com.crewbeat.hondabox;
/** Shared display mapping; does not alter raw direction fields. */
public final class ClimateAirflow {
 public static int mask(ProbeState.Snapshot s){
  if(s==null||!s.connected)return -1;
  Integer glass=s.number(18),face=s.number(19),feet=s.number(20);int mask=(Integer.valueOf(1).equals(glass)?1:0)|(Integer.valueOf(1).equals(face)?2:0)|(Integer.valueOf(1).equals(feet)?4:0);
  if(mask==0&&!s.civic()&&s.supportsClimate()&&Integer.valueOf(1).equals(s.climatePower())&&(!s.temperature(27).equals("—")||!s.temperature(28).equals("—")))return 1;
  return glass==null||face==null||feet==null?-1:mask;
 }
 /** Compact display fallback. Civic may omit directions even with AUTO=0; no raw flag is changed. */
 public static boolean automatic(ProbeState.Snapshot s){
  if(s==null||!s.connected||!s.supportsClimate()||!Integer.valueOf(1).equals(s.climatePower()))return false;
  if(!s.civic()&&!Integer.valueOf(1).equals(s.number(13))&&!Integer.valueOf(1).equals(s.number(49)))return false;
  return !Integer.valueOf(1).equals(s.number(18))&&!Integer.valueOf(1).equals(s.number(19))&&!Integer.valueOf(1).equals(s.number(20));
 }
 public static int glyph(ProbeState.Snapshot s){switch(mask(s)){case 1:return 17;case 2:return 19;case 6:return 119;case 4:return 20;case 5:return 120;default:return -1;}}
 private ClimateAirflow(){}
}
