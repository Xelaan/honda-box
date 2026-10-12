package com.crewbeat.hondabox;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/** FYT IPC subset for subscriptions and the explicitly selected stock audio commands. */
public final class FytProtocol {
    public static final String TOOLKIT = "com.syu.ipc.IRemoteToolkit";
    public static final String MODULE = "com.syu.ipc.IRemoteModule";
    public static final String CALLBACK = "com.syu.ipc.IModuleCallback";
    public interface Listener { void update(int code, int[] ints, float[] floats, String[] strings); }

    public static IBinder canModule(IBinder toolkit) throws RemoteException {
        return remoteModule(toolkit, 7);
    }
    public static IBinder remoteModule(IBinder toolkit, int moduleCode) throws RemoteException {
        requireDescriptor(toolkit, TOOLKIT);
        Parcel in = Parcel.obtain(), out = Parcel.obtain();
        try {
            in.writeInterfaceToken(TOOLKIT);
            in.writeInt(moduleCode);
            if (!toolkit.transact(1, in, out, 0)) throw new RemoteException("getRemoteModule unsupported");
            out.readException();
            IBinder module = out.readStrongBinder();
            if (module == null) throw new RemoteException("Module " + moduleCode + " is null");
            requireDescriptor(module, MODULE);
            return module;
        } finally { in.recycle(); out.recycle(); }
    }
    public static void command(IBinder module, int code, int[] args) throws RemoteException {
        Parcel data = Parcel.obtain();
        try {
            data.writeInterfaceToken(MODULE); data.writeInt(code); data.writeIntArray(args);
            data.writeFloatArray(null); data.writeStringArray(null);
            // Stock cmd is one-way. Acceptance is not a hardware acknowledgement.
            if (!module.transact(1, data, null, IBinder.FLAG_ONEWAY)) throw new RemoteException("cmd transaction unsupported");
        } finally { data.recycle(); }
    }

    private static void requireDescriptor(IBinder binder, String expected) throws RemoteException {
        String actual = binder.getInterfaceDescriptor();
        if (!expected.equals(actual)) throw new RemoteException("Unexpected Binder descriptor: " + actual);
    }

    public static void register(IBinder module, IBinder callback, int code) throws RemoteException {
        subscription(module, callback, code, true);
    }
    public static void unregister(IBinder module, IBinder callback, int code) throws RemoteException {
        subscription(module, callback, code, false);
    }
    private static void subscription(IBinder module, IBinder callback, int code, boolean register) throws RemoteException {
        Parcel data = Parcel.obtain();
        try {
            data.writeInterfaceToken(MODULE);
            data.writeStrongBinder(callback);
            data.writeInt(code);
            if (register) data.writeInt(1); // Request the existing value as well as future changes.
            if (!module.transact(register ? 3 : 4, data, null, IBinder.FLAG_ONEWAY))
                throw new RemoteException("Subscription transaction unsupported");
        } finally { data.recycle(); }
    }

    public static final class Callback extends Binder {
        private final Listener listener;
        public Callback(Listener listener) { this.listener = listener; attachInterface(null, CALLBACK); }
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) { if (reply != null) reply.writeString(CALLBACK); return true; }
            if (code != 1) return super.onTransact(code, data, reply, flags);
            data.enforceInterface(CALLBACK);
            int updateCode = data.readInt();
            int[] ints = data.createIntArray();
            float[] floats = data.createFloatArray();
            String[] strings = data.createStringArray();
            listener.update(updateCode, ints, floats, strings);
            if (reply != null) reply.writeNoException();
            return true;
        }
    }
}
