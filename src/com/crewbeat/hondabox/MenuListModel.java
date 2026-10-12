package com.crewbeat.hondabox;
import java.util.*;
/** Static navigation choices, received values and observed browse entries only. */
public final class MenuListModel {
    public final String title;public final List<String> items;public final int selected;
    public String heading(ProbeState.Snapshot s){
        AudioMode mode=AudioMode.reported(s);String source=AudioMode.aux(s)?s.auxName:mode==AudioMode.RADIO?"Radio":mode==AudioMode.CD?"CD":mode==AudioMode.USB?(s.mediaDevice==ProbeState.MediaDevice.IPOD?"iPod":s.mediaDevice==ProbeState.MediaDevice.USB?"USB":"USB/iPod"):title.equals("iPod")?"iPod":"";
        String base=(source.isEmpty()?"":source+" · ")+I18n.t(R.string.ui_settings);
        return title.equals("PLAY MODE")||title.equals("RDS SETTINGS")?base+" · "+title:base;
    }
    private MenuListModel(String title,List<String> items,int selected){this.title=title;this.items=items;this.selected=selected;}
    public static String label(String raw){return raw==null?"":raw.replaceAll("[\\p{Cntrl}]"," ").trim().toUpperCase(Locale.ROOT).replaceFirst("^\\d{1,3}(?:\\s*:\\s*|\\s+)","").replaceFirst("\\s*[>…]\\s*$","").replace("CENTRE","CENTER").replaceAll("\\s+"," ").trim();}
    private static final String[] IPOD={"Playlists","Artists","Albums","Songs"};
    private static boolean matches(String row,String item){String a=label(row),b=label(item);return a.equals(b)||a.startsWith(b+" ")||a.startsWith(b+":");}
    private static boolean any(String[] rows,String[] items){for(String row:rows)for(String item:items)if(matches(row,item))return true;return false;}
    private static boolean extraMenu(String row){return any(new String[]{row},new String[]{"RDS SETTING","RDS SETTINGS","PLAY MODE","NORMAL PLAY","REPEAT","RANDOM","SCAN","SHUFFLE"})||any(new String[]{row},MenuValues.RDS);}
    public static MenuListModel from(ProbeState.Snapshot s){
        String[] rows={s.menuRow(0),s.menuRow(1),s.menuRow(2)};String[] choices=null;String title="CAR MENU";boolean radio=Integer.valueOf(1).equals(s.audioSource());
        Integer band=s.displayNumber(115);boolean noExtra=AudioMode.aux(s)||(radio&&band!=null&&band>=3&&band<=5);
        String[] settings=noExtra?new String[]{"BASS","TREBLE","FADER","BALANCE","CENTER","SUBW","SVC","CLOCK"}:new String[]{"BASS","TREBLE","FADER","BALANCE","CENTER","SUBW","SVC",radio?"RDS SETTING":"PLAY MODE","CLOCK"};
        boolean removed=false;if(noExtra)for(int i=0;i<rows.length;i++)if(extraMenu(rows[i])){rows[i]="";removed=true;}
        if(!radio&&any(rows,IPOD)){choices=IPOD;title="iPod";}
        else if(radio&&!noExtra&&any(rows,MenuValues.RDS)){title="RDS SETTINGS";choices=MenuValues.RDS;}
        else if(!radio&&!noExtra&&any(rows,new String[]{"NORMAL PLAY","REPEAT","RANDOM","SCAN","SHUFFLE"})){title="PLAY MODE";}
        else if(removed||any(rows,settings)||any(rows,new String[]{"AUDIO SETTINGS"})){choices=settings;title="AUDIO SETTINGS";}
        List<String> list=new ArrayList<>();int selected=-1;
        if(choices==null){
            for(String item:s.browseItems)if(!noExtra||!extraMenu(item))list.add(item);
            for(String row:rows)if(!row.isEmpty()&&!list.contains(row))list.add(row);
            if(!list.isEmpty())selected=rows[1].isEmpty()?0:list.indexOf(rows[1]);
        }else{list.addAll(Arrays.asList(choices));for(int i=0;i<choices.length;i++){String cached=s.menuValues.get((choices==MenuValues.RDS?"RDS:":"")+choices[i]);if(cached!=null)list.set(i,cached);}for(int r:new int[]{1,0,2})for(int i=0;i<choices.length;i++)if(matches(rows[r],choices[i])){if(selected==-1)selected=i;list.set(i,choices==IPOD?choices[i]:label(rows[r]).startsWith("CLOCK")?s.menuValues.getOrDefault("CLOCK",rows[r]):rows[r].replace("CENTRE","CENTER").replace("Centre","Center"));}}
        return new MenuListModel(title,list,selected);
    }
}
