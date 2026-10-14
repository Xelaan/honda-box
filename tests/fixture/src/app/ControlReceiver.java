package app;
import android.content.*;
import android.os.*;
public class ControlReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        if(i.hasExtra("fytAcc")){c.sendBroadcast(new Intent(i.getIntExtra("fytAcc",0)==1?"com.fyt.boot.ACCON":"com.fyt.boot.ACCOFF").setPackage("com.crewbeat.hondabox"));return;}
        if(i.getBooleanExtra("getCommands",false)){setResultData(ToolkitService.commandReport());return;}
        if(i.getBooleanExtra("getRoutes",false)){setResultData(ToolkitService.routeReport());return;}
        if(i.hasExtra("simulateSourceCommands"))ToolkitService.simulateSourceCommands=i.getBooleanExtra("simulateSourceCommands",false);
        if(i.hasExtra("noRouteModules"))ToolkitService.noRouteModules=i.getBooleanExtra("noRouteModules",false);
        if(i.hasExtra("routeDelayMs"))ToolkitService.routeDelayMs=i.getIntExtra("routeDelayMs",0);
        if(i.hasExtra("routeScreen"))ToolkitService.ROUTE_VALUES.put("0:168",new int[]{i.getIntExtra("routeScreen",0)});
        if(i.hasExtra("routeMain"))ToolkitService.ROUTE_VALUES.put("0:0",new int[]{i.getIntExtra("routeMain",0)});
        if(i.hasExtra("ipodState"))ToolkitService.ROUTE_VALUES.put("5:0",new int[]{i.getIntExtra("ipodState",0)});
        if(i.hasExtra("ipodExists"))ToolkitService.ROUTE_VALUES.put("5:15",new int[]{i.getIntExtra("ipodExists",0)});
        if(i.hasExtra("ipodTitle"))ToolkitService.ROUTE_STRINGS.put("5:8",new String[]{i.getStringExtra("ipodTitle")});
        if(i.getBooleanExtra("lateRoute",false)){if(ToolkitService.lateMain!=null)ToolkitService.emitRoute("0:0",ToolkitService.lateMain);return;}
        if(i.getBooleanExtra("resetCommands",false)){ToolkitService.COMMANDS.clear();setResultData("[]");return;}
        if(i.getBooleanExtra("kill",false)){new Handler(Looper.getMainLooper()).postDelayed(()->android.os.Process.killProcess(android.os.Process.myPid()),100);return;}
        if(i.getBooleanExtra("getTouches",false)){setResultData(String.valueOf(c.getSharedPreferences("touches",0).getInt("count",0)));return;}
        if(i.hasExtra("raw"))ToolkitService.raw=i.getIntArrayExtra("raw");
        String[] keys={"mediaState","repeat","profile","source","band","volume","left","right","power","ac","auto","dual","recirc","rear","front","fan","windshield","face","feet","flowAuto","cycleAuto","blowAuto","track","disc","minute","second","frequency","preset1","preset2","preset3","preset4","preset5","preset6","animation","menuShow","menuType"};int[] codes={114,102,1000,98,115,132,27,28,10,11,13,14,12,16,17,21,18,19,20,33,36,49,101,113,99,100,119,120,121,122,123,124,125,145,139,138};
        for(int k=0;k<keys.length;k++)if(i.hasExtra(keys[k]))ToolkitService.VALUES.put(codes[k],i.getIntExtra(keys[k],0));
        String[] names={"title","album","artist"};for(int k=0;k<names.length;k++)if(i.hasExtra(names[k]))ToolkitService.STRINGS.put(103+k,i.getStringExtra(names[k]));
        for(int k=0;k<3;k++)if(i.hasExtra("menu"+(k+1)))ToolkitService.STRINGS.put(135+k,i.getStringExtra("menu"+(k+1)));
        if(i.hasExtra("silent"))ToolkitService.silent=i.getBooleanExtra("silent",false);
        if(i.hasExtra("nullTemp"))ToolkitService.nullTemp=i.getBooleanExtra("nullTemp",false);
        if(!i.getBooleanExtra("routeOnly",false))ToolkitService.emitAll();
        ToolkitService.emitRoutes();
    }
}
