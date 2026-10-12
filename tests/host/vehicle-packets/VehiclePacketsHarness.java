package dev.alex.fyt.probe;
public final class VehiclePacketsHarness {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        ProbeState s=IpodMarkerStateHarness.state(2,1);s.recording(false);
        check(s.snapshot().vehiclePacket0E==null&&s.snapshot().vehiclePacket10==null,"not received baseline");
        int[] car={0xFF,0x34,0x12,0x78,0x56,3,4,5,0xBC,0x9A,0xEF,0xCD,0xAB};
        IpodMarkerStateHarness.frame(s,14,car);BsjVehiclePackets.Sample a=s.snapshot().vehiclePacket0E;
        check(a!=null&&a.count==1&&a.mappedCount==1&&a.length==13,"recording off captures type0E");
        check(a.fields.get(140)==255&&a.fields.get(141)==0x1234&&a.fields.get(142)==0x5678&&a.fields.get(143)==3&&a.fields.get(144)==4&&a.fields.get(145)==5&&a.fields.get(146)==0x9ABC&&a.fields.get(147)==0xABCDEF,"all stock little-endian mappings");
        check(s.snapshot().bsjValues.get(147).ints[0]==0xABCDEF,"existing decoder retained");
        IpodMarkerStateHarness.frame(s,14,car);check(s.snapshot().vehiclePacket0E.count==2,"repeat counted");
        check(a.count==1&&a.fields.get(141)==0x1234,"previous snapshots immutable");
        IpodMarkerStateHarness.frame(s,14,new int[14]);a=s.snapshot().vehiclePacket0E;
        check(a.count==3&&a.mappedCount==2&&a.length==14&&a.fields.isEmpty(),"longer payload counted raw; not guessed");
        try{a.fields.put(140,7);throw new AssertionError("mutable fields");}catch(UnsupportedOperationException expected){checks++;}
        IpodMarkerStateHarness.frame(s,16,0xEA);BsjVehiclePackets.Sample b=s.snapshot().vehiclePacket10;
        check(b.count==1&&b.fields.get(133)==7&&b.fields.get(134)==10,"type10 first-byte bit split");
        IpodMarkerStateHarness.frame(s,16,0x23,0xFF);b=s.snapshot().vehiclePacket10;
        check(b.count==2&&b.mappedCount==2&&b.fields.get(133)==1&&b.fields.get(134)==3&&b.fields.size()==2&&b.payloadHex.equals("23 FF"),"type10 second byte preserved without invented field");
        IpodMarkerStateHarness.frame(s,16,new int[0]);b=s.snapshot().vehiclePacket10;
        check(b.count==3&&b.mappedCount==2&&b.fields.isEmpty(),"empty type10 safe");
        IpodMarkerStateHarness.frame(s,16,1,2,3);check(s.snapshot().vehiclePacket10.count==4&&s.snapshot().vehiclePacket10.fields.isEmpty(),"unexpected type10 length retained");
        s.event(1019,new int[]{46,16,1,0xEA,0},null,null);
        check(s.snapshot().vehiclePacket10.count==4,"bad checksum cannot increment count");
        check(s.snapshot().audioSource()==2&&s.snapshot().bsjAudioEnabled==null,"watch cannot change audio source/power");
        s.clearLog();check(s.snapshot().vehiclePacket0E==null&&s.snapshot().vehiclePacket10==null,"log reset clears watch counters");
        check(s.snapshot().bsjValues.get(147).ints[0]==0xABCDEF,"log reset retains existing decoded state");
        IpodMarkerStateHarness.frame(s,14,car);s.event(1000,new int[]{1},null,null);IpodMarkerStateHarness.frame(s,14,car);
        check(s.snapshot().vehiclePacket0E==null,"profile change clears/gates watch");
        s.event(1000,new int[]{6291908},null,null);IpodMarkerStateHarness.frame(s,16,0xEA);s.newSession();
        check(s.snapshot().vehiclePacket10==null,"connection session resets watch");
        System.out.println("PASS "+checks+" vehicle packet watch checks");
    }
}
