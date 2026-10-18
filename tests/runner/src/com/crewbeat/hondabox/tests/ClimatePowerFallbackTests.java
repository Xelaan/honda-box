package com.crewbeat.hondabox.tests;

import android.app.Instrumentation;
import android.os.Bundle;
import com.crewbeat.hondabox.*;

/** Shared power/compact eligibility, gated by optional Accord interpretation. */
public final class ClimatePowerFallbackTests extends Instrumentation {
    private int checks;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    private ProbeState model(int profile,Integer power,Integer fan,boolean override){
        ProbeState state=new ProbeState();state.processAsHondaAccord(override);state.newSession();state.running(true);state.status("test","synthetic climate",true);state.event(1000,new int[]{profile},null,null);
        if(power!=null)state.event(10,new int[]{power},null,null);if(fan!=null)state.event(21,new int[]{fan},null,null);return state;
    }
    @Override public void onStart(){Bundle result=new Bundle();int outcome=0;
        try{
            ClimatePopupView[] popup={null};runOnMainSync(()->popup[0]=new ClimatePopupView(getTargetContext()));
            java.lang.reflect.Method flag=ClimatePopupView.class.getDeclaredMethod("flag",int.class);flag.setAccessible(true);
            for(int profile:new int[]{6291908,3604932,6357444,12321220,9175479,123456,0,-1})for(boolean override:new boolean[]{false,true}){
                boolean accord=profile>0&&(override||profile==6291908);
                for(Integer power:new Integer[]{null,0,1})for(Integer fan:new Integer[]{null,-1,0,1,4,7,8}){
                    ProbeState state=model(profile,power,fan,override);ProbeState.Snapshot s=state.snapshot();
                    Integer expected=accord&&fan!=null&&fan>=0&&fan<=7?Integer.valueOf(fan==0?0:1):power;
                    check(java.util.Objects.equals(expected,s.climatePower()),"power profile="+profile+" override="+override+" raw="+power+" fan="+fan);
                    check(s.number(1000)==profile,"actual profile preserved");check(java.util.Objects.equals(power,s.number(10)),"raw power preserved");
                    check(ClimateStripOverlay.active(s)==(s.honda()&&Integer.valueOf(1).equals(expected)),"compact eligibility follows shared rule");
                    if(accord){String[] shown={null};runOnMainSync(()->{try{popup[0].content(s,false);shown[0]=(String)flag.invoke(popup[0],10);}catch(Exception e){throw new RuntimeException(e);}});
                        check((expected==null?"—":I18n.t(expected==0?R.string.ui_off:R.string.ui_on)).equals(shown[0]),"large power indicator follows same rule");}
                    state.status("offline","",false);check(!ClimateStripOverlay.active(state.snapshot()),"disconnected hidden");
                    state.status("test","",true);state.running(false);check(!ClimateStripOverlay.active(state.snapshot()),"stopped hidden");
                }
            }
            result.putString("result","PASS: "+checks+" climate power/compact/large panel checks; opt-in override and original defaults");
        }catch(Throwable e){outcome=1;result.putString("failure",android.util.Log.getStackTraceString(e));}
        finish(outcome,result);
    }
}
