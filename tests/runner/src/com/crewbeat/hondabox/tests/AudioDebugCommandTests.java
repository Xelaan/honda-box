package com.crewbeat.hondabox.tests;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.crewbeat.hondabox.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
/** Tests the actual service and Binder serialization against local mocks, never vehicle hardware. */
public final class AudioDebugCommandTests extends Instrumentation {
    private final ProbeState state=ProbeState.INSTANCE;
    private HandlerThread thread;private Handler worker;private ProbeService service;
    private final List<Step> steps=Collections.synchronizedList(new ArrayList<>());
    private AudioSourceController screens;
    private volatile Throwable failure;private int checks,routeDelay;private Activity activity;
    private static final class Step {final int module,cmd;final int[] args;final long at;Step(int m,int c,int[] a){module=m;cmd=c;args=a;at=SystemClock.uptimeMillis();}}
    private void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    private void field(String name,Object value)throws Exception{java.lang.reflect.Field f=ProbeService.class.getDeclaredField(name);f.setAccessible(true);f.set(service,value);}
    private void await(String text,Callable<Boolean> condition,long timeout)throws Exception{long end=SystemClock.uptimeMillis()+timeout;while(SystemClock.uptimeMillis()<end){if(failure!=null)throw new AssertionError("Worker failure",failure);if(condition.call())return;SystemClock.sleep(10);}throw new AssertionError(text);}
    private Binder module(final int id){return new Binder(){
        {attachInterface(null,FytProtocol.MODULE);}
        @Override protected boolean onTransact(int code,Parcel in,Parcel reply,int flags)throws RemoteException{
            if(code!=1)return false;in.enforceInterface(FytProtocol.MODULE);
            int cmd=in.readInt();int[] args=in.createIntArray();float[] floats=in.createFloatArray();String[] strings=in.createStringArray();
            if(args==null||floats!=null||strings!=null||in.dataAvail()!=0||(flags&1)==0)failure=new AssertionError("Invalid Binder payload");
            steps.add(new Step(id,cmd,args));
            if(id==0&&routeDelay>=0)worker.postDelayed(()->state.audioRoutes.event(0,0,new int[]{13},null,null),routeDelay);
            if(id==7&&(cmd==1||(cmd==1008&&args[0]==0x83))){int mode=cmd==1?args[0]:args[2];worker.postDelayed(()->state.event(98,new int[]{mode+1},null,null),20);}
            return true;
        }
    };}
    private void setup()throws Exception{
        steps.clear();state.newSession();state.running(true);state.status("TEST","local mocks only",true);
        state.event(1000,new int[]{6291908},null,null);state.event(98,new int[]{2},null,null);
        state.audioRoutes.start();state.audioRoutes.event(0,0,new int[]{0},null,null);field("active",true);field("generation",17);
    }
    private void send(AudioCommand action,AudioCommandOptions options)throws Exception{
        CountDownLatch started=new CountDownLatch(1);worker.post(()->{try{
            java.lang.reflect.Method m=ProbeService.class.getDeclaredMethod("sendCommand",AudioCommand.class,AudioCommandOptions.class);m.setAccessible(true);m.invoke(service,action,options);
        }catch(Throwable e){failure=e;}finally{started.countDown();}});check(started.await(2,TimeUnit.SECONDS),"manual action starts");
    }
    private List<Step> copy(){synchronized(steps){return new ArrayList<>(steps);}}
    private void assertSteps(AudioCommand action,AudioCommandOptions options)throws Exception{
        List<Step> all=copy();boolean main=options.setsMain(action);int start=main?1:0;
        check(all.size()==start+(action.selectsSource()?1:2),"one manual action; no retries");
        if(main){Step r=all.get(0);check(r.module==0&&r.cmd==0&&Arrays.equals(r.args,new int[]{13}),"MAIN13 request");}
        Step first=all.get(start);check(first.module==7&&first.cmd==options.command(action)&&Arrays.equals(first.args,options.args(action,true)),"known CAN bytes");
        if(main)check(first.at-all.get(0).at>=options.delayMs(action)-20,"configured route delay");
        if(!action.selectsSource()){Step release=all.get(start+1);check(release.cmd==first.cmd&&Arrays.equals(release.args,options.args(action,false)),"matched release transport");check(release.at-first.at>=options.holdMs-10,"configured hold duration");}
        JSONArray commands=new JSONObject(state.exportJson()).getJSONArray("commands");JSONObject command=commands.getJSONObject(commands.length()-1);JSONArray recorded=command.getJSONArray("steps");check(recorded.length()==all.size(),"every step exported");for(int i=0;i<recorded.length();i++)check(recorded.getJSONObject(i).has("mainChannelAtStep"),"MAIN state recorded per step");JSONObject record=command.getJSONObject("audioOptions");
        check(record.getString("transport").equals(options.transport.name())&&record.getString("route").equals(options.route.name())&&record.getInt("holdMs")==options.holdMs,"export records exact options");
    }
    private void finished()throws Exception{await("command completes",()->!state.snapshot().commandBusy,5000);}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();int outcome=Activity.RESULT_OK;Context c=getTargetContext();String language=I18n.language(c);android.content.SharedPreferences prefs=c.getSharedPreferences("audioDebug",0);Map<String,?> saved=prefs.getAll();
        try{
            c.stopService(new Intent(c,ProbeService.class));SystemClock.sleep(300);I18n.language(c,"en");
            thread=new HandlerThread("TEST-local-audio-commands");thread.start();worker=new Handler(thread.getLooper());service=new ProbeService();field("worker",worker);
            final Binder can=module(7),main=module(0);field("module",can);
            Binder toolkit=new Binder(){ {attachInterface(null,FytProtocol.TOOLKIT);}
                @Override protected boolean onTransact(int code,Parcel in,Parcel reply,int flags){in.enforceInterface(FytProtocol.TOOLKIT);int id=in.readInt();reply.writeNoException();reply.writeStrongBinder(id==0?main:can);return true;}};field("toolkitBinder",toolkit);
            for(AudioCommandOptions.Transport transport:AudioCommandOptions.Transport.values())for(AudioCommandOptions.Route route:AudioCommandOptions.Route.values()){
                AudioCommandOptions options=new AudioCommandOptions(transport,route,60,true);routeDelay=50;setup();send(AudioCommand.USB,options);finished();assertSteps(AudioCommand.USB,options);
                setup();send(AudioCommand.PREVIOUS,options);finished();assertSteps(AudioCommand.PREVIOUS,options);
            }
            for(AudioCommand action:new AudioCommand[]{AudioCommand.RADIO,AudioCommand.CD,AudioCommand.NEXT}){
                setup();AudioCommandOptions options=new AudioCommandOptions(AudioCommandOptions.Transport.RAW_1008,AudioCommandOptions.Route.CAN_ONLY,180,true);send(action,options);finished();assertSteps(action,options);
            }
            for(int hold:new int[]{400,800}){setup();AudioCommandOptions options=new AudioCommandOptions(AudioCommandOptions.Transport.RAW_1008,AudioCommandOptions.Route.CAN_ONLY,hold,true);send(AudioCommand.NEXT,options);finished();assertSteps(AudioCommand.NEXT,options);}
            for(AudioCommand action:new AudioCommand[]{AudioCommand.USB,AudioCommand.NEXT}){setup();send(action,AudioCommandOptions.STANDARD);finished();assertSteps(action,AudioCommandOptions.STANDARD);}
            routeDelay=-1;setup();AudioCommandOptions confirmed=new AudioCommandOptions(AudioCommandOptions.Transport.RAW_1008,AudioCommandOptions.Route.CONFIRMED_13,60,true);send(AudioCommand.USB,confirmed);finished();check(copy().size()==1,"missing route confirmation sends no CAN");
            setup();AudioCommandOptions oem=new AudioCommandOptions(AudioCommandOptions.Transport.RAW_1008,AudioCommandOptions.Route.OEM,60,true);send(AudioCommand.USB,oem);finished();assertSteps(AudioCommand.USB,oem);check(state.audioRoutes.mainChannel()==0,"OEM option works without MAIN callback");
            setup();AudioCommandOptions delayed=new AudioCommandOptions(AudioCommandOptions.Transport.RAW_1008,AudioCommandOptions.Route.DELAY_700,60,true);send(AudioCommand.USB,delayed);worker.postDelayed(()->state.event(1000,new int[]{1},null,null),100);finished();check(copy().size()==1,"changed profile cancels delayed CAN command");
            routeDelay=50;setup();send(AudioCommand.NEXT,new AudioCommandOptions(AudioCommandOptions.Transport.RAW_1008,AudioCommandOptions.Route.CAN_ONLY,800,true));send(AudioCommand.CD,oem);finished();check(copy().size()==2&&copy().get(1).args[3]==0,"concurrent action rejected; original release preserved");
            setup();state.status("TEST","disconnected",false);send(AudioCommand.CD,oem);check(copy().isEmpty(),"disconnected commands rejected");
            try{AudioCommandOptions.debug("RAW_1008","CAN_ONLY",123);throw new AssertionError("invalid hold accepted");}catch(IllegalArgumentException expected){checks++;}
            setup();activity=startActivitySync(new Intent(c,DebugActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));View root=activity.findViewById(android.R.id.content);runOnMainSync(()->{screens=new AudioSourceController(c);screens.start();});state.event(98,new int[]{4},null,null);SystemClock.sleep(350);check(DebugActivity.isVisible()&&!AudioPlayerActivity.isVisible(),"source tests keep Debug visible");
            Spinner transport=(Spinner)root.findViewWithTag("debug-audio-transport"),route=(Spinner)root.findViewWithTag("debug-audio-route"),hold=(Spinner)root.findViewWithTag("debug-audio-hold");
            check(transport.getCount()==2&&route.getCount()==5&&hold.getCount()==4,"all Debug combinations present");
            runOnMainSync(()->{transport.setSelection(1);route.setSelection(4);hold.setSelection(3);});waitForIdleSync();check(prefs.getInt("transport",-1)==1&&prefs.getInt("route",-1)==4&&prefs.getInt("hold",-1)==3,"Debug choices persist");
            java.lang.reflect.Field scrollField=SettingsScreenActivity.class.getDeclaredField("scroll");scrollField.setAccessible(true);ScrollView scroll=(ScrollView)scrollField.get(activity);
            runOnMainSync(()->scroll.scrollTo(0,root.findViewWithTag("debug-audio-commands").getTop()));SystemClock.sleep(300);
            android.graphics.Bitmap image=getUiAutomation().takeScreenshot();try(java.io.OutputStream out=new java.io.FileOutputStream(new java.io.File(c.getExternalFilesDir(null),"audio-debug-options-TEST.png"))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}image.recycle();
            result.putString("result","PASS: "+checks+" actual service/Binder/UI checks; local mocks only");
        }catch(Throwable e){result.putString("failure",android.util.Log.getStackTraceString(e));outcome=1;}
        finally{
            if(screens!=null)runOnMainSync(()->screens.close());if(activity!=null)runOnMainSync(()->activity.finish());waitForIdleSync();if(worker!=null)worker.removeCallbacksAndMessages(null);if(thread!=null)thread.quitSafely();state.newSession();state.running(false);state.clearLog();
            android.content.SharedPreferences.Editor edit=prefs.edit().clear();for(Map.Entry<String,?> e:saved.entrySet())if(e.getValue() instanceof Integer)edit.putInt(e.getKey(),(Integer)e.getValue());edit.commit();I18n.language(c,language);
        }
        finish(outcome,result);
    }
}
