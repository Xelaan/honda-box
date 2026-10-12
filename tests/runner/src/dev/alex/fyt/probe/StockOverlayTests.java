package dev.alex.fyt.probe;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import rikka.shizuku.Shizuku;

/** Real shell-UID integration test against com.syu.canbus TEST ONLY fixture. */
public final class StockOverlayTests extends Instrumentation {
    private int checks;private Activity activity;
    private void check(String label,boolean ok){if(!ok)throw new AssertionError(label);checks++;}
    private void await(String label,Callable<Boolean> condition) throws Exception {long end=SystemClock.uptimeMillis()+18000;while(SystemClock.uptimeMillis()<end){if(condition.call()){checks++;return;}SystemClock.sleep(100);}throw new AssertionError(label);}
    private <T extends TextView>T find(View root,String text,Class<T> kind){if(kind.isInstance(root)&&((TextView)root).getText().toString().equals(text))return kind.cast(root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){T found=find(((ViewGroup)root).getChildAt(i),text,kind);if(found!=null)return found;}return null;}
    private CheckBox toggle(){return find(activity.findViewById(android.R.id.content),I18n.t(R.string.stock_hide),CheckBox.class);}
    private Button button(int text){return find(activity.findViewById(android.R.id.content),I18n.t(text),Button.class);}
    private boolean enabled(){AtomicReference<Boolean> value=new AtomicReference<>(false);runOnMainSync(()->value.set(toggle()!=null&&toggle().isEnabled()));return value.get();}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();Shizuku.UserServiceArgs args=null;ServiceConnection connection=null;try{
        Context c=getTargetContext();c.getPackageManager().getPackageInfo("com.syu.canbus",0);
        check("test fixture only",c.getPackageManager().getApplicationLabel(c.getPackageManager().getApplicationInfo("com.syu.canbus",0)).toString().startsWith("TEST ONLY"));
        activity=startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        await("Shizuku binder received",()->Shizuku.pingBinder());
        if(Shizuku.checkSelfPermission()!=android.content.pm.PackageManager.PERMISSION_GRANTED){runOnMainSync(()->button(R.string.stock_connect).performClick());
            await("permission prompt",()->{android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();return root!=null&&root.getPackageName()!=null&&root.getPackageName().toString().equals("moe.shizuku.privileged.api");});
            android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();boolean clicked=false;
            for(String label:new String[]{"Allow all the time","Allow", "Разрешать всегда", "Разрешить"})for(android.view.accessibility.AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText(label)){if(n.isClickable()&&n.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)){clicked=true;break;}}
            check("grant emulator test permission",clicked);
        }
        await("Shizuku permission",()->Shizuku.checkSelfPermission()==android.content.pm.PackageManager.PERMISSION_GRANTED);
        final CountDownLatch latch=new CountDownLatch(1);final AtomicReference<IBinder> remote=new AtomicReference<>();
        args=new Shizuku.UserServiceArgs(new ComponentName(c,StockOverlayShellService.class)).tag("stock-overlay-tests").version(18).daemon(false).processNameSuffix("stock_overlay_test");
        connection=new ServiceConnection(){public void onServiceConnected(ComponentName n,IBinder b){remote.set(b);latch.countDown();}public void onServiceDisconnected(ComponentName n){}};
        final Shizuku.UserServiceArgs bindArgs=args;final ServiceConnection bindConnection=connection;runOnMainSync(()->Shizuku.bindUserService(bindArgs,bindConnection));check("shell service connects",latch.await(15,TimeUnit.SECONDS));
        IBinder binder=remote.get();check("actual ADB UID 2000",StockOverlayShellService.call(binder,StockOverlayShellService.UID,null).equals("2000"));
        StockOverlayBackup backup=new StockOverlayBackup(c.getSharedPreferences("stock-overlay",0));
        if(backup.original()!=null){StockOverlayShellService.call(binder,StockOverlayShellService.SET,backup.original());backup.restored();}
        StockOverlayShellService.call(binder,StockOverlayShellService.SET,"allow");
        // No writes may run locally in the normal application UID.
        boolean rejected=false;try{StockOverlayShellService.call(new StockOverlayShellService(),StockOverlayShellService.READ,null);}catch(Exception e){rejected=true;}check("normal app UID cannot execute AppOps",rejected);
        rejected=false;try{StockOverlayShellService.call(binder,StockOverlayShellService.SET,"allow; anything");}catch(Exception e){rejected=true;}check("arbitrary shell rejected",rejected);check("invalid mode leaves permission untouched",StockOverlayShellService.call(binder,StockOverlayShellService.READ,null).equals("allow"));
        check("clean initial backup",backup.original()==null);
        runOnMainSync(()->button(R.string.stock_connect).performClick());await("toggle ready",this::enabled);
        runOnMainSync(()->toggle().performClick());await("real AppOp changed to ignore",()->StockOverlayShellService.call(binder,StockOverlayShellService.READ,null).equals("ignore"));await("hide UI settles",this::enabled);
        check("backup captured allow",backup.original().equals("allow"));check("new backup instance recovers prior mode",new StockOverlayBackup(c.getSharedPreferences("stock-overlay",0)).original().equals("allow"));
        runOnMainSync(()->activity.finish());activity=startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        await("after recreate permission reconnect",this::enabled);check("backup survives screen restart",backup.original().equals("allow"));runOnMainSync(()->check("reopened toggle reflects ignore",toggle().isChecked()));
        runOnMainSync(()->toggle().performClick());await("toggle off restores allow",()->StockOverlayShellService.call(binder,StockOverlayShellService.READ,null).equals("allow"));await("backup cleared only after restore",()->backup.original()==null);
        StockOverlayShellService.call(binder,StockOverlayShellService.SET,"deny");runOnMainSync(()->button(R.string.stock_connect).performClick());await("refresh ready",this::enabled);
        runOnMainSync(()->toggle().performClick());await("hide deny state",()->StockOverlayShellService.call(binder,StockOverlayShellService.READ,null).equals("ignore"));await("deny backup recorded",()->"deny".equals(backup.original()));await("UI settles before restore",this::enabled);
        runOnMainSync(()->button(R.string.stock_restore).performClick());await("restore preserves deny instead of forcing allow",()->StockOverlayShellService.call(binder,StockOverlayShellService.READ,null).equals("deny"));await("restore clears backup",()->backup.original()==null);
        StockOverlayShellService.call(binder,StockOverlayShellService.SET,"ignore");runOnMainSync(()->button(R.string.stock_connect).performClick());SystemClock.sleep(700);check("externally hidden cannot restore guessed mode",!enabled());check("external change creates no fake backup",backup.original()==null);
        StockOverlayShellService.call(binder,StockOverlayShellService.SET,"default");runOnMainSync(()->button(R.string.stock_connect).performClick());await("default mode refresh",this::enabled);
        runOnMainSync(()->toggle().performClick());await("default hide",()->StockOverlayShellService.call(binder,StockOverlayShellService.READ,null).equals("ignore"));await("default saved",()->"default".equals(backup.original()));await("default UI settles",this::enabled);
        runOnMainSync(()->toggle().performClick());await("default restores unset permission",()->StockOverlayShellService.call(binder,StockOverlayShellService.READ,null).equals("default"));await("final backup clean",()->backup.original()==null);
        try(android.os.ParcelFileDescriptor fd=getUiAutomation().executeShellCommand("appops set --uid com.syu.canbus SYSTEM_ALERT_WINDOW allow");java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
        rejected=false;try{StockOverlayShellService.call(binder,StockOverlayShellService.SET,"ignore");}catch(Exception e){rejected=true;}check("UID override is detected and not changed",rejected);check("UID override creates no backup",backup.original()==null);
        try(android.os.ParcelFileDescriptor fd=getUiAutomation().executeShellCommand("appops set --uid com.syu.canbus SYSTEM_ALERT_WINDOW default");java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
        check("package mode preserved with UID override",StockOverlayShellService.call(binder,StockOverlayShellService.READ,null).equals("default"));
        runOnMainSync(()->button(R.string.stock_connect).performClick());await("final UI ready",this::enabled);
        runOnMainSync(()->{View box=(View)toggle().getParent();box.requestRectangleOnScreen(new android.graphics.Rect(0,0,box.getWidth(),box.getHeight()),true);});SystemClock.sleep(400);
        android.graphics.Bitmap screen=getUiAutomation().takeScreenshot();try(java.io.OutputStream out=new java.io.FileOutputStream(new java.io.File(c.getExternalFilesDir(null),"honda-box-v084-stock-overlay-TEST.png"))){screen.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}screen.recycle();
        result.putString("result","PASS "+checks+" stock-overlay shell integration checks");finish(Activity.RESULT_OK,result);
    }catch(Throwable error){String extra="";try{java.lang.reflect.Field field=SettingsActivity.class.getDeclaredField("stockOverlays");field.setAccessible(true);Object controller=field.get(activity);for(String name:new String[]{"mode","error","busy","binding"}){java.lang.reflect.Field f=StockOverlaySettings.class.getDeclaredField(name);f.setAccessible(true);extra+=" "+name+"="+f.get(controller);}extra+=" backup="+new StockOverlayBackup(getTargetContext().getSharedPreferences("stock-overlay",0)).original();}catch(Exception ignored){}result.putString("result","FAIL "+checks+": "+error+extra);finish(Activity.RESULT_CANCELED,result);}finally{if(args!=null&&connection!=null)try{Shizuku.unbindUserService(args,connection,true);}catch(Exception ignored){}}}
}
