package dev.alex.fyt.probe;
import java.util.Locale;

/** Honda Accord's confirmed FM/FM2/AM1/AM2 banks, distinct from generic MW/LW mappings. */
public final class RadioFrequency {
    public static String unit(Integer band){return band==null||band<0||band>5?"":band<=2?"MHz":"kHz";}
    public static String value(Integer band,Integer index,boolean accord){
        if(band==null||index==null||index<=0||band<0||band>5)return "—";
        if(band<=2)return String.format(Locale.US,"%.2f",(8750+(index-1)*5)/100f);
        return String.valueOf(!accord&&band==4?153+(index-1)*3:531+(index-1)*9);
    }
}
