package com.crewbeat.hondabox;
import java.util.*;
import java.util.regex.*;
/** Only observed indexed rows. A new navigation page or reused index starts a new list. */
public final class MenuBrowser {
    private final TreeMap<Integer,String> rows=new TreeMap<>();
    private String context="";private Integer source;
    private static final Pattern ITEM=Pattern.compile("^(\\d{1,3})\\s*:\\s*(.+)$");
    public void clear(){rows.clear();context="";source=null;}
    public void accept(Integer next,String[] page){
        if(!Objects.equals(source,next)){clear();source=next;}
        boolean indexed=false;for(String raw:page)if(ITEM.matcher(raw.trim()).matches())indexed=true;
        if(!indexed){String key=String.join("|",page).trim();if(!key.equals(context)){rows.clear();context=key;}return;}
        for(String raw:page){Matcher m=ITEM.matcher(raw.trim());if(!m.matches())continue;int index=Integer.parseInt(m.group(1));String old=rows.get(index);if(old!=null&&!old.equals(raw))rows.clear();rows.put(index,raw);while(rows.size()>128)rows.pollFirstEntry();}
    }
    public List<String> snapshot(String[] page){boolean indexed=false;for(String raw:page)if(ITEM.matcher(raw.trim()).matches())indexed=true;return indexed?new ArrayList<>(rows.values()):new ArrayList<>();}
}
