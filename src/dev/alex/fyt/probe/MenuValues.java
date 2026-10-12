package dev.alex.fyt.probe;
import java.util.*;
/** Last actually received settings in this connection; never supplies default values. */
public final class MenuValues {
    public static final String[] AUDIO={"BASS","TREBLE","FADER","BALANCE","CENTER","SUBW","SVC","CLOCK"};
    public static final String[] RDS={"AF","REGIONAL","PS","AUTO TP","NEWS"};
    private final Map<String,String> values=new LinkedHashMap<>();
    public void clear(){values.clear();}
    public void accept(Integer source,String[] rows){for(String row:rows){String label=MenuListModel.label(row);for(String key:AUDIO)save("",key,label);if(Integer.valueOf(1).equals(source))for(String key:RDS)save("RDS:",key,label);}}
    private void save(String prefix,String key,String label){if(label.startsWith(key+" ")||label.startsWith(key+":")){String value=label.substring(key.length()).replaceFirst("^\\s*:\\s*","").trim();if(!value.isEmpty())values.put(prefix+key,key.equals("CLOCK")?"CLOCK : "+value:key+" "+value);}}
    public Map<String,String> snapshot(){return new LinkedHashMap<>(values);}
}
