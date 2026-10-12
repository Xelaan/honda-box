package dev.alex.fyt.probe;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.widget.*;
import java.util.concurrent.*;
import rikka.shizuku.Shizuku;

/** In-app equivalent of tools/stock_overlay_adb.py, using an authorized ADB UserService. */
public final class StockOverlaySettings {
    private final Activity activity;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final StockOverlayBackup backup;
    private final Shizuku.UserServiceArgs args;
    private IBinder service;
    private boolean registered,closed,binding,busy,rendering;
    private String mode,error="";
    private CheckBox toggle;private TextView status;private Button connect,restore;
    private final Shizuku.OnBinderReceivedListener received=()->ui.post(this::connectIfAllowed);
    private final Shizuku.OnBinderDeadListener died=()->ui.post(()->{service=null;binding=false;mode=null;render();});
    private final Shizuku.OnRequestPermissionResultListener permission=(code,grant)->{if(code==841)ui.post(()->{if(grant==PackageManager.PERMISSION_GRANTED)connectIfAllowed();else{error=I18n.t(R.string.stock_denied);render();}});};
    private final ServiceConnection connection=new ServiceConnection(){
        public void onServiceConnected(ComponentName name,IBinder binder){if(closed)return;binding=false;service=binder;refresh();}
        public void onServiceDisconnected(ComponentName name){service=null;binding=false;mode=null;render();}
    };
    public StockOverlaySettings(Activity activity){this.activity=activity;backup=new StockOverlayBackup(activity.getSharedPreferences("stock-overlay",0));args=new Shizuku.UserServiceArgs(new ComponentName(activity,StockOverlayShellService.class)).tag("stock-overlay-"+SystemClock.elapsedRealtimeNanos()).version(18).daemon(false).processNameSuffix("stock_overlay");}
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    private TextView label(int text,int size,int color){TextView v=new TextView(activity);v.setText(I18n.t(text));v.setTextColor(color);v.setTextSize(size);return v;}
    public android.view.View controls(){
        ThemeSettings.Palette p=ThemeSettings.resolve(activity).palette;
        LinearLayout box=new LinearLayout(activity);box.setOrientation(1);box.setPadding(dp(20),dp(16),dp(20),dp(16));GradientDrawable shape=new GradientDrawable();shape.setColor(p.card);shape.setCornerRadius(dp(18));box.setBackground(shape);
        box.addView(label(R.string.stock_title,14,p.accent));toggle=new CheckBox(activity);toggle.setText(I18n.t(R.string.stock_hide));toggle.setTextColor(p.text);box.addView(toggle);
        box.addView(label(R.string.stock_scope,12,p.muted));status=label(R.string.stock_unavailable,12,p.muted);status.setPadding(0,dp(8),0,dp(8));box.addView(status);
        LinearLayout buttons=new LinearLayout(activity);connect=ConsoleControls.button(activity,I18n.t(R.string.stock_connect),p,false);connect.setOnClickListener(v->authorize());restore=ConsoleControls.button(activity,I18n.t(R.string.stock_restore),p,false);restore.setOnClickListener(v->change(false));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,dp(48));lp.rightMargin=dp(8);buttons.addView(connect,lp);buttons.addView(restore,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,dp(48)));box.addView(buttons);
        Button help=ConsoleControls.button(activity,I18n.t(R.string.stock_setup),p,false);help.setOnClickListener(v->setup());box.addView(help,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,-2));
        toggle.setOnCheckedChangeListener((v,on)->{if(!rendering)change(on);});render();return box;
    }
    private boolean available(){try{return Shizuku.pingBinder()&&!Shizuku.isPreV11();}catch(RuntimeException e){return false;}}
    private boolean authorized(){try{return available()&&Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED;}catch(RuntimeException e){return false;}}
    private boolean installed(){try{activity.getPackageManager().getPackageInfo(StockOverlayShellService.PACKAGE,0);return true;}catch(PackageManager.NameNotFoundException e){return false;}}
    public void start(){if(closed)return;if(!registered){registered=true;Shizuku.addBinderReceivedListener(received);Shizuku.addBinderDeadListener(died);Shizuku.addRequestPermissionResultListener(permission);}connectIfAllowed();}
    private void connectIfAllowed(){if(closed)return;if(!authorized()){mode=null;render();return;}if(service!=null){refresh();return;}if(binding)return;
        try{binding=true;error="";Shizuku.bindUserService(args,connection);render();ui.postDelayed(()->{if(binding&&!closed){binding=false;error=I18n.t(R.string.stock_timeout);render();}},12000);}catch(RuntimeException e){binding=false;error=e.getMessage();render();}
    }
    private void authorize(){error="";if(!available()){setup();return;}try{if(authorized())connectIfAllowed();else if(Shizuku.shouldShowRequestPermissionRationale()){error=I18n.t(R.string.stock_denied);render();Intent i=activity.getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");if(i!=null)activity.startActivity(i);}else Shizuku.requestPermission(841);}catch(RuntimeException e){error=e.getMessage();render();}}
    private void refresh(){if(busy||closed||service==null)return;execute(null);}
    private void change(boolean hide){if(busy||service==null||!authorized()||!installed()){render();return;}if(!hide&&backup.original()==null){render();return;}execute(hide);}
    private void execute(Boolean hide){
        final IBinder remote=service;busy=true;error="";render();worker.execute(()->{
            String value=null,problem="";
            try{
                String uid=StockOverlayShellService.call(remote,StockOverlayShellService.UID,null);if(!"2000".equals(uid)&&!"0".equals(uid))throw new SecurityException("ADB shell rights required");
                value=StockOverlayShellService.call(remote,StockOverlayShellService.READ,null);
                if(hide!=null){String target;
                    if(hide){if(!"ignore".equals(value))backup.save(value);target="ignore";}
                    else {target=backup.original();if(target==null)throw new IllegalStateException("No original AppOp backup");}
                    value=StockOverlayShellService.call(remote,StockOverlayShellService.SET,target);
                    if(!target.equals(value))throw new IllegalStateException("AppOp readback mismatch");
                    if(!hide)backup.restored();
                }
            }catch(Exception e){problem=e.getMessage()==null?e.toString():e.getMessage();try{value=StockOverlayShellService.call(remote,StockOverlayShellService.READ,null);}catch(Exception ignored){value=null;}}
            final String actual=value,failed=problem;ui.post(()->{busy=false;if(closed)return;mode=actual;error=failed;render();});
        });
    }
    private void render(){if(closed||toggle==null)return;rendering=true;toggle.setChecked("ignore".equals(mode));
        boolean ready=service!=null&&authorized()&&installed()&&!binding&&!busy;
        toggle.setEnabled(ready&&mode!=null&&(!"ignore".equals(mode)||backup.original()!=null));restore.setEnabled(ready&&backup.original()!=null);connect.setEnabled(!busy&&!binding);
        int state=busy||binding?R.string.stock_working:!installed()?R.string.stock_missing:!available()?R.string.stock_unavailable:!authorized()?R.string.stock_permission:mode==null?R.string.stock_unknown:"ignore".equals(mode)?R.string.stock_blocked:R.string.stock_allowed;
        String text=I18n.t(state);if(backup.original()!=null)text+="\n"+I18n.t(R.string.stock_backup)+" "+backup.original();else if("ignore".equals(mode))text+="\n"+I18n.t(R.string.stock_external);if(!error.isEmpty())text+="\n"+I18n.t(R.string.stock_error)+" "+error;status.setText(text);rendering=false;
    }
    private void setup(){String command=I18n.t(R.string.stock_command_help);
        try{android.content.pm.PackageInfo info=activity.getPackageManager().getPackageInfo("moe.shizuku.privileged.api",0);String[] version=info.versionName.split("\\.");int major=Integer.parseInt(version[0]),minor=Integer.parseInt(version[1]);command=major>13||major==13&&minor>=6?"adb shell "+info.applicationInfo.nativeLibraryDir+"/libshizuku.so":"adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh";}catch(Exception ignored){}
        TextView help=new TextView(activity);help.setPadding(dp(24),dp(16),dp(24),dp(16));help.setText(I18n.t(R.string.stock_help)+"\n\n"+command);help.setTextIsSelectable(true);
        ScrollView scroll=new ScrollView(activity);scroll.addView(help);
        new AlertDialog.Builder(activity).setTitle(I18n.t(R.string.stock_setup)).setView(scroll).setPositiveButton(I18n.t(R.string.stock_open),(d,w)->{try{Intent launch=activity.getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");if(launch!=null)activity.startActivity(launch);else activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://shizuku.rikka.app/download/")));}catch(RuntimeException e){Toast.makeText(activity,I18n.t(R.string.settings_unavailable),Toast.LENGTH_LONG).show();}}).setNegativeButton(android.R.string.cancel,null).show();
    }
    public void close(){closed=true;ui.removeCallbacksAndMessages(null);if(registered){Shizuku.removeBinderReceivedListener(received);Shizuku.removeBinderDeadListener(died);Shizuku.removeRequestPermissionResultListener(permission);}if(service!=null||binding)try{Shizuku.unbindUserService(args,connection,!busy);}catch(RuntimeException ignored){}worker.shutdown();}
}
