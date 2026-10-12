package dev.alex.fyt.probe.tests;
import dev.alex.fyt.probe.*;
import java.nio.charset.StandardCharsets;

final class BoxTests {
    private int checks;
    private void check(String label,boolean ok){if(!ok)throw new AssertionError(label);checks++;}
    private void raw(ProbeState state,int type,int... p){int[] bytes=new int[p.length+4];bytes[0]=46;bytes[1]=type;bytes[2]=p.length;int sum=type+p.length;for(int i=0;i<p.length;i++){bytes[i+3]=p[i];sum+=p[i];}bytes[bytes.length-1]=(~sum)&255;state.event(1019,bytes,null,null);}
    private void text(ProbeState s,int kind,String value){byte[] b=value.getBytes(StandardCharsets.UTF_8);int[] p=new int[b.length+2];p[0]=kind;for(int i=0;i<b.length;i++)p[i+1]=b[i]&255;raw(s,7,p);}
    int run(){
        check("FM units",RadioFrequency.unit(2).equals("MHz"));check("AM1 units",RadioFrequency.unit(4).equals("kHz"));
        check("FM physical index",RadioFrequency.value(1,299,true).equals("102.40"));
        check("Accord AM1 uses medium wave",RadioFrequency.value(4,22,true).equals("720"));check("Accord AM2 uses medium wave",RadioFrequency.value(5,9,true).equals("603"));
        check("other-profile MW mapping preserved",RadioFrequency.value(4,22,false).equals("216"));check("unknown index",RadioFrequency.value(1,0,true).equals("—"));
        MediaText parts=new MediaText();check("first partial marked",parts.accept("Dazed and Confu>").equals("Dazed and Confu…"));
        check("overlap continuation joins",parts.accept("Confused").equals("Dazed and Confused"));check("duplicate stable",parts.accept("Confused").equals("Dazed and Confused"));
        parts.clear();parts.accept("Complete title>");check("complete repeat removes truncation marker",parts.accept("Complete title").equals("Complete title"));
        parts.clear();parts.accept("Led Zeppelin (R>");check("ambiguous rotation retained separately",parts.accept("Babe I'm Gonna >").equals("Led Zeppelin (R / Babe I'm Gonna…"));
        check("explicit empty clears",parts.accept("").isEmpty());check("valid Russian preserved",MediaText.display("Русский исполнитель").equals("Русский исполнитель"));
        check("replacement box",MediaText.display("Song � text").equals("Song □ text"));check("Latin mojibake hidden",MediaText.display("ÐŸÐµÑ�Ð½Ñ�").indexOf('Ð')<0);
        check("CP1251 mojibake hidden",MediaText.display("РџРµСЃРЅСЏ").indexOf('Р')<0);
        ProbeState s=new ProbeState();s.newSession();s.running(true);s.status("test","",true);s.event(1000,new int[]{6291908},null,null);raw(s,3,0x81,7);raw(s,5,1,0,0,0,0,1,0,1);
        text(s,5,"Dazed and Confu>");text(s,5,"Confused");check("CD continued title rendered",s.snapshot().cdTitle().equals("Dazed and Confused"));
        raw(s,5,1,0,0,0,0,1,0,2);check("same track elapsed update keeps title",s.snapshot().cdTitle().equals("Dazed and Confused"));
        raw(s,5,1,0,0,0,0,2,0,0);check("next track clears previous fragments",s.snapshot().cdTitle().equals("—"));
        text(s,5,"New track>");raw(s,5,2,0,0,0,0,2,0,0);check("disc change with same track clears fragments",s.snapshot().cdTitle().equals("—"));text(s,5,"Other disc>");raw(s,3,0x83,7);check("source change clears fragments",s.snapshot().cdTitle().equals("—"));
        check("external source routes shared player",AudioMode.reported(s.snapshot())==AudioMode.USB);raw(s,7,0,183,183,32,65,0);check("BSJ lost Russian is boxes",s.snapshot().mediaPrimary().equals("□□ A"));
        return checks;
    }
}
