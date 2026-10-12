package dev.alex.fyt.probe;
/** Runs real ProbeState/AudioMode classes on the JVM; Android JSON stubs are only used in caught diagnostic exports. */
public final class IpodMarkerStateHarness {
 static int checks;static void check(String name,boolean ok){checks++;if(!ok)throw new AssertionError(name);}
 static void frame(ProbeState state,int type,int... p){int[] raw=new int[p.length+4];raw[0]=46;raw[1]=type;raw[2]=p.length;int sum=type+p.length;for(int i=0;i<p.length;i++){raw[i+3]=p[i];sum+=p[i];}raw[raw.length-1]=255-(sum&255);state.event(1019,raw,null,null);}
 static void frameAt(ProbeState s,long at,int type,int... p){try{s.event(1019,new int[]{0},null,null);java.lang.reflect.Method m=ProbeState.class.getDeclaredMethod("receiveBsjFrame",int.class,int[].class,long.class);m.setAccessible(true);m.invoke(s,type,p,at);}catch(Exception e){throw new AssertionError(e);}}
 static void confirmTitle(ProbeState s){frameAt(s,System.currentTimeMillis()+4000,7,7,78,101,119,32,115,111,110,103);}
 static void ipodMenu(ProbeState s){int[] p=new int[49];String label="Playlists";for(int i=0;i<label.length();i++)p[i+1]=label.charAt(i);frame(s,9,p);}
 static ProbeState state(int source,int band){ProbeState s=new ProbeState();s.event(1000,new int[]{6291908},null,null);s.event(98,new int[]{source},null,null);s.event(115,new int[]{band},null,null);s.status("test","live",true);return s;}
 public static void main(String[] args)throws Exception{
  ProbeState s=state(2,5);frame(s,5,1,0,1,1,0,4,1,8);frame(s,6,0x32,0x22,0x22);
  ProbeState.Snapshot a=s.overlaySnapshot();check("CD marker immediately selects iPod",a.mediaDevice==ProbeState.MediaDevice.IPOD);check("source label immediately selects iPod",a.source().equals("iPod"));check("shared USB screen selected",AudioMode.reported(a)==AudioMode.USB);check("previous CD counter invalidated",!a.fresh(101));
  long rev=a.mediaDeviceRevision;frame(s,6,0x32,0x22,0x22);check("no duplicate device revision",s.overlaySnapshot().mediaDeviceRevision==rev);
  frame(s,5,1,0,1,1,0,4,1,9);a=s.overlaySnapshot();check("resumed CD corrects inferred iPod",a.mediaDevice==ProbeState.MediaDevice.UNKNOWN&&AudioMode.reported(a)==AudioMode.CD);check("resumed counter remains fresh",a.fresh(101));
  s=state(2,5);frame(s,6,0x32,0x22,0x22);ipodMenu(s);frame(s,5,0,0x70,1,2,0,1,0,0x31);a=s.overlaySnapshot();check("USB-family counters preserve menu-confirmed iPod",a.mediaDevice==ProbeState.MediaDevice.IPOD&&a.source().startsWith("iPod"));check("iPod timer remains fresh",a.fresh(99)&&a.fresh(100)&&a.displayNumber(100)==0x31);
  rev=a.mediaDeviceRevision;frame(s,5,0,0x70,1,2,0,1,0,0x32);a=s.overlaySnapshot();check("advancing timer does not change iPod identity",a.mediaDevice==ProbeState.MediaDevice.IPOD&&a.mediaDeviceRevision==rev&&a.displayNumber(100)==0x32);
  s=state(1,5);frame(s,6,0x32,0x22,0x22);a=s.overlaySnapshot();check("AM2 opens shared media screen",AudioMode.reported(a)==AudioMode.USB);check("raw AM source preserved",a.rawAudioSource()==1&&a.bsjSource==null);check("AM marker immediately selects iPod",a.source().equals("iPod"));
  frame(s,3,0x81,7);a=s.overlaySnapshot();check("later CD carrier retains immediate iPod",a.mediaDevice==ProbeState.MediaDevice.IPOD&&AudioMode.reported(a)==AudioMode.USB);
  frame(s,3,0x82,7);a=s.overlaySnapshot();check("AUX clears tentative iPod",a.mediaDevice==ProbeState.MediaDevice.UNKNOWN&&AudioMode.aux(a));
  s=state(1,5);frame(s,6,0x32,0x22,0x22);frame(s,4,5,0,98,4,1);a=s.overlaySnapshot();check("live radio corrects AM marker assumption",a.mediaDevice==ProbeState.MediaDevice.UNKNOWN&&AudioMode.reported(a)==AudioMode.RADIO);
  s=state(1,5);frame(s,6,0x32,0x22,0x22);frame(s,5,0,0x70,1,2,0,1,0,0x31);a=s.overlaySnapshot();check("USB playback corrects ambiguous AM marker",a.mediaDevice==ProbeState.MediaDevice.USB&&a.audioSource()==4&&a.rawAudioSource()==1&&AudioMode.reported(a)==AudioMode.USB);
  frame(s,4,5,0,98,4,1);check("radio corrects iPod inference over stale AM",AudioMode.reported(s.overlaySnapshot())==AudioMode.RADIO);
  s=state(1,2);frame(s,6,0x32,0x22,0x22);check("FM marker immediately selects iPod",s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.IPOD&&AudioMode.reported(s.overlaySnapshot())==AudioMode.USB);frame(s,4,2,0,98,4,1);check("live FM corrects stale marker",AudioMode.reported(s.overlaySnapshot())==AudioMode.RADIO);
  s=state(4,5);frame(s,5,0,0x70,1,2,0,1,0,0x31);frame(s,6,0x32,0x22,0x22);check("active USB marker does not mislabel flash drive",s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.USB);
  s=state(2,5);frame(s,5,0,0x70,1,2,0,1,0,0x31);frame(s,6,0x32,0x22,0x22);check("USB over stale CD carrier ignores marker",s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.USB);
  s=state(2,5);frame(s,6,0x32,0x22,0x22);frame(s,5,0,9,0,0,0,0,0,0);check("NO DATA clears marker",s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.UNKNOWN);
  {int[] emptyTitle=new int[31];emptyTitle[0]=7;long now=System.currentTimeMillis();
   s=state(4,5);frameAt(s,now,5,0,11,0,0,0,0,0,0);frameAt(s,now+100,6,0x32,0x22,0x22);frameAt(s,now+7000,7,emptyTitle);a=s.overlaySnapshot();check("paused iPod becomes iPod before playback",a.mediaDevice==ProbeState.MediaDevice.IPOD&&a.source().equals("iPod")&&a.mediaDeviceEvidence.contains("paused iPod"));check("paused iPod leaves missing title empty",a.combinedMediaText().equals("—"));
   s=state(4,5);frameAt(s,now,6,0x32,0x22,0x22);frameAt(s,now+7000,7,emptyTitle);check("empty title without connection state stays USB",s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.USB);
   s=state(4,5);frameAt(s,now,5,0,11,0,0,0,0,0,0);frameAt(s,now+100,6,0x32,0x22,0x22);frameAt(s,now+300,5,0,0,1,2,0,1,0,1);frameAt(s,now+7000,7,emptyTitle);check("USB loading followed by counters stays USB",s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.USB);
  }
  for(int carrier:new int[]{2,4}){
   s=state(carrier,5);frame(s,5,0,0,1,2,0,1,0,9);
   frame(s,5,0,9,0,0,0,0,0,0);check("USB unplug clears identity, carrier "+carrier,s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.UNKNOWN);
   frame(s,6,0x32,0x22,0x22);a=s.overlaySnapshot();rev=a.mediaDeviceRevision;
   check("replacement defaults to USB without MODE, carrier "+carrier,a.mediaDevice==ProbeState.MediaDevice.USB&&AudioMode.reported(a)==AudioMode.USB);
   frame(s,6,0x32,0x22,0x22);check("replacement marker does not rebuild repeatedly",s.overlaySnapshot().mediaDeviceRevision==rev);
   confirmTitle(s);check("replacement iPod confirmed with title",s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.IPOD&&s.overlaySnapshot().combinedMediaText().equals("New song"));
   frame(s,3,0x81,7);ipodMenu(s);frame(s,5,0,0,1,2,0,1,0,1);check("timer does not demote menu-confirmed replacement iPod",s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.IPOD);
   frame(s,3,0x81,7);check("CD carrier confirmation preserves replacement iPod",s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.IPOD);
   frame(s,3,0x82,7);check("AUX still clears replacement iPod",s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.UNKNOWN&&AudioMode.aux(s.overlaySnapshot()));
  }
  for(int aux:new int[]{3,4}){
   s=state(aux,5);s.auxSource(aux);frame(s,3,0x80|(aux-1),7);
   frame(s,3,aux-1,0);frame(s,6,0,0,0);frame(s,6,0x32,0x22,0x22);
   frame(s,3,0x80|(aux-1),7);frame(s,6,0x32,0x22,0x22);confirmTitle(s);
   a=s.overlaySnapshot();check("AUX power cycle remains AUX, code "+aux,a.mediaDevice==ProbeState.MediaDevice.UNKNOWN&&a.audioSource()==aux&&AudioMode.reported(a)==null&&AudioMode.aux(a));
   frame(s,3,0x81,7);frame(s,6,0x32,0x22,0x22);a=s.overlaySnapshot();check("leaving AUX for CD restores iPod marker detection, code "+aux,a.mediaDevice==ProbeState.MediaDevice.IPOD&&AudioMode.reported(a)==AudioMode.USB);
  }
  s=state(2,5);frame(s,5,0,0,1,2,0,1,0,9);frame(s,5,0,9,0,0,0,0,0,0);frame(s,5,1,0,1,1,0,4,0,1);frame(s,6,0x32,0x22,0x22);check("real CD counters clear stale USB replacement context",s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.IPOD);
  s=state(4,5);frame(s,5,0,0,1,2,0,1,0,9);frame(s,5,0,9,0,0,0,0,0,0);frame(s,3,0x80,7);frame(s,6,0x32,0x22,0x22);check("radio selection clears stale USB context",s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.IPOD);
  for(int carrier:new int[]{2,4}){
   s=state(carrier,5);frame(s,6,0x32,0x22,0x22);frame(s,5,0,0,1,2,0,1,0,9);
   check("flash counters correct tentative iPod, carrier "+carrier,s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.USB&&s.overlaySnapshot().source().equals("USB"));
   long stable=s.overlaySnapshot().mediaDeviceRevision;frame(s,6,0x32,0x22,0x22);frame(s,5,0,0,1,2,0,1,0,10);
   check("repeated marker cannot relabel active flash, carrier "+carrier,s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.USB&&s.overlaySnapshot().mediaDeviceRevision==stable);
   frame(s,5,0,9,0,0,0,0,0,0);frame(s,6,0x32,0x22,0x22);
   check("flash unplug keeps USB as replacement default, carrier "+carrier,s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.USB);confirmTitle(s);check("replacement title confirms iPod, carrier "+carrier,s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.IPOD);
  }
  for(String file:args){
   s=state(file.contains("connected-paused")||file.contains("flash-reconnect")?4:2,5);long markerAt=-1;int tentative=0,corrections=0;
   for(String line:java.nio.file.Files.readAllLines(java.nio.file.Paths.get(file))){String[] f=line.split(",");long at=Long.parseLong(f[0]);int type=Integer.parseInt(f[1]);int[] payload=new int[f.length-3];for(int i=0;i<payload.length;i++)payload[i]=Integer.parseInt(f[i+3]);
    ProbeState.MediaDevice before=s.overlaySnapshot().mediaDevice;frameAt(s,at,type,payload);a=s.overlaySnapshot();
    if(before!=ProbeState.MediaDevice.IPOD&&a.mediaDevice==ProbeState.MediaDevice.IPOD){tentative++;if(markerAt<0)markerAt=at;}
    if(before==ProbeState.MediaDevice.IPOD&&a.mediaDevice!=ProbeState.MediaDevice.IPOD)corrections++;
   }
   if(file.contains("connected-paused"))check("physical paused iPod replay selects before playback",markerAt==1791733178124L&&a.mediaDevice==ProbeState.MediaDevice.IPOD);
   if(file.contains("flash-reconnect"))check("physical USB replay never selects iPod",tentative==0&&a.mediaDevice==ProbeState.MediaDevice.USB);
   System.out.println(file+": tentative="+tentative+", corrections="+corrections+", final="+a.source());
   if(file.contains("5092957"))check("real state short1 marker and iPod final",markerAt==1791545083822L&&a.mediaDevice==ProbeState.MediaDevice.IPOD);
   if(file.contains("5340483"))check("real state short2 marker and iPod final",markerAt==1791545328700L&&a.mediaDevice==ProbeState.MediaDevice.IPOD);
   if(file.contains("4982637"))check("long USB counterexample corrected; final CD retained",a.mediaDevice==ProbeState.MediaDevice.UNKNOWN&&AudioMode.reported(a)==AudioMode.CD);
  }
  System.out.println("PASS: "+checks+" real ProbeState marker checks");
 }
}
