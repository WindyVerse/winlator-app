package com.winlator.xserver.extensions;

/**
 * A client's xshmfence received through DRI3 FenceFromFD. Mesa waits on it before
 * reusing a swapchain image; the X server triggers it when Present reports the image idle.
 */
public final class ShmFence {
    static { System.loadLibrary("winlator"); }

    private long address;

    private ShmFence(long address) {
        this.address = address;
    }

    /** Maps the fence and closes the descriptor; returns null when it cannot be mapped. */
    public static ShmFence map(int fd) {
        long address = nativeMap(fd);
        return address == 0 ? null : new ShmFence(address);
    }

    public synchronized void trigger() {
        if (address != 0) nativeTrigger(address);
    }

    public synchronized void close() {
        if (address == 0) return;
        nativeUnmap(address);
        address = 0;
    }

    private static native long nativeMap(int fd);
    private static native void nativeTrigger(long address);
    private static native void nativeUnmap(long address);
}
