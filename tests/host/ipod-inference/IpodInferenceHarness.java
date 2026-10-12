import dev.alex.fyt.probe.BsjIpodInference;
import java.nio.file.*;
public final class IpodInferenceHarness {
 static int checks;
 static void check(String name,boolean ok){checks++;if(!ok)throw new AssertionError(name);}
 static int[] text(String s){int[] p=new int[s.length()+1];p[0]=7;for(int i=0;i<s.length();i++)p[i+1]=s.charAt(i);return p;}
 static int[] marker={0x32,0x22,0x22};
 static BsjIpodInference cd(){BsjIpodInference m=new BsjIpodInference();for(int i=0;i<3;i++)m.accept(5,new int[]{1,0,1,1,0,4,1,i},i*1000,2);return m;}
 public static void main(String[] args)throws Exception{
  BsjIpodInference m=cd();check("silence alone does not infer",!m.accept(3,new int[]{129,7},10000,2));
  check("text without changer does not infer",!m.accept(7,text("New song"),11000,2));
  check("active CD changer does not switch",!m.accept(6,new int[]{0x42,0x22,0x22},12000,2));
  check("CD marker selects iPod without waiting for title",m.accept(6,marker,12001,2));
  check("marker evidence retained",m.fromMarker()&&!m.pendingMarker());
  check("repeated marker does not switch twice",!m.accept(6,marker,13000,2));
  check("CD title needs no second device confirmation",!m.accept(7,text("New song"),16000,2)&&!m.pendingMarker());
  check("confirmed title does not switch twice",!m.accept(7,text("New song"),17000,2));
  m=new BsjIpodInference();check("CD requires no timer prelude",m.accept(6,marker,0,2));
  for(int band=3;band<=5;band++){m=new BsjIpodInference();check("AM band "+band+" switches on marker",m.accept(6,marker,0,1,band));}
  for(int band=0;band<=2;band++){m=new BsjIpodInference();check("FM band "+band+" switches immediately on marker",m.accept(6,marker,0,1,band)&&!m.pendingMarker());}
  m=new BsjIpodInference();check("non-USB radio with unknown band selects iPod",m.accept(6,marker,0,1)&&!m.pendingMarker());
  m=new BsjIpodInference();check("AUX marker selects iPod",m.accept(6,marker,0,3)&&!m.pendingMarker());
  m=new BsjIpodInference();check("USB marker opens provisional external media",m.accept(6,marker,0,4));
  m=new BsjIpodInference();check("unknown source does not switch",!m.accept(6,marker,0,null));
  m=new BsjIpodInference();check("exact bytes required",!m.accept(6,new int[]{0x32,0x22,0x23},0,2));
  check("exact length required",!m.accept(6,new int[]{0x32,0x22},0,2));
  m=new BsjIpodInference();m.accept(6,marker,0,2);m.accept(5,new int[]{1,0,1,1,0,4,1,0},1000,2);check("resumed CD counters rearm subsequent transition",m.accept(6,marker,2000,2));
  m=new BsjIpodInference();m.accept(6,marker,0,2);m.accept(5,new int[]{0,0x70,1,2,0,1,1,0},1000,4);check("active USB counters block repeated marker",!m.accept(6,marker,2000,4));m.accept(5,new int[]{0,9,0,0,0,0,0,0},2500,4);check("disconnect rearms replacement iPod marker",m.accept(6,marker,3000,4));
  m=new BsjIpodInference();m.accept(6,marker,0,2);m.accept(5,new int[]{0,9,0,0,0,0,0,0},1000,2);check("NO DATA resets inferred latch",!m.fromMarker());
  m=new BsjIpodInference();m.accept(6,marker,0,2);m.accept(127,new int[16],1000,2);check("adapter reset clears marker",!m.fromMarker());
  m=new BsjIpodInference();m.accept(6,marker,0,4);check("early title leaves USB default",!m.accept(7,text("Flash song"),100,4)&&m.pendingMarker());m.accept(5,new int[]{0,0,1,2,0,1,0,1},200,2);check("USB playback cancels pending confirmation",!m.pendingMarker());check("later flash title never confirms iPod",!m.accept(7,text("Flash song"),6000,2));
  m=new BsjIpodInference();m.accept(6,marker,0,4);check("silence and repeated markers cannot confirm iPod",!m.accept(6,marker,10000,4)&&m.pendingMarker());check("NO INFO is not confirmation",!m.accept(7,text("NO INFO"),11000,4));check("live iPod text confirms without a timer",m.accept(7,text("iPod song"),12000,4)&&m.fromMarker()&&!m.pendingMarker());
  int[] blankTitle=new int[31];blankTitle[0]=7;
  m=new BsjIpodInference();m.accept(5,new int[]{0,11,0,0,0,0,0,0},0,4);m.accept(6,marker,100,4);check("paused iPod confirms before Play",m.accept(7,blankTitle,7000,4)&&m.fromStandbyTitle());
  m=new BsjIpodInference();m.accept(6,marker,0,4);check("empty TITLE alone is not iPod evidence",!m.accept(7,blankTitle,7000,4)&&m.pendingMarker());
  m=new BsjIpodInference();m.accept(5,new int[]{0,11,0,0,0,0,0,0},0,4);m.accept(6,marker,100,4);check("loading and marker alone stay USB",!m.accept(6,marker,7000,4)&&m.pendingMarker());check("NO INFO does not replace empty TITLE heartbeat",!m.accept(7,text("NO INFO"),7100,4));
  m=new BsjIpodInference();m.accept(5,new int[]{0,11,0,0,0,0,0,0},0,4);m.accept(6,marker,100,4);check("early empty TITLE stays USB",!m.accept(7,blankTitle,200,4));m.accept(5,new int[]{0,0,1,2,0,1,0,1},300,4);check("USB counters block empty TITLE classification",!m.accept(7,blankTitle,7000,4)&&!m.fromStandbyTitle());
  m=new BsjIpodInference();m.accept(5,new int[]{0,11,0,0,0,0,0,0},0,4);m.accept(6,marker,100,4);check("old loading state cannot confirm",!m.accept(7,blankTitle,20000,4));
  for(String file:args){java.util.List<String> lines=Files.readAllLines(Paths.get(file));boolean lengthColumn=true;for(String row:lines){String[] cells=row.split(",");if(Integer.parseInt(cells[2])!=cells.length-3)lengthColumn=false;}m=new BsjIpodInference();int source=file.contains("connected-paused")||file.contains("flash-reconnect")?4:2;int matches=0;long first=-1,last=-1;for(String line:lines){String[] fields=line.split(",");long at=Long.parseLong(fields[0]);int type=Integer.parseInt(fields[1]),length=Integer.parseInt(fields[2]);int[] p=new int[fields.length-3];for(int i=0;i<p.length;i++)p[i]=Integer.parseInt(fields[i+3]);if(!lengthColumn)source=length;else if(length!=p.length)throw new AssertionError("CSV payload length");if(lengthColumn&&type==3&&p.length==2){int next=(p[0]&15)+1;if(source!=next)m.reset();source=next;}if(m.accept(type,p,at,source)){matches++;last=at;if(first<0)first=at;System.out.println(file+": inferred at "+at);}}
   if(file.contains("5092957"))check("short CD to iPod switches at marker",matches==1&&first==1791545083822L);
   if(file.contains("5340483"))check("short AM/CD to iPod switches at marker",matches==1&&first==1791545328700L);
   if(file.contains("connected-paused"))check("paused iPod confirmed on its first empty TITLE heartbeat",matches==3&&last==1791733178124L);
   if(file.contains("flash-reconnect"))check("USB replay stays provisional USB until counters",matches>=1&&!m.pendingMarker());
   // Long log contains CD -> USB too: tentative iPod is deliberately corrected by USB counters in ProbeState.
  }
  System.out.println("PASS: "+checks+" iPod marker checks");
 }
}
