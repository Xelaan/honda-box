package com.crewbeat.hondabox;

/** AUX transition policy. No playback commands on connection, wake or settings changes alone. */
public final class AuxPlaybackPolicy {
    public enum ReturnAction { ALWAYS_PLAY, IF_AUTO_PAUSED, NOTHING }
    public interface Player {
        boolean isPlaying();
        boolean pause();
        boolean play();
    }
    private static final long PAUSE_TIMEOUT_MS=2000;
    private long session=-1,pauseAt;private int previousSource,auxSource;
    private boolean baseline,pausePending,autoPaused,resumePending;
    public void reset(){baseline=false;forgetPause();}
    private void forgetPause(){pausePending=false;autoPaused=false;resumePending=false;}
    public void update(long session,boolean valid,Integer source,int aux,boolean pauseOnLeave,ReturnAction onReturn,long now,Player player){
        if(!valid||source==null||source<1||source>16){reset();return;}
        if(!baseline||this.session!=session||auxSource!=aux){
            reset();baseline=true;this.session=session;auxSource=aux;previousSource=source;return;
        }
        if(pausePending){
            if(!player.isPlaying()){pausePending=false;autoPaused=true;}
            else if(now-pauseAt>=PAUSE_TIMEOUT_MS)forgetPause();
        }else if(autoPaused&&source!=aux&&player.isPlaying()){
            // Playback restarted independently while away: a later pause is no longer ours.
            forgetPause();
        }
        if(previousSource!=source){
            int old=previousSource;previousSource=source;
            if(old==aux){
                forgetPause();
                if(pauseOnLeave&&player.isPlaying()&&player.pause()){pausePending=true;pauseAt=now;}
            }else if(source==aux){
                if(onReturn==ReturnAction.ALWAYS_PLAY){forgetPause();player.play();}
                else if(onReturn==ReturnAction.IF_AUTO_PAUSED){
                    if(autoPaused){forgetPause();if(!player.isPlaying())player.play();}
                    else if(pausePending)resumePending=true;
                }else forgetPause();
            }
        }
        // A quick return can beat the asynchronous PAUSE acknowledgement.
        if(resumePending&&source==aux){
            if(onReturn==ReturnAction.NOTHING)forgetPause();
            else if(onReturn==ReturnAction.ALWAYS_PLAY){forgetPause();player.play();}
            else if(autoPaused){forgetPause();if(!player.isPlaying())player.play();}
            else if(!pausePending)resumePending=false;
        }
    }
}
