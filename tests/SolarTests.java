import dev.alex.fyt.probe.SolarTimes;
import java.util.*;
import java.text.*;

public final class SolarTests {
    static int checks;
    static void check(String name,boolean valid){if(!valid)throw new AssertionError(name);checks++;}
    static long instant(String date,String zone)throws Exception{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US);f.setTimeZone(TimeZone.getTimeZone(zone));return f.parse(date).getTime();}
    static void reference(String name,String date,String zone,double lat,double lon,String rise,String set)throws Exception{
        TimeZone z=TimeZone.getTimeZone(zone);long noon=instant(date+" 12:00",zone);SolarTimes.Result r=SolarTimes.calculate(noon,lat,lon,z);
        check(name+" sunrise published reference",Math.abs(r.sunrise-instant(date+" "+rise,zone))<180000);
        check(name+" sunset published reference",Math.abs(r.sunset-instant(date+" "+set,zone))<180000);
        check(name+" noon day",r.day);check(name+" midnight night",!SolarTimes.calculate(instant(date+" 00:00",zone),lat,lon,z).day);
        check(name+" immediately before sunrise",!SolarTimes.calculate(r.sunrise-1,lat,lon,z).day);
        check(name+" exact sunrise",SolarTimes.calculate(r.sunrise,lat,lon,z).day);
        check(name+" immediately before sunset",SolarTimes.calculate(r.sunset-1,lat,lon,z).day);
        check(name+" exact sunset",!SolarTimes.calculate(r.sunset,lat,lon,z).day);
        check(name+" next switch at sunset",r.next==r.sunset);
    }
    public static void main(String[] args)throws Exception {
        // Independent published references, not values produced by our algorithm.
        // Timeanddate New York October2026; Royal Astronomical Society of NZ seasonal table.
        reference("NY October", "2026-10-07","America/New_York",40.7128,-74.006,"06:58","18:28");
        reference("Auckland Jan", "2026-01-01","Pacific/Auckland",-36.8485,174.7633,"06:05","20:43");
        reference("Auckland winter", "2026-06-20","Pacific/Auckland",-36.8485,174.7633,"07:33","17:11");
        reference("Wellington Jan", "2026-01-01","Pacific/Auckland",-41.2865,174.7762,"05:51","20:57");
        reference("Christchurch Jan", "2026-01-01","Pacific/Auckland",-43.5321,172.6362,"05:52","21:14");
        reference("Dunedin winter", "2026-06-20","Pacific/Auckland",-45.8788,170.5028,"08:20","16:59");
        TimeZone oslo=TimeZone.getTimeZone("Europe/Oslo");
        SolarTimes.Result summer=SolarTimes.calculate(instant("2026-06-21 00:00","Europe/Oslo"),69.6492,18.9553,oslo);
        check("polar summer midnight day",summer.day&&summer.sunrise==0&&summer.sunset==0);
        SolarTimes.Result winter=SolarTimes.calculate(instant("2026-12-21 12:00","Europe/Oslo"),69.6492,18.9553,oslo);
        check("polar winter noon night",!winter.day&&winter.sunrise==0&&winter.sunset==0);
        long utcNoon=instant("2026-03-20 12:00","UTC");
        check("zero coordinates valid",SolarTimes.valid(0,0));check("zero longitude noon day",SolarTimes.calculate(utcNoon,0,0,TimeZone.getTimeZone("UTC")).day);
        check("NaN rejected",!SolarTimes.valid(Double.NaN,0));check("infinity rejected",!SolarTimes.valid(0,Double.POSITIVE_INFINITY));check("range rejected",!SolarTimes.valid(90.1,0));
        for(String date:new String[]{"2026-03-08","2026-11-01","2028-02-29"}) {
            long now=instant(date+" 12:00","America/New_York");SolarTimes.Result r=SolarTimes.calculate(now,40.7128,-74.006,TimeZone.getTimeZone("America/New_York"));
            check(date+" valid events",r.sunrise<now&&r.sunset>now&&r.day);
            check(date+" timezone change preserves actual day",SolarTimes.calculate(now,40.7128,-74.006,TimeZone.getTimeZone("GMT+05:30")).day==r.day);
        }
        for(double lat:new double[]{-90,90})check("pole well defined",SolarTimes.calculate(utcNoon,lat,0,TimeZone.getTimeZone("UTC"))!=null);
        System.out.println("PASS: "+checks+" solar checks");
    }
}
