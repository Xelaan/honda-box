package com.crewbeat.hondabox;

import org.json.*;
import java.util.*;

/** Experimental correlations, never a verified decoder or source of vehicle UI values. */
public final class SignalCandidates {
    public static final String[] ENCODINGS={"u8","s8","BE16","sBE16","LE16","sLE16"};
    private static final double[] SCALES={1.0/256,.01,.05,.0625,.1,.25,.5,1,2,4,10,100};
    public static final int MAX_FIELDS=4096,MAX_RESULTS=40;
    private static final class Samples {
        final double median,min,max,mode;final int count,modeCount;
        Samples(List<BsjAnalyzer.Frame> frames,int offset,String encoding){
            int[] v=new int[frames.size()];Map<Integer,Integer> counts=new HashMap<>();int mc=0,mv=0;
            for(int i=0;i<v.length;i++){v[i]=BsjAnalyzer.decode(frames.get(i),offset,encoding);int n=counts.containsKey(v[i])?counts.get(v[i])+1:1;counts.put(v[i],n);if(n>mc){mc=n;mv=v[i];}}
            Arrays.sort(v);count=v.length;min=v[0];max=v[v.length-1];median=(v[(v.length-1)/2]+(double)v[v.length/2])/2;mode=mv;modeCount=mc;
        }
    }
    private static Map<String,List<BsjAnalyzer.Frame>> groups(BsjAnalyzer.Window w){
        Map<String,List<BsjAnalyzer.Frame>> map=new LinkedHashMap<>();
        for(BsjAnalyzer.Frame f:w.frames)if(f.type!=0x11&&f.at>=w.at&&f.at<=w.end()){List<BsjAnalyzer.Frame> list=map.get(f.key());if(list==null){list=new ArrayList<>();map.put(f.key(),list);}list.add(f);}return map;
    }
    public static JSONArray find(BsjAnalyzer.Snapshot s,long now)throws Exception {
        LinkedHashMap<String,BsjAnalyzer.Window> latest=new LinkedHashMap<>();
        // A newer pending/interrupted experiment supersedes an old result for that label.
        for(BsjAnalyzer.Window w:s.windows)if(w.segment==s.segment&&w.session==s.session&&w.profile==6291908)latest.put(w.label,w);
        Map<String,Map<String,List<BsjAnalyzer.Frame>>> indexed=new HashMap<>();
        for(Map.Entry<String,BsjAnalyzer.Window> e:latest.entrySet())if(e.getValue().usable(now))indexed.put(e.getKey(),groups(e.getValue()));
        JSONArray result=new JSONArray();int[] budget={MAX_FIELDS};
        detect("RPM",new String[]{"RPM_1500","RPM_2000","RPM_3000"},new double[]{1500,2000,3000},3,indexed,latest,s,result,budget);
        detect("Steering",new String[]{"STEER_CENTER","STEER_LEFT_90","STEER_RIGHT_90"},new double[]{0,-90,90},3,indexed,latest,s,result,budget);
        detect("Gear",new String[]{"GEAR_P","GEAR_R","GEAR_N","GEAR_D","GEAR_S"},null,3,indexed,latest,s,result,budget);
        detect("Speed",new String[]{"SPEED_0","SPEED_10","SPEED_20","SPEED_30"},new double[]{0,10,20,30},3,indexed,latest,s,result,budget);
        return result;
    }
    private static void detect(String kind,String[] requested,double[] target,int required,
        Map<String,Map<String,List<BsjAnalyzer.Frame>>> index,Map<String,BsjAnalyzer.Window> windows,BsjAnalyzer.Snapshot snapshot,JSONArray out,int[] budget)throws Exception {
        List<String> labels=new ArrayList<>();List<Double> expected=new ArrayList<>();
        for(int i=0;i<requested.length;i++)if(index.containsKey(requested[i])){labels.add(requested[i]);if(target!=null)expected.add(target[i]);}
        if(labels.size()<required)return;
        for(String key:index.get(labels.get(0)).keySet()){
            List<List<BsjAnalyzer.Frame>> series=new ArrayList<>();boolean enough=true;
            for(String label:labels){List<BsjAnalyzer.Frame> list=index.get(label).get(key);if(list==null||list.size()<3){enough=false;break;}series.add(list);}if(!enough)continue;
            BsjAnalyzer.Frame exemplar=series.get(0).get(0);
            for(int offset=0;offset<exemplar.length();offset++)for(String encoding:ENCODINGS){
                if(encoding.endsWith("16")&&offset+1>=exemplar.length())continue;if(budget[0]--<=0)return;
                Samples[] values=new Samples[labels.size()];for(int i=0;i<values.length;i++)values[i]=new Samples(series.get(i),offset,encoding);
                double scale=Double.NaN,error=0,center=0;String orientation="";boolean match=false;
                if(kind.equals("Gear")){
                    Set<Double> distinct=new HashSet<>();match=true;
                    for(Samples v:values)if(v.modeCount<Math.ceil(v.count*.9)||!distinct.add(v.mode)){match=false;break;}
                }else if(kind.equals("Steering")){
                    center=values[0].median;double l=values[1].median-center,r=values[2].median-center;
                    if(l*r<0&&Math.abs(Math.abs(l)-Math.abs(r))<=Math.max(Math.abs(l),Math.abs(r))*.25){
                        for(double k:SCALES){double e=Math.max(Math.abs(Math.abs(l)*k-90),Math.abs(Math.abs(r)*k-90));
                            if(e<=15&&stable(values,k,10)){scale=k;error=e/90;match=true;orientation=l<0?"left negative":"left positive";break;}}
                    }
                }else {
                    boolean monotonic=true;for(int i=1;i<values.length;i++)if(values[i].median<=values[i-1].median)monotonic=false;
                    if(monotonic)for(double k:SCALES){double e=0;boolean fits=true;
                        for(int i=0;i<values.length;i++){double tolerance=kind.equals("RPM")?Math.max(150,expected.get(i)*.15):Math.max(2,expected.get(i)*.15);
                            double delta=Math.abs(values[i].median*k-expected.get(i));if(delta>tolerance)fits=false;e=Math.max(e,delta/Math.max(1,expected.get(i)));}
                        if(fits&&stable(values,k,kind.equals("RPM")?250:3)){scale=k;error=e;match=true;break;}}
                }
                if(!match)continue;
                JSONArray evidence=new JSONArray();for(int i=0;i<labels.size();i++){Samples v=values[i];JSONObject item=new JSONObject().put("label",labels.get(i)).put("markerId",windows.get(labels.get(i)).id)
                    .put("samples",v.count).put("rawMedian",v.median).put("rawMin",v.min).put("rawMax",v.max).put("rawMode",v.mode).put("modeSamples",v.modeCount);
                    if(!Double.isNaN(scale))item.put("scaledMedian",(v.median-center)*scale);evidence.put(item);}
                if(out.length()<MAX_RESULTS)out.put(new JSONObject().put("status","candidate_unverified").put("signal",kind).put("type",exemplar.type).put("length",exemplar.length())
                    .put("offset",offset).put("width",encoding.endsWith("16")?2:1).put("encoding",encoding).put("scale",Double.isNaN(scale)?JSONObject.NULL:scale)
                    .put("centerOffset",center).put("orientation",orientation).put("relativeFitError",error).put("session",snapshot.session).put("captureSegment",snapshot.segment)
                    .put("evidence",evidence).put("note","Correlation with manual labels; unrelated changes can match. Post-marker samples only; repeat isolated experiments to verify."));
            }
        }
    }
    private static boolean stable(Samples[] values,double scale,double tolerance){for(Samples v:values)if((v.max-v.min)*scale>tolerance)return false;return true;}
}
