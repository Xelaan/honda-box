package dev.alex.fyt.probe;

import java.util.regex.Pattern;

/** Each OEM text packet is one display value; truncation never implies continuation. */
public final class MediaText {
    private String current="";
    private static final Pattern MOJIBAKE=Pattern.compile("(?:[РС][\\u0400-\\u045f\\u00a0-\\u00ff\\u2000-\\u2122]){2,}");
    public static String display(String raw){
        if(raw==null)return "—";
        String s=raw.replace('\ufffd','□').replaceAll("[\\p{Cntrl}]"," ").trim();
        s=s.replaceAll("[ÐÑ][^\\x00-\\x7f]","□");
        java.util.regex.Matcher m=MOJIBAKE.matcher(s);StringBuffer out=new StringBuffer();
        while(m.find()){StringBuilder boxes=new StringBuilder();for(int i=0;i<m.group().length()/2;i++)boxes.append('□');m.appendReplacement(out,boxes.toString());}m.appendTail(out);s=out.toString();
        return s.endsWith(">")?s.substring(0,s.length()-1).trim()+"…":s;
    }
    public void clear(){current="";}
    public String accept(String raw){
        if(raw==null||raw.trim().isEmpty()){clear();return "";}
        current=display(raw);return current;
    }
}
