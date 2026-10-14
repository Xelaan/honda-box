package com.crewbeat.hondabox;

import android.app.job.*;
import android.content.*;
import android.os.*;

/** Read-only service recovery. No exact alarms, wake locks or persisted jobs across reboot. */
public final class BackgroundRecovery {
    public static final String ACC_ON="com.fyt.boot.ACCON",ACC_OFF="com.fyt.boot.ACCOFF";
    public static final String REASON="automaticStartReason";
    public static final int JOB_ID=0x4842;
    private static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("background",0);}
    public static boolean enabled(Context c){return prefs(c).getBoolean("recovery",true);}
    public static void enabled(Context c,boolean on){prefs(c).edit().putBoolean("recovery",on).apply();schedule(c);}
    public static boolean awake(Context c){return prefs(c).getBoolean("accAwake",true);}
    public static void notePower(Context c,boolean awake){prefs(c).edit().putBoolean("accAwake",awake).putLong(awake?"lastWakeMs":"lastSleepMs",System.currentTimeMillis()).commit();schedule(c);}
    public static boolean mayRecover(Context c){
        PowerManager power=(PowerManager)c.getSystemService(Context.POWER_SERVICE);
        return enabled(c)&&BackgroundSettings.wanted(c)&&awake(c)&&power!=null&&power.isInteractive();
    }
    public static boolean mayStart(Context c,String reason){
        UserManager user=(UserManager)c.getSystemService(Context.USER_SERVICE);
        if(user!=null&&!user.isUserUnlocked())return false;
        return "watchdog".equals(reason)?mayRecover(c):BackgroundSettings.mayAutoStart(c);
    }
    public static boolean start(Context c,String reason){
        if(!mayStart(c,reason))return false;
        Intent intent=new Intent(c,ProbeService.class).putExtra("backgroundStart",true).putExtra(REASON,reason);
        try{if(Build.VERSION.SDK_INT>=26)c.startForegroundService(intent);else c.startService(intent);return true;}
        catch(RuntimeException e){ProbeState.INSTANCE.log("AUTOMATIC START "+reason+": "+e);return false;}
    }
    public static void schedule(Context c){
        JobScheduler scheduler=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(scheduler==null)return;
        if(!enabled(c)||!BackgroundSettings.wanted(c)||!awake(c)){scheduler.cancel(JOB_ID);return;}
        if(scheduler.getPendingJob(JOB_ID)!=null)return;
        // A new cold boot is governed by the user's boot-autostart setting, not an old job.
        JobInfo job=new JobInfo.Builder(JOB_ID,new ComponentName(c,ServiceRecoveryJob.class)).setPeriodic(15*60*1000L,5*60*1000L).build();
        try{if(scheduler.schedule(job)!=JobScheduler.RESULT_SUCCESS)ProbeState.INSTANCE.log("RECOVERY: Android did not accept periodic job");}
        catch(RuntimeException e){ProbeState.INSTANCE.log("RECOVERY SCHEDULE: "+e);}
    }
    public static boolean check(Context c){
        if(!enabled(c)||!BackgroundSettings.wanted(c)){schedule(c);return false;}
        if(ProbeState.INSTANCE.overlaySnapshot().running||!mayRecover(c))return false;
        return start(c,"watchdog");
    }
    private BackgroundRecovery(){}
}
