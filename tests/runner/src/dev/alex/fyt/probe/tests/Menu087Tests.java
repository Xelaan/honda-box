package dev.alex.fyt.probe.tests;
import dev.alex.fyt.probe.*;
import java.util.*;
final class Menu087Tests {
 private int checks;private void check(String name,boolean ok){if(!ok)throw new AssertionError(name);checks++;}
 private ProbeState.Snapshot page(int source,String text){ProbeState model=new ProbeState();model.newSession();model.status("test","",true);model.event(1000,new int[]{6291908},null,null);model.event(98,new int[]{source},null,null);model.event(136,null,null,new String[]{text});return model.overlaySnapshot();}
 private ProbeState.Snapshot bandPage(int band,String text){ProbeState model=new ProbeState();model.newSession();model.status("test","",true);model.event(1000,new int[]{6291908},null,null);model.event(98,new int[]{1},null,null);model.event(115,new int[]{band},null,null);model.event(136,null,null,new String[]{text});return model.overlaySnapshot();}
 int run(){
  for(int source:new int[]{1,2,4}){MenuListModel m=MenuListModel.from(page(source,"SVC HIGH"));check("root submenu source "+source,m.items.get(7).equals(source==1?"RDS SETTING":"PLAY MODE"));check("CENTER source "+source,m.items.contains("CENTER")&&!m.items.contains("CENTRE"));check("SVC highlight "+source,m.selected==6);}
  String[][] modes={{"NORMAL PLAY","REPEAT ONE TRK","REPEAT ONE FLD","REPEAT DISC","RANDOM IN FLD","RANDOM DISC","SCAN TRK","SCAN FLD","SCAN DISC"},{"NORMAL PLAY","REPEAT ONE TRK","REPEAT ONE FLD","RANDOM IN FLD","RANDOM ALL","SCAN TRK","SCAN FLD"},{"NORMAL PLAY","SHUFFLE OFF","SHUFFLE ALL","SHUFFLE ALBUM","REPEAT OFF","REPEAT ONE TRK"}};
  for(int device=0;device<modes.length;device++)for(String label:modes[device]){ProbeState.Snapshot s=page(device==0?2:4,label);if(device==2)s.mediaDevice=ProbeState.MediaDevice.IPOD;MenuListModel m=MenuListModel.from(s);check("received-only "+device+" "+label,m.title.equals("PLAY MODE")&&m.items.equals(Arrays.asList(label))&&m.selected==0);}
  MenuValues memory=new MenuValues();memory.accept(1,new String[]{"CLOCK:12H","CENTER C","2\u0000REGIONAL OFF"});ProbeState.Snapshot radio=page(1,"SVC HIGH");radio.menuValues=memory.snapshot();MenuListModel m=MenuListModel.from(radio);check("clock value survives cursor movement",m.items.contains("CLOCK : 12H"));check("center received value",m.items.contains("CENTER C"));
  radio=page(1,"4\u0000AUTO TP ON");radio.menuValues=memory.snapshot();m=MenuListModel.from(radio);check("RDS complete set",m.items.size()==5&&m.items.get(0).equals("AF")&&m.items.get(1).equals("REGIONAL OFF")&&m.items.get(4).equals("NEWS"));check("RDS live selection",m.selected==3&&m.items.get(3).contains("AUTO TP ON"));
  for(String category:new String[]{"Playlists","Artists","Albums","Songs"}){m=MenuListModel.from(page(4,"02: "+category.toUpperCase()+" >"));check("iPod category "+category,m.items.contains(category)&&m.items.size()==4);}
  for(int source:new int[]{2,4}){m=MenuListModel.from(page(source,"01: Music >"));check("received folder, no catalog invented "+source,m.items.size()==1&&m.items.get(0).equals("01: Music >"));}
  ProbeState.Snapshot stale=page(2,"SVC HIGH");stale.sourceSequence=10;check("previous-source menu hidden",stale.menuRow(1).isEmpty());
  ProbeState live=new ProbeState();live.newSession();live.status("test","",true);live.event(1000,new int[]{6291908},null,null);live.event(98,new int[]{2},null,null);int[] frame=new int[53];frame[0]=46;frame[1]=9;frame[2]=49;frame[3]=32;String old="SVC HIGH";for(int i=0;i<old.length();i++)frame[20+i]=old.charAt(i);int sum=0;for(int i=1;i<52;i++)sum+=frame[i];frame[52]=(~sum)&255;live.event(1019,frame,null,null);live.event(98,new int[]{4},null,null);live.event(136,null,null,new String[]{"PLAY MODE"});check("fresh logical menu supersedes previous-source raw row",live.overlaySnapshot().menuRow(1).equals("PLAY MODE"));
  ProbeState closing=new ProbeState();closing.newSession();closing.status("test","",true);closing.event(1000,new int[]{6291908},null,null);
  closing.event(139,new int[]{0},null,null);closing.event(136,null,null,new String[]{"BASS +1"});
  check("text following close is a new stock menu event",closing.overlaySnapshot().menuContentSequence>closing.overlaySnapshot().menuVisibilitySequence);
  closing.event(139,new int[]{0},null,null);
  check("repeated logical close closes newer content",closing.overlaySnapshot().menuVisibilitySequence>=closing.overlaySnapshot().menuContentSequence);
  long closedRevision=closing.overlaySnapshot().menuRevision;closing.event(139,new int[]{0},null,null);
  check("idle repeated close does not create revisions",closing.overlaySnapshot().menuRevision==closedRevision);
  closing.event(1019,new int[]{46,3,2,129,4,117},null,null);closing.event(136,null,null,new String[]{"TREBLE +1"});
  closing.event(1019,new int[]{46,3,2,129,4,117},null,null);
  check("repeated raw BSJ close closes newer content",closing.overlaySnapshot().menuVisibilitySequence>=closing.overlaySnapshot().menuContentSequence);
  closing.event(139,new int[]{1},null,null);
  check("stale logical show does not override raw close",Integer.valueOf(0).equals(closing.overlaySnapshot().displayNumber(139)));
  for(int band:new int[]{3,4,5}){
   ProbeState.Snapshot am=bandPage(band,"SVC HIGH");m=MenuListModel.from(am);
   check("AM has no extra source submenu "+band,m.items.size()==8&&!m.items.contains("RDS SETTING")&&!m.items.contains("PLAY MODE")&&m.items.get(7).equals("CLOCK")&&m.selected==6);
   for(String forbidden:new String[]{"RDS SETTING","2 REGIONAL OFF","PLAY MODE","REPEAT ONE TRK"}){am=bandPage(band,forbidden);m=MenuListModel.from(am);check("AM rejects stale submenu "+band+" "+forbidden,m.title.equals("AUDIO SETTINGS")&&m.items.size()==8&&!m.items.toString().contains("REGIONAL")&&!m.items.toString().contains("REPEAT"));}
  }
  for(int band:new int[]{0,1,2}){ProbeState.Snapshot fm=bandPage(band,"SVC HIGH");check("FM keeps RDS "+band,MenuListModel.from(fm).items.contains("RDS SETTING"));}
  for(int auxSource:new int[]{3,4}){ProbeState.Snapshot aux=page(auxSource,"CLOCK:12H");aux.auxSource=auxSource;m=MenuListModel.from(aux);check("AUX has no media submenu "+auxSource,m.items.size()==8&&!m.items.contains("PLAY MODE")&&!m.items.contains("RDS SETTING")&&m.selected==7);}
  for(int source:new int[]{1,2,3,4}){ProbeState.Snapshot heading=page(source,"SVC HIGH");if(source==4)heading.mediaDevice=ProbeState.MediaDevice.USB;String expected=source==1?"Radio":source==2?"CD":source==3?"AUX":"USB";check("mode settings header "+expected,MenuListModel.from(heading).heading(heading).startsWith(expected+" · "));}
  ProbeState.Snapshot ipodHeading=page(2,"SVC HIGH");ipodHeading.mediaDevice=ProbeState.MediaDevice.IPOD;check("iPod settings header on CD carrier",MenuListModel.from(ipodHeading).heading(ipodHeading).startsWith("iPod · "));
  ProbeState.Snapshot sharedHeading=page(4,"NORMAL PLAY");check("unknown USB subtype is honest in header",MenuListModel.from(sharedHeading).heading(sharedHeading).startsWith("USB/iPod · ")&&MenuListModel.from(sharedHeading).heading(sharedHeading).endsWith("PLAY MODE"));
  ProbeState.Snapshot manualHeading=page(2,"SVC HIGH");manualHeading.manualMediaDeviceHint=ProbeState.MediaDevice.IPOD;check("manual iPod menu heading",MenuListModel.from(manualHeading).heading(manualHeading).startsWith("iPod · "));manualHeading.manualMediaDeviceHint=ProbeState.MediaDevice.USB;check("manual USB menu heading",MenuListModel.from(manualHeading).heading(manualHeading).startsWith("USB · "));
  memory.clear();check("new connection has no invented clock default",memory.snapshot().isEmpty());return checks;
 }
}
