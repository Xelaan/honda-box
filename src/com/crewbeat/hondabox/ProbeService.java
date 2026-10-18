package com.crewbeat.hondabox;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import java.util.ArrayList;
import java.util.List;

public class ProbeService extends Service {
    public static final String COMPATIBILITY="com.crewbeat.hondabox.COMPATIBILITY";
    public static final String STOP = "com.crewbeat.hondabox.STOP";
    public static final String OVERLAY = "com.crewbeat.hondabox.OVERLAY";
    private CarMenuOverlay carMenuOverlay;private AudioSourceController audioScreens;private VolumeOverlay overlay;private SourceOverlay sourceOverlay;private ClimateOverlay climateOverlay;private ClimateStripOverlay climateStrip;
    public static final String COMMAND = "com.crewbeat.hondabox.COMMAND";
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ProbeState state = ProbeState.INSTANCE;
    private HandlerThread thread; private Handler worker;
    private ServiceConnection binding; private IBinder module, toolkitBinder; private FytProtocol.Callback callback;
    private IBinder.DeathRecipient death;
    private final List<Integer> subscribed = new ArrayList<>();
    private final List<RouteSubscription> routeSubscriptions = new ArrayList<>();
    private static final class RouteSubscription {
        IBinder binder; FytProtocol.Callback callback; final List<Integer> codes = new ArrayList<>(); boolean closed;
    }
    private AuxPlaybackController androidMedia;
    private TrackOverlay trackOverlay;private UnknownOverlay unknownOverlay;
    private boolean active; private int generation;
    private long lastWakeAt=-1;
    private final BroadcastReceiver wakeReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
        String action=i==null?null:i.getAction();
        if(BackgroundRecovery.ACC_OFF.equals(action)){
            BackgroundRecovery.notePower(c,false);androidMedia.reset();final PendingResult pending=goAsync();
            if(!worker.post(()->{try{prepareForSleep();}finally{pending.finish();}}))pending.finish();
        }else if(Intent.ACTION_SCREEN_ON.equals(action)||BackgroundRecovery.ACC_ON.equals(action)){
            BackgroundRecovery.notePower(c,true);worker.post(()->wakeUp(action));
        }
    }};
    private void prepareForSleep(){
        lastWakeAt=-1;
        worker.removeCallbacks(retry);worker.removeCallbacks(watchdog);
        worker.removeCallbacks(captureLogicalMenu);worker.removeCallbacks(saveMenus);worker.removeCallbacks(savePresets);menuSaveQueued=false;presetSaveQueued=false;
        state.captureLogicalMenu();
        try{
            getSharedPreferences("carMenuMemory",MODE_PRIVATE).edit().putString(cacheKey(state.carMenuMemory.profile()),state.carMenuMemory.json().toString()).commit();savedMenuRevision=state.carMenuMemory.revision();
            getSharedPreferences("radioPresets",MODE_PRIVATE).edit().putString(cacheKey(state.radioPresets.profile()),state.radioPresets.json().toString()).commit();savedPresetRevision=state.radioPresets.revision();
        }catch(Exception e){state.log("ACC OFF CACHE: "+e);}
        state.log("ACC OFF: caches saved; reconnect retries suspended");
    }
    private void wakeUp(){BackgroundRecovery.notePower(this,true);wakeUp("fyt_screen");}
    private void wakeUp(String reason){
        if(!active||!BackgroundSettings.wanted(this)||!BackgroundRecovery.awake(this))return;
        long now=SystemClock.elapsedRealtime();if(lastWakeAt>=0&&now-lastWakeAt<2000)return;lastWakeAt=now;
        // Suppress player navigation even if subscriptions remained connected through sleep.
        ui.post(()->{audioScreens.backgroundStart();sourceOverlay.announceCurrentSource();});
        // ACC OFF cancels the timeout. Re-arm it on wake without discarding an in-flight bind.
        if(binding!=null&&module==null){worker.removeCallbacks(watchdog);worker.postDelayed(watchdog,10000);return;}
        ProbeState.Snapshot current=state.overlaySnapshot();
        boolean fresh=current.connected&&module!=null&&module.isBinderAlive()&&System.currentTimeMillis()-current.lastEvent<2000;
        if(fresh)return;
        state.status(I18n.t(R.string.ui_connecting),"",false);
        state.log("WAKE "+reason+": reconnecting read-only FYT subscriptions");disconnect();connect();
    }


    private CommandRun commandRun;
    private final Runnable captureLogicalMenu=()->captureMenuAndSave();
    private void captureMenuAndSave(){state.captureLogicalMenu();saveMenus.run();}
    private String cacheKey(int profile){return profile==AccordCompatibility.PROFILE?"received":"received."+profile;}
    private void restoreProfileCaches(){
        int profile=state.radioPresets.profile();if(profile<=0)return;
        state.radioPresets.restore(getSharedPreferences("radioPresets",MODE_PRIVATE).getString(cacheKey(profile),""));
        state.carMenuMemory.restore(getSharedPreferences("carMenuMemory",MODE_PRIVATE).getString(cacheKey(profile),""));
    }
    private long savedMenuRevision=-1;private boolean menuSaveQueued;
    private final Runnable saveMenus=()->{menuSaveQueued=false;try{long revision=state.carMenuMemory.revision();if(state.carMenuMemory.profile()>0&&revision!=savedMenuRevision){getSharedPreferences("carMenuMemory",MODE_PRIVATE).edit().putString(cacheKey(state.carMenuMemory.profile()),state.carMenuMemory.json().toString()).apply();savedMenuRevision=revision;}}catch(Exception e){state.log("MENU CACHE ERROR: "+e);}};
    private long savedPresetRevision=-1;private boolean presetSaveQueued;
    private final Runnable savePresets=()->{presetSaveQueued=false;try{long revision=state.radioPresets.revision();if(state.radioPresets.profile()>0&&revision!=savedPresetRevision){getSharedPreferences("radioPresets",MODE_PRIVATE).edit().putString(cacheKey(state.radioPresets.profile()),state.radioPresets.json().toString()).apply();savedPresetRevision=revision;}}catch(Exception e){state.log("PRESET CACHE ERROR: "+e);}};
    private static final class CommandRun {
        long id,routeStartedAt,modeSentAt; int generation; IBinder can; AudioCommand action; AudioCommandOptions options; boolean pressed;
        Runnable next, finish;
    }
    private final Runnable retry = () -> connect();
    private final Runnable watchdog = () -> fail(I18n.t(R.string.ui_no_service_response), I18n.t(R.string.ui_connection_did_not_complete_within), true);

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel("probe", I18n.t(R.string.ui_fyt_diagnostics), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(I18n.t(R.string.ui_background_head_unit_data_capture)); getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        // Fulfil startForegroundService immediately, including rejected/racing start requests.
        showNotification();
        state.processAsHondaAccord(AccordCompatibility.enabled(this));state.recording(DiagnosticSettings.enabled(this));state.carMenuMemory.restore(getSharedPreferences("carMenuMemory",MODE_PRIVATE).getString("received",""));state.radioPresets.restore(getSharedPreferences("radioPresets",MODE_PRIVATE).getString("received",""));state.auxSource(SourceSettings.aux(this));state.auxName(SourceSettings.auxName(this));unknownOverlay=new UnknownOverlay(this);trackOverlay=new TrackOverlay(this);carMenuOverlay=new CarMenuOverlay(this); audioScreens=new AudioSourceController(this);androidMedia=new AuxPlaybackController(this); overlay = new VolumeOverlay(this);sourceOverlay=new SourceOverlay(this);climateOverlay=new ClimateOverlay(this);climateStrip=new ClimateStripOverlay(this,climateOverlay); thread = new HandlerThread("FytProbeIPC"); thread.start(); worker = new Handler(thread.getLooper());IntentFilter wakeFilter=new IntentFilter(Intent.ACTION_SCREEN_ON);wakeFilter.addAction(BackgroundRecovery.ACC_ON);wakeFilter.addAction(BackgroundRecovery.ACC_OFF);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(wakeReceiver,wakeFilter,Context.RECEIVER_EXPORTED);else registerReceiver(wakeReceiver,wakeFilter);

    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP.equals(intent.getAction())) { BackgroundSettings.pause(this);stopSelf(); return START_NOT_STICKY; }
        if(intent!=null&&COMPATIBILITY.equals(intent.getAction())){
            if(!state.overlaySnapshot().running){state.processAsHondaAccord(AccordCompatibility.enabled(this));stopSelf();return START_NOT_STICKY;}
            audioScreens.backgroundStart();androidMedia.reset();
            worker.post(()->{
                boolean on=AccordCompatibility.enabled(this);
                if(on==state.overlaySnapshot().processAsHondaAccord)return;
                saveMenus.run();savePresets.run();disconnect();state.processAsHondaAccord(on);connect();
            });
            return START_STICKY;
        }
        if (intent != null && OVERLAY.equals(intent.getAction())) {
            if (!state.snapshot().running) { stopSelf(); return START_NOT_STICKY; }
            showNotification();unknownOverlay.refreshOptions();audioScreens.refreshOptions();overlay.refreshOptions();sourceOverlay.refreshOptions();climateOverlay.refreshOptions();climateStrip.refreshOptions();carMenuOverlay.refreshOptions();
            if(intent.getBooleanExtra("trackDemo",false))trackOverlay.demo();
            if (intent.getBooleanExtra("demo",false)) overlay.demo();
            if(intent.getBooleanExtra("climateDemo",false))climateOverlay.demo();
            if(intent.getBooleanExtra("climateStripDemo",false))climateStrip.demo();
            if(intent.getBooleanExtra("carMenuDemo",false))carMenuOverlay.demo();
            return START_STICKY;
        }
        if (intent != null && COMMAND.equals(intent.getAction())) {
            final String name = intent.getStringExtra("command");
            final boolean debugCommand=intent.getBooleanExtra("audioDebug",false);
            final String transport=intent.getStringExtra("audioTransport"),route=intent.getStringExtra("audioRoute");
            final int hold=intent.getIntExtra("audioHoldMs",180);
            worker.post(() -> {
                try { sendCommand(AudioCommand.valueOf(name == null ? "" : name),debugCommand?AudioCommandOptions.debug(transport,route,hold):AudioCommandOptions.STANDARD); }
                catch (IllegalArgumentException | NullPointerException e) { state.commandRejected(I18n.t(R.string.ui_unknown_command_nothing_sent)); }
            });
            // A command never starts or reconnects a diagnostic session by itself.
            if (!state.snapshot().running) stopSelf();
            return state.snapshot().running?START_STICKY:START_NOT_STICKY;
        }
        final String automaticReason=intent==null?null:intent.getStringExtra(BackgroundRecovery.REASON);
        // Recheck at service delivery too: STOP may race a previously queued automatic start.
        if(automaticReason!=null&&!BackgroundRecovery.mayStart(this,automaticReason)){
            boolean running=state.overlaySnapshot().running;if(!running)stopSelf(startId);return running?START_STICKY:START_NOT_STICKY;
        }
        if(intent==null&&!BackgroundSettings.wanted(this)){stopSelf();return START_NOT_STICKY;}
        if(intent!=null){if(automaticReason==null)BackgroundRecovery.notePower(this,true);BackgroundSettings.activate(this);}else BackgroundRecovery.schedule(this);
        String startReason=intent==null?"sticky_restart":automaticReason==null?"manual":automaticReason;
        getSharedPreferences("background",MODE_PRIVATE).edit().putString("lastStartReason",startReason).putLong("lastStartMs",System.currentTimeMillis()).apply();
        state.log("SERVICE START: "+startReason);
        if(intent==null||(intent!=null&&intent.getBooleanExtra("backgroundStart",false)))audioScreens.backgroundStart();
        showNotification();
        unknownOverlay.start();trackOverlay.start();audioScreens.start();androidMedia.start();overlay.start();sourceOverlay.start();climateStrip.start();climateOverlay.start();carMenuOverlay.start();
        worker.post(() -> { if (!active) { active = true; state.running(true); state.newSession(); connect(); }else wakeUp(automaticReason==null?"service_start":automaticReason); });
        return START_STICKY;
    }
    private void showNotification(){
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, ProbeService.class).setAction(STOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "probe") : new Notification.Builder(this);
        startForeground(71, builder.setSmallIcon(R.drawable.ic_console_notification).setContentTitle(I18n.t(R.string.ui_fyt_probe_diagnostics_active))
            .setContentText(I18n.t(R.string.ui_reading_vehicle_data_tap_to)).setContentIntent(pending)
            .setOngoing(true).addAction(new Notification.Action.Builder(null, I18n.t(R.string.ui_stop), stop).build()).build());
    }
    private void connect() {
        if (!active || !BackgroundRecovery.awake(this) || binding != null) return;
        worker.removeCallbacks(retry); state.newSession();
        ComponentName component = new ComponentName("com.syu.ms", "app.ToolkitService");
        try {
            PackageInfo p = getPackageManager().getPackageInfo(component.getPackageName(), 0);
            ServiceInfo s = getPackageManager().getServiceInfo(component, 0);
            state.backend(getPackageManager().getApplicationLabel(p.applicationInfo) + " · " + p.versionName + " (" + p.versionCode + ") · exported=" + s.exported + " · permission=" + s.permission);
            if (!s.enabled || !s.applicationInfo.enabled) { fail(I18n.t(R.string.ui_service_disabled), I18n.t(R.string.ui_toolkitservice_is_disabled_in_android), false); return; }
            if (!s.exported) { fail(I18n.t(R.string.ui_access_denied), I18n.t(R.string.ui_toolkitservice_is_not_exported_to), false); return; }
            if (s.permission != null && checkSelfPermission(s.permission) != PackageManager.PERMISSION_GRANTED) {
                fail(I18n.t(R.string.ui_service_permission_required), s.permission + I18n.t(R.string.ui_check_the_log_the_app), false); return;
            }
        } catch (PackageManager.NameNotFoundException e) {
            state.backend(I18n.t(R.string.ui_com_syu_ms_app_toolkitservice));
            fail(I18n.t(R.string.ui_fyt_service_not_found), I18n.t(R.string.ui_this_is_expected_on_a), true); return;
        } catch (RuntimeException e) { fail(I18n.t(R.string.ui_service_check_error), e.toString(), false); return; }
        final int session = ++generation;
        state.status(I18n.t(R.string.ui_connecting), I18n.t(R.string.ui_requesting_can_module_7_from), false);
        binding = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder toolkit) { worker.post(() -> establish(session, toolkit)); }
            @Override public void onServiceDisconnected(ComponentName name) { worker.post(() -> { if (session == generation) fail(I18n.t(R.string.ui_connection_lost), I18n.t(R.string.ui_fyt_service_disconnected_retrying_in), true); }); }
            @Override public void onBindingDied(ComponentName name) { onServiceDisconnected(name); }
            @Override public void onNullBinding(ComponentName name) { worker.post(() -> { if (session == generation) fail(I18n.t(R.string.ui_service_refused_connection), I18n.t(R.string.ui_toolkitservice_returned_a_null_binder), false); }); }
        };
        try {
            boolean accepted = bindService(new Intent("com.syu.ms.toolkit").setComponent(component), binding, BIND_AUTO_CREATE);
            if (!accepted) { fail(I18n.t(R.string.ui_connection_rejected), I18n.t(R.string.ui_bindservice_returned_false_retrying_in), true); return; }
            worker.postDelayed(watchdog, 10000);
        } catch (SecurityException e) { fail(I18n.t(R.string.ui_access_denied), e.toString(), false); }
          catch (RuntimeException e) { fail(I18n.t(R.string.ui_connection_error), e.toString(), true); }
    }
    private void establish(int session, IBinder toolkit) {
        if (!active || session != generation) return;
        worker.removeCallbacks(watchdog);
        try {
            module = FytProtocol.canModule(toolkit);
            toolkitBinder = toolkit;
            death = () -> worker.post(() -> { if (session == generation) fail(I18n.t(R.string.ui_can_module_disconnected), I18n.t(R.string.ui_binder_terminated_retrying_in_5), true); });
            module.linkToDeath(death, 0);
            callback = new FytProtocol.Callback((code, ints, floats, strings) -> worker.post(() -> {
                if (active && session == generation && module != null){
                    if(code==1000){saveMenus.run();savePresets.run();}
                    int oldCacheProfile=state.radioPresets.profile();state.event(code, ints, floats, strings);
                    if(code==1000&&oldCacheProfile!=state.radioPresets.profile())restoreProfileCaches();if(code>=135&&code<=139){worker.removeCallbacks(captureLogicalMenu);worker.postDelayed(captureLogicalMenu,200);}if(!menuSaveQueued&&state.carMenuMemory.revision()!=savedMenuRevision){menuSaveQueued=true;worker.postDelayed(saveMenus,1000);}if(code==1019||(code>=120&&code<=125)){if(!presetSaveQueued&&state.radioPresets.revision()!=savedPresetRevision){presetSaveQueued=true;worker.postDelayed(savePresets,1000);}}}
            }));
            state.status(I18n.t(R.string.ui_subscription_sent), I18n.t(R.string.ui_binder_available_waiting_for_callbacks), true);
            // The stock Honda callback subscribes to the complete 0..147 range.
            // Keep unknown fields raw; receiving an initial zero does not prove support.
            FytProtocol.register(module, callback, 1000); subscribed.add(1000);
            FytProtocol.register(module, callback, 1037); subscribed.add(1037);
            FytProtocol.register(module, callback, 1019); subscribed.add(1019);
            // Multiplexed CAN callback: subtype 0 carries the stock normalized steering value.
            FytProtocol.register(module, callback, 1049); subscribed.add(1049);
            for (int code = 0; code < 148; code++) { FytProtocol.register(module, callback, code); subscribed.add(code); }
            state.audioRoutes.start();
            subscribeRoute(session,toolkit,AudioRouteCapture.MAIN);
            subscribeRoute(session,toolkit,AudioRouteCapture.IPOD);
            worker.postDelayed(() -> {
                if (active && session == generation && state.snapshot().count == 0)
                    state.status(I18n.t(R.string.ui_connected_no_data), I18n.t(R.string.ui_no_callbacks_within_10_seconds), true);
            }, 10000);
        } catch (SecurityException e) { fail(I18n.t(R.string.ui_can_access_denied), e.toString(), false); }
          catch (Exception e) { fail(I18n.t(R.string.ui_fyt_protocol_error), e.toString(), true); }
    }
    private void subscribeRoute(int session,IBinder toolkit,int moduleCode) {
        RouteSubscription subscription=new RouteSubscription();
        try {
            subscription.binder=FytProtocol.remoteModule(toolkit,moduleCode);
            subscription.callback=new FytProtocol.Callback((code,ints,floats,strings)->worker.post(()->{
                if(active&&session==generation&&!subscription.closed){Integer before=moduleCode==0&&code==168?state.audioRoutes.mainScreenOn():null;state.audioRoutes.event(moduleCode,code,ints,floats,strings);if(Integer.valueOf(0).equals(before)&&Integer.valueOf(1).equals(state.audioRoutes.mainScreenOn()))wakeUp();}
            }));
            routeSubscriptions.add(subscription);
            for(int code:AudioRouteCapture.codes(moduleCode)) {
                FytProtocol.register(subscription.binder,subscription.callback,code); subscription.codes.add(code);
            }
            state.audioRoutes.status(moduleCode,"Read-only subscription sent; callbacks may be unavailable");
        } catch(Exception e) {
            closeRoute(subscription); routeSubscriptions.remove(subscription);
            state.audioRoutes.status(moduleCode,"Unavailable: "+e);
            state.log(I18n.t(R.string.ui_additional_audio_module)+moduleCode+": "+e+I18n.t(R.string.ui_can_reading_continues));
        }
    }
    private void closeRoute(RouteSubscription subscription) {
        subscription.closed=true;
        for(int code:subscription.codes) try {
            FytProtocol.unregister(subscription.binder,subscription.callback,code);
        } catch(Exception ignored) { }
        subscription.codes.clear();
    }
    private void fail(String title, String detail, boolean again) {
        disconnect(); state.status(title, detail, false);
        if (active && again && BackgroundRecovery.awake(this)) worker.postDelayed(retry, 5000);
    }
    private void sendCommand(AudioCommand action,AudioCommandOptions options) {
        ProbeState.Snapshot s = state.snapshot();
        if (!active || !s.running || module == null || !s.connected) { state.commandRejected(I18n.t(R.string.ui_not_connected_command_not_sent)); return; }
        if (!s.accord()) { state.commandRejected(I18n.t(R.string.ui_commands_are_available_only_for)); return; }
        if (commandRun != null) { state.commandRejected(I18n.t(R.string.ui_previous_command_still_in_progress)); return; }
        final CommandRun run = new CommandRun();
        run.id=state.beginCommand(action.title()+(options.debug?" · "+options.description():""));
        run.generation=generation;run.can=module;run.action=action;run.options=options;commandRun=run;
        state.commandOptions(run.id,options.transport.name(),options.route.name(),options.holdMs,options.debug);
        try{
            if(!options.setsMain(action)){dispatchAudio(run);return;}
            IBinder main=FytProtocol.remoteModule(toolkitBinder,0);
            run.routeStartedAt=SystemClock.uptimeMillis();transmit(run,main,0,0,new int[]{13});
            run.next=()->{
                if(run!=commandRun||run.generation!=generation||!active)return;
                long elapsed=SystemClock.uptimeMillis()-run.routeStartedAt;
                if(elapsed<options.delayMs(action)||(options.waitsForMain(action)&&!Integer.valueOf(13).equals(state.audioRoutes.mainChannel()))){
                    if(elapsed>=3000){endCommand(run,I18n.t(R.string.ui_vehicle_audio_channel_13_not)+state.audioRoutes.mainChannel()+I18n.t(R.string.debug_audio_no_send));return;}
                    worker.postDelayed(run.next,50);return;
                }
                try{dispatchAudio(run);}catch(Exception e){endCommand(run,I18n.t(R.string.ui_command_transmission_error)+e);}
            };
            worker.postDelayed(run.next,options.delayMs(action));
        }catch(Exception e){endCommand(run,I18n.t(R.string.ui_command_transmission_error)+e);}
    }
    private boolean sameCommandTarget(CommandRun run){
        ProbeState.Snapshot current=state.overlaySnapshot();
        return run==commandRun&&run.generation==generation&&active&&module==run.can&&current.running&&current.connected&&current.accord();
    }
    private void dispatchAudio(CommandRun run)throws RemoteException{
        if(!sameCommandTarget(run)){endCommand(run,I18n.t(R.string.debug_audio_target_changed));return;}
        run.modeSentAt=System.currentTimeMillis();
        state.log("AUDIO DISPATCH MAIN="+state.audioRoutes.mainChannel()+" · "+run.options.transport+" / "+run.options.route);
        transmit(run,run.can,7,run.options.command(run.action),run.options.args(run.action,true));
        if(run.action.selectsSource()){waitForObservation(run);return;}
        run.pressed=true;
        run.next=()->{if(run!=commandRun)return;String error=releaseKey(run);if(error!=null)endCommand(run,error);else waitForObservation(run);};
        worker.postDelayed(run.next,run.options.holdMs);
    }
    private void transmit(CommandRun run, IBinder remote, int moduleCode, int code, int[] args) throws RemoteException {
        try { FytProtocol.command(remote,code,args); state.commandStep(run.id,moduleCode,code,args,"one-way IPC accepted; hardware unconfirmed"); }
        catch (RemoteException | RuntimeException e) { state.commandStep(run.id,moduleCode,code,args,"ERROR: " + e); throw e; }
    }
    private String releaseKey(CommandRun run) {
        if (!run.pressed) return null;
        run.pressed = false; // A failed release is logged, never replayed on another connection.
        if(!sameCommandTarget(run))return I18n.t(R.string.debug_audio_target_changed);
        try { transmit(run,run.can,7,run.options.command(run.action),run.options.args(run.action,false)); return null; }
        catch (Exception e) { return I18n.t(R.string.ui_key_release_error) + e; }
    }
    private void waitForObservation(CommandRun run) {
        if(!run.action.selectsSource()){run.finish=()->endCommand(run,run.action.title()+I18n.t(R.string.ui_sent_to_ipc_execution_by));worker.postDelayed(run.finish,1200);return;}
        run.finish=()->{if(run!=commandRun)return;ProbeState.Snapshot s=state.snapshot();ProbeState.Value source=s.reading(98);
            if(source!=null&&source.at>run.modeSentAt&&Integer.valueOf(run.action.mode+1).equals(s.audioSource())){endCommand(run,I18n.t(R.string.ui_received_source_state)+s.source()+I18n.t(R.string.ui_after_sending_the_command_this));return;}
            if(System.currentTimeMillis()-run.modeSentAt>=2000){endCommand(run,run.action.title()+I18n.t(R.string.ui_sent_to_ipc_execution_by_137)+s.mainChannel+", CAN source="+s.audioSource()+".");return;}
            worker.postDelayed(run.finish,100);};worker.postDelayed(run.finish,100);
    }
    private void endCommand(CommandRun run, String result) {
        if (run != commandRun) return;
        if (run.next != null) worker.removeCallbacks(run.next);
        if (run.finish != null) worker.removeCallbacks(run.finish);
        String releaseError = releaseKey(run);
        commandRun = null; state.finishCommand(run.id,result + (releaseError == null ? "" : " · " + releaseError));
    }
    private void disconnect() {
        if (commandRun != null) endCommand(commandRun,I18n.t(R.string.ui_operation_interrupted_by_disconnect_no));
        generation++; worker.removeCallbacks(watchdog); worker.removeCallbacks(retry);
        for(RouteSubscription subscription:routeSubscriptions) closeRoute(subscription);
        routeSubscriptions.clear(); state.audioRoutes.stop();
        if (module != null) {
            for (int code : subscribed) try { FytProtocol.unregister(module, callback, code); } catch (Exception ignored) { }
            if (death != null) try { module.unlinkToDeath(death, 0); } catch (Exception ignored) { }
        }
        subscribed.clear(); module = null; toolkitBinder = null; callback = null; death = null;
        if (binding != null) { try { unbindService(binding); } catch (IllegalArgumentException ignored) { } binding = null; }
    }
    @Override public void onDestroy() {
        try{unregisterReceiver(wakeReceiver);}catch(IllegalArgumentException ignored){}
        unknownOverlay.close();trackOverlay.close();audioScreens.close();androidMedia.close();overlay.close();sourceOverlay.close();climateStrip.close();climateOverlay.close();carMenuOverlay.close(); state.running(false);
        worker.post(() -> { active = false; disconnect();worker.removeCallbacks(saveMenus);saveMenus.run();worker.removeCallbacks(savePresets);savePresets.run(); state.status(I18n.t(R.string.ui_stopped), I18n.t(R.string.ui_background_capture_stopped_you_can), false); thread.quitSafely(); });
        stopForeground(true); super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
