package com.crewbeat.hondabox;

/** Bounded BSJ 2E/command/length/payload/checksum stream reader. No serial access or writes. */
public final class BsjFrames {
    public interface Listener { void frame(int command,int[] payload); }
    public interface TimedListener { void frame(int command,int[] payload,long completedAtMs); }
    private final TimedListener listener;
    private final int[] buffer=new int[260];private int size;private long lastAt;
    public long accepted,rejected;
    public BsjFrames(Listener target){listener=(command,payload,at)->target.frame(command,payload);}
    public BsjFrames(TimedListener target){listener=target;}
    public void reset(){size=0;lastAt=0;accepted=0;rejected=0;}
    private void discard(int n){System.arraycopy(buffer,n,buffer,0,size-n);size-=n;}
    public void accept(int[] chunk,long at){
        if(chunk==null||(chunk.length==1&&chunk[0]==0))return;
        lastAt=at;
        for(int k=0;k<Math.min(chunk.length,8192);k++){
            if(size==buffer.length)discard(1);buffer[size++]=chunk[k]&255;
            while(size>0){
                if(buffer[0]!=0x2e){discard(1);continue;}
                if(size<3)break;
                int total=buffer[2]+4;if(size<total)break;
                int sum=0;for(int i=1;i<total;i++)sum+=buffer[i];
                if((sum&255)!=255){rejected++;discard(1);continue;}
                int command=buffer[1],length=buffer[2];int[] payload=new int[length];System.arraycopy(buffer,3,payload,0,length);
                discard(total);accepted++;listener.frame(command,payload,at);
            }
        }
    }
}
