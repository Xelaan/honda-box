package com.crewbeat.hondabox;
import java.util.HashMap;
import java.util.Locale;
/** Tracks settled station/counter changes and same-field text when counters stop arriving. */
public final class TrackChange {
    private long session=-1,changedAt,textChangedAt,counterSeenAt,lastAnnouncedAt,counterSequence=-1;
    private Integer profile;private AudioMode mode;
    private String identity="",pendingKey="",pendingText="",lastAnnouncedText="";
    private boolean initialized,sourcePending,textPending,lastWasText;
    private final HashMap<Integer,String> titles=new HashMap<>();
    private static String normalized(String text){return text.replaceAll("[>…]+$","").replaceAll("\\s+"," ").trim().toLowerCase(Locale.ROOT);}
    private static boolean usable(String text){return text!=null&&!text.isEmpty()&&!text.equals("—")&&!text.equalsIgnoreCase("NO INFO");}
    private static boolean continuation(String old,String next){return old.equals(next)||old.startsWith(next)||next.startsWith(old);}
    private static boolean trackField(ProbeState.Snapshot s,Integer field){
        // Folder names are not track identities. iPod's generic file-name field
        // can carry album/artist text, so it cannot establish a new track either.
        return field!=null&&(field==1||field==7||(field==5&&s.mediaDevice!=ProbeState.MediaDevice.IPOD));
    }
    private static boolean titleAfterCounter(ProbeState.Snapshot s){
        ProbeState.Value track=s.reading(101),title=null;Integer field=s.currentTitleField();
        if(track==null)return true;
        if(field!=null){switch(field){
            case 0:title=s.reading(104);break;case 1:title=s.reading(103);break;case 3:title=s.reading(105);break;
            case 4:title=s.bsjFolder;break;case 5:title=s.bsjFilename;break;
            case 6:case 7:case 8:title=s.bsjExtraTexts.get(field);break;
        }}else{
            if(s.fresh(103)&&usable(s.text(103)))title=s.reading(103);
            else if(s.bsjFilename!=null)title=s.bsjFilename;
            else if(s.fresh(104))title=s.reading(104);
        }
        return title!=null&&title.sequence>=track.sequence;
    }
    private void clearPending(){pendingKey="";pendingText="";textPending=false;}
    public String update(ProbeState.Snapshot s,long now,boolean foreground){
        AudioMode current=AudioMode.reported(s);
        boolean reset=!s.connected||session!=s.sessionId||!java.util.Objects.equals(profile,s.number(1000));
        if(reset){session=s.sessionId;profile=s.number(1000);mode=current;identity="";titles.clear();clearPending();initialized=false;sourcePending=false;counterSeenAt=now;counterSequence=-1;lastAnnouncedText="";}
        if(!s.connected)return null;
        if(current!=mode){sourcePending=initialized;mode=current;identity="";titles.clear();clearPending();counterSeenAt=now;counterSequence=-1;}
        if(current==null){clearPending();return null;}
        // A cached standby display is not a newly playing track.
        if(s.usingStandbyText()||s.usingStandbyTrack()){clearPending();sourcePending=false;return null;}
        String key="",text="—",fallback="—";
        boolean radio=current==AudioMode.RADIO;
        if(radio){
            Integer band=s.displayNumber(115),frequency=s.displayNumber(119);
            String station=s.fresh(115)&&s.fresh(119)?RadioFrequency.value(band,frequency,Integer.valueOf(6291908).equals(s.number(1000))):"—";
            if(!station.equals("—")){
                key="radio:"+band+":"+frequency;String unit=RadioFrequency.unit(band);
                unit=unit.equals("kHz")?I18n.t(R.string.radio_khz):unit.equals("MHz")?I18n.t(R.string.radio_mhz):unit;
                Integer preset=s.fresh(116)?s.displayNumber(116):null;
                String presetLabel=preset!=null&&preset>=1&&preset<=6?"#"+preset+" · ":"";
                text=s.radioBand()+" · "+presetLabel+station+(unit.isEmpty()?"":" "+unit);
            }
        }else{
            String track=s.cdNumber(101);
            text=s.combinedMediaText();
            if(!track.equals("—")){key=current.name()+":"+s.cdNumber(113)+":"+track;fallback=I18n.t(R.string.ui_track)+track;}
        }
        if(!key.isEmpty()&&!key.equals(identity)){
            boolean announce=sourcePending||!identity.isEmpty();identity=key;counterSeenAt=now;initialized=true;sourcePending=false;
            if(!radio&&lastWasText&&now-lastAnnouncedAt<8000&&usable(text)&&normalized(text).equals(lastAnnouncedText))announce=false;
            titles.clear();clearPending();
            if(announce){pendingKey=key;changedAt=now;}
        }
        if(!radio&&!key.isEmpty()){
            ProbeState.Value counter=s.reading(101);
            if(counter!=null&&counter.sequence!=counterSequence){counterSequence=counter.sequence;counterSeenAt=now;}
        }
        Integer kind=radio?null:s.currentTitleField();
        // TITLE rotation is not a track change; each field gets its own baseline.
        // Ignore growing scrolling fragments of the same text.
        if(trackField(s,kind)&&usable(text)){
            String next=normalized(text),old=titles.put(kind,next);
            if(sourcePending){sourcePending=false;initialized=true;pendingKey="text:"+kind;textPending=true;changedAt=textChangedAt=now;pendingText=text;}
            else if(old!=null&&!continuation(old,next)&&pendingKey.isEmpty()){
                pendingKey="text:"+kind;textPending=true;changedAt=textChangedAt=now;pendingText=text;
            }else if(textPending&&pendingKey.equals("text:"+kind)&&!pendingText.equals(text)){pendingText=text;textChangedAt=now;}
            initialized=true;
        }
        if(foreground){clearPending();sourcePending=false;return null;}
        if(pendingKey.isEmpty())return null;
        String result=null;
        if(textPending){
            // A TITLE press may select an artist/album while a title is settling.
            // Announce only while that same track field is still selected.
            if(pendingKey.equals("text:"+kind)&&now-textChangedAt>=1200&&(key.isEmpty()||now-counterSeenAt>=2500))result=usable(text)?text:pendingText;
        }else if(now-changedAt>=600){
            if(usable(text)&&(radio||titleAfterCounter(s)))result=text;
            else if(now-changedAt>=1800&&usable(fallback))result=fallback;
        }
        if(result!=null){boolean fromText=textPending;clearPending();
            // A late counter confirming a text detection must not announce it twice.
            if(!radio&&!fromText&&lastWasText&&now-lastAnnouncedAt<8000&&normalized(result).equals(lastAnnouncedText))return null;
            lastAnnouncedText=normalized(result);lastAnnouncedAt=now;lastWasText=fromText;return result;
        }
        if(now-changedAt>8000)clearPending();
        return null;
    }
}
