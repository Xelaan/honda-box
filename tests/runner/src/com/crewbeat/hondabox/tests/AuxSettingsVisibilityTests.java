package com.crewbeat.hondabox.tests;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;import android.graphics.Rect;import com.crewbeat.hondabox.*;
/** Checks actual AUX controls in the measured Settings layout, beyond presence in the view tree. */
public final class AuxSettingsVisibilityTests extends Instrumentation {
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle out=new Bundle();int code=0;Activity a=null;
  try{a=startActivitySync(new Intent(getTargetContext(),SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();Activity activity=a;
   Throwable[] error={null};runOnMainSync(()->{try{View root=activity.findViewById(android.R.id.content);View pause=root.findViewWithTag("aux-pause-android"),button=root.findViewWithTag("aux-return-android");
    if(pause==null||button==null)throw new AssertionError("AUX controls missing");
    StringBuilder detail=new StringBuilder();View current=pause;while(current!=null){detail.append(current.getClass().getSimpleName()).append(" h=").append(current.getHeight()).append(" measured=").append(current.getMeasuredHeight()).append(" visibility=").append(current.getVisibility());ViewGroup.LayoutParams lp=current.getLayoutParams();if(lp!=null)detail.append(" lp=").append(lp.width).append(',').append(lp.height);detail.append('\n');current=current.getParent() instanceof View?(View)current.getParent():null;}
    out.putString("hierarchy",detail.toString());
    if(pause.getHeight()<=0||button.getHeight()<=0||((View)pause.getParent()).getHeight()<=0)throw new AssertionError("Controls have zero measured height");
    pause.requestRectangleOnScreen(new Rect(0,0,pause.getWidth(),button.getBottom()-pause.getTop()),true);
   }catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError(error[0]);SystemClock.sleep(200);out.putString("result","PASS: actual AUX controls have nonzero height in Settings");
  }catch(Throwable e){code=1;out.putString("failure",android.util.Log.getStackTraceString(e));}
  finally{if(a!=null){Activity activity=a;runOnMainSync(()->activity.finish());}}finish(code,out);
 }
}
