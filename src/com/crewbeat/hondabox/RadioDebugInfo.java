package com.crewbeat.hondabox;
import java.util.Locale;
/** Last received radio/text values; no RDS inference or commands. */
public final class RadioDebugInfo {
 private RadioDebugInfo(){}
 private static final int[] LABELS={R.string.debug_radio_band,R.string.debug_radio_preset,R.string.debug_radio_scan,R.string.debug_radio_stereo,R.string.debug_radio_frequency};
 private static String raw(ProbeState.Value v,long source){
  if(v==null)return "—";String data;
  if(v.ints!=null)data=java.util.Arrays.toString(java.util.Arrays.copyOf(v.ints,Math.min(8,v.ints.length)));
  else if(v.strings!=null){StringBuilder text=new StringBuilder();for(int i=0;i<Math.min(3,v.strings.length);i++){if(i>0)text.append(" / ");String part=v.strings[i]==null?"":v.strings[i];part=part.substring(0,Math.min(160,part.length())).replace("\0","\\0").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t");text.append('"').append(part).append('"');}data=text.length()==0?"[]":text.toString();}else data="—";
  return data+" @"+String.format(Locale.US,"%tT",v.at)+(source>0&&v.sequence<=source?" · "+I18n.t(R.string.debug_radio_before_source):"");
 }
 private static String decoded(ProbeState.Snapshot s,int code){
  if(!s.connected||!Integer.valueOf(6291908).equals(s.number(1000))||!s.fresh(code))return "";Integer n=s.displayNumber(code);if(n==null)return "";
  if(code==115){String[] bands={"FM","FM1","FM2","AM","AM1 / MW","AM2 / LW"};return n>=0&&n<bands.length?bands[n]:"";}
  if(code==116)return n>=1&&n<=6?"#"+n:n==0?I18n.t(R.string.debug_radio_no_preset):"";
  if(code==117){String[] states={"—","Normal","TUNE+","TUNE−","SEEK+","SEEK−","SCAN","Auto Search"};return n>=1&&n<states.length?states[n]:"";}
  if(code==118)return n==0?"ST OFF":n==1?"ST ON":"";
  if(!s.fresh(115)||AudioMode.reported(s)!=AudioMode.RADIO)return "";Integer band=s.displayNumber(115);String f=RadioFrequency.value(band,n,true),unit=RadioFrequency.unit(band);if(f.equals("—"))return "";
  return f+" "+(unit.equals("kHz")?I18n.t(R.string.radio_khz):unit.equals("MHz")?I18n.t(R.string.radio_mhz):unit);
 }
 public static String fields(ProbeState.Snapshot s){
  StringBuilder out=new StringBuilder(I18n.t(s.connected?R.string.debug_radio_connected:R.string.debug_radio_history));out.append('\n');
  for(int code=115;code<=125;code++){String label=code<=119?I18n.t(LABELS[code-115]):I18n.t(R.string.debug_radio_saved)+" "+(code-119);out.append('[').append(code).append("] ").append(label).append('\n');out.append("  CAN ").append(raw(s.values.get(code),s.sourceSequence)).append("  |  BSJ ").append(raw(s.bsjValues.get(code),s.sourceSequence));String value=decoded(s,code);if(!value.isEmpty())out.append("  → ").append(value);out.append('\n');}return out.toString().trim();
 }
 public static String textChannels(ProbeState.Snapshot s){
  StringBuilder out=new StringBuilder();int[] codes={103,104,105,135,136,137},names={R.string.usb_title_track,R.string.usb_title_album,R.string.usb_title_artist};
  for(int i=0;i<codes.length;i++){int code=codes[i];out.append('[').append(code).append("] ").append(i<3?I18n.t(names[i]):I18n.t(R.string.debug_radio_menu_row)+" "+(i-2)).append('\n');out.append("  CAN ").append(raw(s.values.get(code),s.sourceSequence)).append("  |  BSJ ").append(raw(s.bsjValues.get(code),s.sourceSequence)).append('\n');}
  out.append("BSJ 07 · ").append(I18n.t(R.string.usb_title_unknown)).append(": ").append(raw(s.bsjTitleField,s.sourceSequence)).append('\n');for(int kind:new int[]{4,5,6,7,8}){ProbeState.Value v=kind==4?s.bsjFolder:kind==5?s.bsjFilename:s.bsjExtraTexts.get(kind);out.append("BSJ 07/").append(String.format(Locale.US,"%02X",kind)).append(": ").append(raw(v,s.sourceSequence)).append('\n');}return out.toString().trim();
 }
}
