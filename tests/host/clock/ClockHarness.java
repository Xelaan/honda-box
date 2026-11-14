package com.crewbeat.hondabox;
public final class ClockHarness {
 private static int checks;
 private static void check(boolean pass){checks++;if(!pass)throw new AssertionError("check "+checks);}
 public static void main(String[] args){
  // Observed Acura minute/hour rollover and previous captures.
  check("5:58".equals(BsjClock.decode(new int[]{1,0xF5,0x58})));
  check("5:59".equals(BsjClock.decode(new int[]{1,0xF5,0x59})));
  check("6:00".equals(BsjClock.decode(new int[]{1,0xF6,0})));
  check("4:57".equals(BsjClock.decode(new int[]{1,0xF4,0x57})));
  check("7:22".equals(BsjClock.decode(new int[]{1,0xF7,0x22})));
  int[][] bad={null,{}, {1,0xF5},{1,0xF5,0,0},{0,0xF5,0},{1,0xE5,0},{1,0xFA,0},{1,0xF5,0x60},{1,0xF5,0x5A},{1,0xF5,-1}};
  for(int[] p:bad)check(BsjClock.decode(p)==null);
  BsjClock c=new BsjClock();check(c.latest()==null);
  int[] payload={1,0xF5,0x58};c.accept(payload,1000,true);BsjClock.Sample first=c.latest();payload[2]=0;
  check("01 F5 58".equals(first.payloadHex));check("5:58".equals(first.candidate));
  c.accept(new int[]{1,0xF6,0},61000,true);check(c.latest().at==61000&&c.latest().count==2);check(first.count==1);
  c.accept(new int[]{1,0xFA,0},121000,true);check(c.latest().candidate==null&&"01 FA 00".equals(c.latest().payloadHex));
  c.accept(new int[]{1,0xF6,0},181000,false);check(c.latest().candidate==null);
  c.clear();check(c.latest()==null);c.accept(new int[]{1,0xF6,0},241000,true);check(c.latest().count==1);
  System.out.println(checks+" clock checks passed");
 }
}
