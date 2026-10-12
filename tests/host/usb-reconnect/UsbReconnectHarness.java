package dev.alex.fyt.probe;
/** Exercises validated BSJ packets against the real state model, without USB hardware. */
public final class UsbReconnectHarness {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        for(int source:new int[]{2,4}){
            ProbeState state=IpodMarkerStateHarness.state(source,5);
            IpodMarkerStateHarness.frame(state,5,0,0,1,2,0,1,0,9);
            check(state.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.USB,"initial USB identified");
            IpodMarkerStateHarness.frame(state,5,0,9,0,0,0,0,0,0);
            check(state.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.UNKNOWN,"unplug clears device");
            // During reconnect a counter can be valid before the display family is known.
            for(int disc:new int[]{0,1})for(int family:new int[]{0,1,2,3,255}){
                IpodMarkerStateHarness.frame(state,5,disc,0,1,family,0,1,0,0);
                check(state.overlaySnapshot().displayNumber(101)==1,"transitional packet accepted");
            }
            IpodMarkerStateHarness.frame(state,5,0,0,1,2,0,1,0,1);
            check(state.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.USB,"USB resumes without source event");
            check(AudioMode.reported(state.overlaySnapshot())==AudioMode.USB,"shared player restored");
        }
        System.out.println("PASS: "+checks+" USB reconnect checks");
    }
}
