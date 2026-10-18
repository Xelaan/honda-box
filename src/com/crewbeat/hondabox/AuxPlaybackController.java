package com.crewbeat.hondabox;

import android.content.Context;
import android.media.AudioManager;
import android.os.*;
import android.view.KeyEvent;

/** Android media keys only; independent of source-window/overlay settings and CAN commands. */
public final class AuxPlaybackController {
    private final Context context;private final Handler ui=new Handler(Looper.getMainLooper());
    private final AuxPlaybackPolicy policy=new AuxPlaybackPolicy();private final AuxPlaybackPolicy.Player player;
    private boolean started;
    public AuxPlaybackController(Context c){
        context=c;final AudioManager audio=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        player=new AuxPlaybackPolicy.Player(){
            public boolean isPlaying(){return audio!=null&&audio.isMusicActive();}
            private boolean send(int code){
                if(audio==null)return false;long now=SystemClock.uptimeMillis();
                audio.dispatchMediaKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,code,0));
                audio.dispatchMediaKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_UP,code,0));
                ProbeState.INSTANCE.log("ANDROID MEDIA: "+(code==KeyEvent.KEYCODE_MEDIA_PAUSE?"pause on leaving AUX":"play on returning to AUX"));return true;
            }
            public boolean pause(){return send(KeyEvent.KEYCODE_MEDIA_PAUSE);}
            public boolean play(){return send(KeyEvent.KEYCODE_MEDIA_PLAY);}
        };
    }
    private final Runnable tick=new Runnable(){public void run(){if(!started)return;refresh();ui.postDelayed(this,200);}};
    public void start(){if(started)return;started=true;policy.reset();ui.post(tick);}
    public void reset(){policy.reset();}
    private void refresh(){
        ProbeState.Snapshot s=ProbeState.INSTANCE.overlaySnapshot();
        boolean valid=s.running&&s.connected&&s.accord()
            &&BackgroundRecovery.awake(context)&&!Boolean.FALSE.equals(s.bsjAudioEnabled);
        try{policy.update(s.sessionId,valid,s.rawAudioSource(),s.auxSource,AuxPlaybackSettings.pauseOnLeave(context),AuxPlaybackSettings.onReturn(context),SystemClock.uptimeMillis(),player);}
        catch(RuntimeException error){policy.reset();ProbeState.INSTANCE.log("ANDROID MEDIA ERROR: "+error);}
    }
    public void close(){started=false;ui.removeCallbacks(tick);policy.reset();}
}
