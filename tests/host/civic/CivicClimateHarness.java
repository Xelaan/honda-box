package com.crewbeat.hondabox;
import java.nio.file.*;
/** Replays numeric FYT callbacks and raw chunks through the production state/transport. */
public final class CivicClimateHarness {
 static int checks;
 static void check(String label,boolean b){checks++;if(!b)throw new AssertionError(label);}
 static void event(ProbeState s,int code,int n){s.event(code,new int[]{n},null,null);}
 static ProbeState state(int profile){ProbeState s=new ProbeState();s.processAsHondaAccord(true);event(s,1000,profile);s.running(true);s.status("test","live",true);event(s,28,-1);return s;}
 static void frame(ProbeState s,int type,int...p){int[] r=new int[p.length+4];r[0]=46;r[1]=type;r[2]=p.length;int sum=type+p.length;for(int i=0;i<p.length;i++){r[i+3]=p[i];sum+=p[i];}r[r.length-1]=255-(sum&255);s.event(1019,r,null,null);}
 static String temp(int raw){switch(raw){case 255:return "—";case 0:return "LOW";case 31:return "HI";case 32:return "16.0°";case 34:return "17.0°";default:return String.format(java.util.Locale.US,"%.1f°",(raw-1)/2f+18);}}
 public static void main(String[] args)throws Exception{
  for(int profile:new int[]{15729093,15794629}){
   ProbeState s=state(profile);ProbeState.Snapshot a=s.overlaySnapshot();
   check("Civic routed separately",a.civic()&&a.supportsClimate()&&!a.accord());
   check("no inferred power before first raw",a.climatePower()==null);
   event(s,27,220);event(s,28,240);check("real right temperature retained",s.overlaySnapshot().temperature(28).equals("24.0°"));event(s,28,-1);
   int frames=0;
   for(String row:Files.readAllLines(Paths.get(args[0]))){if(row.startsWith("#")||row.isEmpty())continue;String[] cols=row.split(",");int code=Integer.parseInt(cols[0]);int[] v=new int[cols.length-1];for(int i=0;i<v.length;i++)v[i]=Integer.parseInt(cols[i+1]);s.event(code,v,null,null);
    if(code!=1019)continue;frames++;a=s.overlaySnapshot();int p0=v[3],p1=v[4],t=v[5];boolean on=t!=255||(p0&8)!=0||(p1&15)>0;
    check("power frame "+frames,Integer.valueOf(on?1:0).equals(a.climatePower()));
    check("compact active frame "+frames,ClimateStripOverlay.active(a)==on);
    check("left temp frame "+frames,temp(t).equals(a.temperature(27)));
    check("single-zone display synchronized",a.temperature(28).equals(a.temperature(27))&&a.number(28)==-1&&!a.dualAvailable&&!a.dualInferred());
    check("fan exact",a.number(21)==(p1&15));
    check("AUTO exact",a.number(13)==((p0>>>3)&1));
    check("AC exact",a.number(11)==((p0>>>6)&1));
    int mask=((p1&128)!=0?1:0)|((p1&64)!=0?2:0)|((p1&32)!=0?4:0);
    check("directions without Accord fallback",ClimateAirflow.mask(a)==mask);
    check("compact AUTO for absent direction",ClimateAirflow.automatic(a)==(on&&mask==0));
    check("AUTO fan zero supported",a.climateFanAutomatic()==((p0&8)!=0&&(p1&15)==0));
   }
   check("54 captured frames",frames==54&&s.overlaySnapshot().bsjFramesAccepted==54);
   check("climate packets known",s.unknownMessages.latest().isEmpty());
   check("climate decoded with logging off",!s.recording()&&s.snapshot().rawCount==0);
   event(s,98,123456);a=s.overlaySnapshot();check("distance retained raw",a.number(98)==123456);check("distance not audio",a.audioSource()==null&&AudioMode.reported(a)==null&&a.source().equals("—"));
   frame(s,3,0x81,7);frame(s,6,0x32,0x22,0x22);a=s.overlaySnapshot();check("no Accord source/iPod inference",a.bsjSource==null&&a.mediaDevice==ProbeState.MediaDevice.UNKNOWN&&a.bsjAudioEnabled==null);
   event(s,27,220);frame(s,0x28,8,16,9,255,0,0);a=s.overlaySnapshot();long rev=a.climateRevision;
   frame(s,0x28,8,16,9,255,0,0);check("repeat packet no climate revision",s.overlaySnapshot().climateRevision==rev);
   s.event(1019,new int[]{46,40,6,0,16,255,255,0,0,0},null,null);check("bad checksum cannot turn off",s.overlaySnapshot().climatePower()==1);
   frame(s,0x28,0,16,255);check("short packet cannot turn off",s.overlaySnapshot().climatePower()==1);check("malformed retained",!s.unknownMessages.latest().isEmpty());
   s.clearLog();check("log reset preserves climate",s.overlaySnapshot().climatePower()==1);
   s.status("test","offline",false);check("disconnect hides climate",!ClimateStripOverlay.active(s.overlaySnapshot())&&s.overlaySnapshot().temperature(27).equals("—"));
   event(s,1000,6291908);check("profile switch clears Civic",s.overlaySnapshot().civicClimate==null);
   s.newSession();check("new session clears Civic",s.overlaySnapshot().civicClimate==null);
  }
  ProbeState s=state(6291908);event(s,27,44);check("Accord half degrees unchanged",s.overlaySnapshot().temperature(27).equals("22.0°"));event(s,27,20);check("Accord special18 unchanged",s.overlaySnapshot().temperature(27).equals("18.0°"));
  frame(s,0x28,8,16,9,255,0,0);check("28 not universal",s.overlaySnapshot().civicClimate==null&&!s.unknownMessages.latest().isEmpty());
  event(s,10,1);event(s,13,1);event(s,21,0);event(s,18,0);event(s,19,0);event(s,20,0);
  check("Accord compact AUTO with no direction",ClimateAirflow.automatic(s.overlaySnapshot()));
  event(s,19,1);check("explicit direction takes priority",!ClimateAirflow.automatic(s.overlaySnapshot()));
  event(s,19,0);event(s,13,0);event(s,21,3);check("manual unknown is not AUTO",!ClimateAirflow.automatic(s.overlaySnapshot()));
  check("invalid bytes rejected",CivicClimate.decode(40,new int[]{256,16,9,255,0,0},0)==null);
  check("invalid fan remains unknown",CivicClimate.decode(40,new int[]{0,31,255,255,0,0},0).power()==null);
  check("defensive payload copy",CivicClimate.decode(40,new int[]{0,16,0,255,0,0},0).power()==1);
  System.out.println("PASS: "+checks+" Civic checks (LOW capture replay + HIGH shared-handler synthetic routing)");
 }
}
