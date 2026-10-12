package dev.alex.fyt.probe;

import android.os.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.regex.*;

/** Shizuku UserService: only one package and one AppOp; no arbitrary shell interface. */
public final class StockOverlayShellService extends Binder {
    static final String DESCRIPTOR="dev.alex.fyt.probe.StockOverlayShell";
    static final String PACKAGE="com.syu.canbus", OP="SYSTEM_ALERT_WINDOW";
    static final int READ=1, SET=2, UID=3;
    public StockOverlayShellService(){attachInterface(null,DESCRIPTOR);}
    static boolean validMode(String mode){return "allow".equals(mode)||"ignore".equals(mode)||"deny".equals(mode)||"default".equals(mode)||"foreground".equals(mode);}
    static String parseMode(String output) throws IOException {
        Matcher m=Pattern.compile("(?m)^\\s*(?:Uid mode:\\s*)?(?:SYSTEM_ALERT_WINDOW|android:system_alert_window):\\s*(allow|ignore|deny|default|foreground)\\b",Pattern.CASE_INSENSITIVE).matcher(output);
        if(m.find())return m.group(1);
        if(output.trim().matches("No operations\\.(?:\\s+Default mode: \\w+)?"))return "default";
        throw new IOException("Cannot read package AppOp: "+output);
    }
    private String command(String... args) throws Exception {
        if(android.os.Process.myUid()!=2000&&android.os.Process.myUid()!=0)throw new SecurityException("ADB shell rights required");
        ProcessBuilder builder=new ProcessBuilder(args);builder.redirectErrorStream(true);java.lang.Process process=builder.start();
        ExecutorService reader=Executors.newSingleThreadExecutor();
        Future<String> result=reader.submit(()->{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] chunk=new byte[1024];int n;try(InputStream input=process.getInputStream()){while((n=input.read(chunk))!=-1){if(out.size()+n>16384)throw new IOException("AppOp output too large");out.write(chunk,0,n);}}int exit=process.waitFor();String text=new String(out.toByteArray(),StandardCharsets.UTF_8);if(exit!=0)throw new IOException(text);return text;});
        try{return result.get(8,TimeUnit.SECONDS);}finally{process.destroy();result.cancel(true);reader.shutdownNow();}
    }
    private String read() throws Exception {
        // Package mode alone is not enough if an external tool has set a UID override.
        String user=String.valueOf(Binder.getCallingUid()/100000);
        String packages=command("/system/bin/cmd","package","list","packages","-U","--user",user,PACKAGE);
        Matcher uid=Pattern.compile("(?m)^package:"+Pattern.quote(PACKAGE)+" uid:(\\d+)\\s*$").matcher(packages);
        if(!uid.find())throw new IOException("Stock CANBUS package is not installed");
        // `get --uid PACKAGE` also prints package operations on some Android releases.
        String uidMode=command("/system/bin/appops","get",uid.group(1),OP);
        if(!"default".equals(parseMode(uidMode)))throw new IOException("UID-level AppOp override: "+uidMode.trim());
        return parseMode(command("/system/bin/appops","get","--user",user,PACKAGE,OP));
    }
    @Override protected synchronized boolean onTransact(int code,Parcel data,Parcel reply,int flags) throws RemoteException {
        if(code==16777115){android.os.Process.killProcess(android.os.Process.myPid());return true;}
        if(code==INTERFACE_TRANSACTION){reply.writeString(DESCRIPTOR);return true;}
        if(code!=READ&&code!=SET&&code!=UID)return super.onTransact(code,data,reply,flags);
        data.enforceInterface(DESCRIPTOR);
        try {
            String value;
            if(code==UID)value=String.valueOf(android.os.Process.myUid());
            else if(code==READ)value=read();
            else {String mode=data.readString();if(!validMode(mode))throw new IllegalArgumentException("Invalid AppOp mode");read();command("/system/bin/appops","set","--user",String.valueOf(Binder.getCallingUid()/100000),PACKAGE,OP,mode);value=read();if(!mode.equals(value))throw new IOException("AppOp readback mismatch: "+value);}
            reply.writeNoException();reply.writeString(value);
        }catch(Exception error){reply.writeException(new IllegalStateException(error.getMessage(),error));}
        return true;
    }
    static String call(IBinder binder,int code,String mode) throws RemoteException {
        Parcel data=Parcel.obtain(),reply=Parcel.obtain();try{data.writeInterfaceToken(DESCRIPTOR);if(code==SET)data.writeString(mode);if(!binder.transact(code,data,reply,0))throw new RemoteException("Unsupported AppOp service");reply.readException();return reply.readString();}finally{data.recycle();reply.recycle();}
    }
}
