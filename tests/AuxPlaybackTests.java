import com.crewbeat.hondabox.AuxPlaybackPolicy;
import static com.crewbeat.hondabox.AuxPlaybackPolicy.ReturnAction.*;

/** Observable playback effects across source sequences, including asynchronous/ignored PAUSE. */
public final class AuxPlaybackTests {
    static int checks;static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
    static final class Player implements AuxPlaybackPolicy.Player {
        boolean playing=true,acceptPause=true,async;int pauses,plays;
        public boolean isPlaying(){return playing;}
        public boolean pause(){pauses++;if(acceptPause&&!async)playing=false;return acceptPause;}
        public boolean play(){plays++;playing=true;return true;}
    }
    static final class Run {
        final AuxPlaybackPolicy policy=new AuxPlaybackPolicy();final Player player=new Player();
        long now=1,session=1;boolean valid=true,pause=true;int aux=3;AuxPlaybackPolicy.ReturnAction returning=IF_AUTO_PAUSED;
        void source(Integer source){now+=200;policy.update(session,valid,source,aux,pause,returning,now,player);}
        void leave(){source(3);source(1);}
    }
    public static void main(String[] args){
        for(int nonAux:new int[]{1,2,4}){
            Run r=new Run();r.source(3);check(r.player.pauses==0&&r.player.plays==0,"AUX baseline is silent");r.source(nonAux);
            check(r.player.pauses==1&&!r.player.playing,"leaving AUX pauses for source"+nonAux);r.source(nonAux);r.source(nonAux);
            check(r.player.pauses==1,"duplicates do not repeat PAUSE");r.source(3);check(r.player.plays==1&&r.player.playing,"default resumes own pause");r.source(3);check(r.player.plays==1,"duplicates do not repeat PLAY");
        }
        Run r=new Run();r.player.playing=false;r.leave();r.source(3);check(r.player.pauses==0&&r.player.plays==0,"user-paused media stays paused");
        r=new Run();r.pause=false;r.leave();r.source(3);check(r.player.pauses==0&&r.player.plays==0,"pause switch disabled does not claim playback");
        r=new Run();r.leave();r.source(2);r.source(4);check(r.player.pauses==1&&r.player.plays==0,"non-AUX changes retain pause without commands");r.source(3);check(r.player.plays==1,"return after several sources resumes once");
        for(AuxPlaybackPolicy.ReturnAction action:AuxPlaybackPolicy.ReturnAction.values()){
            r=new Run();r.returning=action;r.player.playing=false;r.source(1);r.source(3);
            check(r.player.plays==(action==ALWAYS_PLAY?1:0),"return mode with no own pause: "+action);
            r=new Run();r.returning=action;r.leave();r.source(1);r.source(3);
            check(r.player.pauses==1&&r.player.plays==(action==NOTHING?0:1),"return mode with own pause: "+action);
        }
        r=new Run();r.returning=ALWAYS_PLAY;r.pause=false;r.player.playing=false;r.leave();r.source(3);check(r.player.plays==1,"always play independent of pause option");
        r=new Run();r.returning=ALWAYS_PLAY;r.player.playing=false;r.source(3);r.source(3);check(r.player.plays==0,"always play still needs a transition");
        r=new Run();r.leave();r.source(1);r.player.playing=true;r.source(1);r.player.playing=false;r.source(1);r.source(3);check(r.player.plays==0,"independent resume cancels ownership of a later manual pause");
        r=new Run();r.leave();r.valid=false;r.source(1);r.valid=true;r.source(3);check(r.player.plays==0,"reconnect never resumes stale pause");
        r=new Run();r.leave();r.session++;r.source(3);check(r.player.plays==0,"new session never resumes stale pause");
        r=new Run();r.leave();r.aux=4;r.source(4);check(r.player.plays==0,"AUX remap rebaselines without playback");
        r=new Run();r.leave();r.source(0);r.source(3);check(r.player.plays==0,"unknown source does not manufacture a return");
        r=new Run();r.source(null);r.source(3);check(r.player.pauses==0&&r.player.plays==0,"no source baseline sends no commands");
        r=new Run();r.player.acceptPause=false;r.leave();r.player.playing=false;r.source(3);check(r.player.plays==0,"failed PAUSE cannot authorize default PLAY");
        r=new Run();r.player.async=true;r.leave();r.source(3);check(r.player.plays==0,"quick return waits for PAUSE acknowledgement");r.player.playing=false;r.source(3);check(r.player.plays==1,"late acknowledgement completes quick return");r.source(3);check(r.player.plays==1,"late acknowledgement resumes just once");
        r=new Run();r.player.async=true;r.leave();r.now+=2500;r.source(1);r.player.playing=false;r.source(3);check(r.player.plays==0,"ignored PAUSE expires instead of claiming a future user pause");
        r=new Run();r.player.async=true;r.leave();r.source(3);r.returning=NOTHING;r.player.playing=false;r.source(3);check(r.player.plays==0,"do nothing cancels a pending quick-return resume");
        r=new Run();r.leave();r.source(1);r.policy.reset();r.source(3);check(r.player.plays==0,"stopping/sleeping clears playback ownership");
        r=new Run();r.source(1);r.pause=false;r.returning=ALWAYS_PLAY;r.source(1);check(r.player.plays==0&&r.player.pauses==0,"settings change alone sends no commands");
        System.out.println("PASS: "+checks+" AUX playback policy checks");
    }
}
