package app;
import android.app.Service;
import android.content.Intent;
import android.os.*;
import java.util.*;
import org.json.*;

/** Emulator fixture only, independently signed. This is not an FYT service implementation. */
public class ToolkitService extends Service {
    static final Map<Integer,Integer> VALUES = Collections.synchronizedMap(new LinkedHashMap<>());
    static final Map<Integer,String> STRINGS=Collections.synchronizedMap(new LinkedHashMap<>());
    static final Map<Integer,IBinder> CALLBACKS = Collections.synchronizedMap(new LinkedHashMap<>());
    static final Map<String,IBinder> ROUTE_CALLBACKS=Collections.synchronizedMap(new LinkedHashMap<>());
    static final Map<String,int[]> ROUTE_VALUES=Collections.synchronizedMap(new LinkedHashMap<>());
    static final Map<String,String[]> ROUTE_STRINGS=Collections.synchronizedMap(new LinkedHashMap<>());
    static volatile boolean noRouteModules;
    static volatile int routeDelayMs;
    static IBinder lateMain;
    static { ROUTE_VALUES.put("0:0",new int[]{13}); }
    static String routeReport() { return new JSONArray(ROUTE_CALLBACKS.keySet()).toString(); }
    static void emitRoute(String key,IBinder callback) {
        int[] ints=ROUTE_VALUES.get(key); String[] strings=ROUTE_STRINGS.get(key);
        if(ints==null&&strings==null) return;
        Parcel p=Parcel.obtain();
        try { p.writeInterfaceToken("com.syu.ipc.IModuleCallback");p.writeInt(Integer.parseInt(key.split(":")[1]));
            p.writeIntArray(ints);p.writeFloatArray(null);p.writeStringArray(strings);callback.transact(1,p,null,IBinder.FLAG_ONEWAY);
        }catch(Exception ignored){}finally{p.recycle();}
    }
    static void emitRoutes(){synchronized(ROUTE_CALLBACKS){for(Map.Entry<String,IBinder> e:ROUTE_CALLBACKS.entrySet())emitRoute(e.getKey(),e.getValue());}}
    static volatile boolean silent, nullTemp,simulateSourceCommands;
    static int[] frame(int type,int... payload){int[] a=new int[payload.length+4];a[0]=46;a[1]=type;a[2]=payload.length;int sum=type+payload.length;for(int i=0;i<payload.length;i++){a[i+3]=payload[i];sum+=payload[i];}a[a.length-1]=(~sum)&255;return a;}
    private static void previewSource(int source){VALUES.put(98,source);VALUES.put(114,0);VALUES.put(101,source==2?5:2);VALUES.put(99,2);VALUES.put(100,0x16);VALUES.put(113,source==2?1:0);STRINGS.put(103,"TEST · "+(source==2?"CD":source==4?"USB/iPod":"Radio")+" playback");STRINGS.put(104,"");STRINGS.put(105,"");raw=frame(3,0x80|((source-1)&15),18);emitAll();}
    static int[] raw;
    static final List<JSONObject> COMMANDS = Collections.synchronizedList(new ArrayList<>());
    static String commandReport() { synchronized(COMMANDS) { return new JSONArray(COMMANDS).toString(); } }
    static void recordCommand(int module,Parcel d,int flags) throws RemoteException {
        int cmd=d.readInt(); int[] args=d.createIntArray(); float[] floats=d.createFloatArray(); String[] strings=d.createStringArray();
        // Binder adds FLAG_ACCEPT_FDS (16) during cross-process delivery.
        boolean valid = args != null && floats == null && strings == null && d.dataAvail() == 0 && (flags & IBinder.FLAG_ONEWAY) != 0;
        if(module==0) valid &= cmd==0 && args!=null && args.length==1 && args[0]==13;
        else valid &= args!=null && ((cmd==1 && args.length==1 && (args[0]==0||args[0]==1||args[0]==3))
            ||(cmd==0 && args.length==2 && (args[0]==3||args[0]==4) && (args[1]==0||args[1]==1)));
        try { COMMANDS.add(new JSONObject().put("module",module).put("cmd",cmd).put("args",args==null?JSONObject.NULL:new JSONArray(args))
            .put("flags",flags).put("valid",valid).put("atMs",SystemClock.uptimeMillis())); }
        catch(JSONException e){throw new RemoteException(e.toString());}
        if(module==0&&cmd==0&&args!=null&&args.length==1&&args[0]==13&&routeDelayMs>=0)new Handler(Looper.getMainLooper()).postDelayed(()->{ROUTE_VALUES.put("0:0",new int[]{13});emitRoutes();},routeDelayMs);
        if(simulateSourceCommands&&module==7&&cmd==1&&args!=null&&args.length==1)previewSource(args[0]+1);
        if(module==7 && cmd==0 && args!=null && args.length==2){ VALUES.put(146,args[1]==1?args[0]:0); emitAll(); }
    }
    static { VALUES.put(1000,6291908); VALUES.put(98,1); VALUES.put(115,2); VALUES.put(132,18); VALUES.put(27,44); VALUES.put(28,46); VALUES.put(21,4);
        VALUES.put(10,1); VALUES.put(11,1); VALUES.put(13,1); VALUES.put(18,0); VALUES.put(19,1); VALUES.put(20,1); VALUES.put(96,321);
        for(int field:new int[]{12,14,16,17,33,36,49})VALUES.put(field,0);
        VALUES.put(99,2); VALUES.put(100,21); VALUES.put(101,9); VALUES.put(113,1); VALUES.put(114,9);VALUES.put(138,32);VALUES.put(139,0);STRINGS.put(136,"");STRINGS.put(137,""); }
    static void emit(int code, IBinder callback) {
        if (silent) return;
        Integer value=VALUES.get(code); if(value==null && code!=135 && code!=1019 && !STRINGS.containsKey(code)) return;
        Parcel p=Parcel.obtain();
        try {
            p.writeInterfaceToken("com.syu.ipc.IModuleCallback"); p.writeInt(code);
            p.writeIntArray(code==1019?(raw==null?new int[]{0}:raw):(code==135||STRINGS.containsKey(code)) || (code==27 && nullTemp) ? null:new int[]{value}); p.writeFloatArray(null);
            p.writeStringArray(STRINGS.containsKey(code)?new String[]{STRINGS.get(code)}:code==135?new String[]{"TEST MENU"}:null);
            callback.transact(1,p,null,IBinder.FLAG_ONEWAY);
        } catch(Exception ignored) {} finally {p.recycle();}
    }
    static void emitAll() { synchronized(CALLBACKS) { for(Map.Entry<Integer,IBinder> e:CALLBACKS.entrySet()) emit(e.getKey(),e.getValue()); } }
    private final Binder module=new Binder() {
        { attachInterface(null,"com.syu.ipc.IRemoteModule"); }
        @Override protected boolean onTransact(int code,Parcel d,Parcel r,int flags) throws RemoteException {
            if(code==INTERFACE_TRANSACTION){r.writeString("com.syu.ipc.IRemoteModule");return true;}
            d.enforceInterface("com.syu.ipc.IRemoteModule");
            if(code==1){recordCommand(7,d,flags);return true;}
            if(code==3){IBinder cb=d.readStrongBinder();int event=d.readInt();int initial=d.readInt();CALLBACKS.put(event,cb);if(initial==1)emit(event,cb);return true;}
            if(code==4){d.readStrongBinder();CALLBACKS.remove(d.readInt());return true;}
            return false;
        }
    };
    private Binder routeModule(final int moduleCode) { return new Binder() {
        { attachInterface(null,"com.syu.ipc.IRemoteModule"); }
        @Override protected boolean onTransact(int code,Parcel d,Parcel r,int flags) throws RemoteException {
            if(code==INTERFACE_TRANSACTION){r.writeString("com.syu.ipc.IRemoteModule");return true;}
            d.enforceInterface("com.syu.ipc.IRemoteModule");
            if(code==1){recordCommand(moduleCode,d,flags);return true;}
            if(code==3){IBinder callback=d.readStrongBinder();int event=d.readInt();int initial=d.readInt();String key=moduleCode+":"+event;
                ROUTE_CALLBACKS.put(key,callback);if(initial==1)emitRoute(key,callback);return true;}
            if(code==4){d.readStrongBinder();String key=moduleCode+":"+d.readInt();if(key.equals("0:0"))lateMain=ROUTE_CALLBACKS.get(key);ROUTE_CALLBACKS.remove(key);return true;}
            return false;
        }
    }; }
    private final Binder main=routeModule(0),ipod=routeModule(5);
    private final Binder toolkit=new Binder() {
        {attachInterface(null,"com.syu.ipc.IRemoteToolkit");}
        @Override protected boolean onTransact(int code,Parcel d,Parcel r,int flags) throws RemoteException {
            if(code==INTERFACE_TRANSACTION){r.writeString("com.syu.ipc.IRemoteToolkit");return true;}
            if(code!=1)return false;d.enforceInterface("com.syu.ipc.IRemoteToolkit");int moduleCode=d.readInt();r.writeNoException();r.writeStrongBinder(moduleCode==7?module:noRouteModules?null:moduleCode==0?main:moduleCode==5?ipod:null);return true;
        }
    };
    @Override public IBinder onBind(Intent intent){return toolkit;}
    @Override public void onDestroy(){CALLBACKS.clear();ROUTE_CALLBACKS.clear();super.onDestroy();}
}
