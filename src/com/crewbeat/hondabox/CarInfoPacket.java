package com.crewbeat.hondabox;
/** Exact stock TYPE0E mapping. Field semantics and units are deliberately not inferred. */
public final class CarInfoPacket {
 public static int[] decode(int[] p){
  if(p==null||p.length!=13)return null;
  return new int[]{p[0]&255,(p[1]&255)|((p[2]&255)<<8),(p[3]&255)|((p[4]&255)<<8),p[5]&255,p[6]&255,p[7]&255,(p[8]&255)|((p[9]&255)<<8),(p[10]&255)|((p[11]&255)<<8)|((p[12]&255)<<16)};
 }
 private CarInfoPacket(){}
}
