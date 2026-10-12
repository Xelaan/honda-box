package dev.alex.fyt.probe;
import android.app.*;
import android.os.Bundle;
public final class ProbeApplication extends Application {
    private static int resumed;
    public static boolean foreground(){return resumed>0;}
    @Override public void onCreate(){super.onCreate();I18n.init(this);registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks(){
        public void onActivityResumed(Activity a){resumed++;}public void onActivityPaused(Activity a){resumed=Math.max(0,resumed-1);}
        public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}
    });}
}
