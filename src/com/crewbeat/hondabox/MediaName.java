package com.crewbeat.hondabox;

import java.util.regex.*;

/** Optional owner-requested display heuristic, subordinate to actual title/artist fields. */
public final class MediaName {
    public final String artist,title;
    private MediaName(String artist,String title){this.artist=artist;this.title=title;}
    public static MediaName parse(String text){
        if(text==null||text.equals("—")||(text.indexOf('·')>=0||text.indexOf('□')>=0||text.indexOf('…')>=0||text.contains(" / "))||text.indexOf('�')>=0||text.endsWith(">"))return null;
        String value=text.trim().replaceFirst("(?i)\\.(mp3|flac|wav|m4a|aac|ogg|wma)$","");
        Matcher m=Pattern.compile("\\s+[-–—]\\s+").matcher(value);if(!m.find())return null;int start=m.start(),end=m.end();if(m.find())return null;
        String artist=value.substring(0,start).trim(),title=value.substring(end).trim();
        return artist.isEmpty()||title.isEmpty()?null:new MediaName(artist,title);
    }
}
