package dev.alex.fyt.probe;
/** Shared display mapping; does not alter raw direction fields. */
public final class ClimateAirflow {
 public static int mask(ProbeState.Snapshot s){
  if(s==null||!s.connected)return -1;
  Integer glass=s.number(18),face=s.number(19),feet=s.number(20);int mask=(Integer.valueOf(1).equals(glass)?1:0)|(Integer.valueOf(1).equals(face)?2:0)|(Integer.valueOf(1).equals(feet)?4:0);
  if(mask==0&&s.honda()&&Integer.valueOf(1).equals(s.climatePower())&&(!s.temperature(27).equals("—")||!s.temperature(28).equals("—")))return 1;
  return glass==null||face==null||feet==null?-1:mask;
 }
 public static int glyph(ProbeState.Snapshot s){switch(mask(s)){case 1:return 17;case 2:return 19;case 6:return 119;case 4:return 20;case 5:return 120;default:return -1;}}
 private ClimateAirflow(){}
}
