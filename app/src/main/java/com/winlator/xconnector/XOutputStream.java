package com.winlator.xconnector;

import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import com.winlator.core.ProcessHelper;
import com.winlator.xserver.XServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

import dalvik.annotation.optimization.CriticalNative;

public class XOutputStream implements XStreamLock {
    private final ReentrantLock lock = new ReentrantLock();
    private long nativePtr;
    private final int initialCapacity;
    private final ParcelFileDescriptor socket;
    private final int socketFd;
    private ParcelFileDescriptor ancillary;
    private final OrderedOutput writer;

    static {
        System.loadLibrary("winlator");
    }

    public XOutputStream(int clientFd, int initialCapacity) {
        this.initialCapacity = initialCapacity;
        try { socket = ParcelFileDescriptor.fromFd(clientFd); }
        catch (IOException error) { throw new IllegalStateException("Cannot duplicate game socket", error); }
        socketFd = socket.getFd();
        nativePtr = nativeAllocate(socketFd, initialCapacity);
        writer = new OrderedOutput(16 * 1024 * 1024, 1024, this::interruptSocket,
            () -> closeFd(socket), error -> ProcessHelper.emitDebugMessage("[X11 transport] " + error.getMessage()));
    }

    public void setAncillaryFd(int ancillaryFd) throws IOException {
        ParcelFileDescriptor replacement = ancillaryFd >= 0 ? ParcelFileDescriptor.fromFd(ancillaryFd) : null;
        closeFd(ancillary);
        ancillary = replacement;
        setAncillaryFd(nativePtr, replacement == null ? 0 : replacement.getFd());
    }

    public void writeByte(byte value) {
        writeByte(nativePtr, value);
    }

    public void writeShort(short value) {
        writeShort(nativePtr, value);
    }

    public void writeInt(int value) {
        writeInt(nativePtr, value);
    }

    public void writeLong(long value) {
        writeLong(nativePtr, value);
    }

    public void writeString8(String str) {
        byte[] bytes = str.getBytes(XServer.LATIN1_CHARSET);
        int length = -str.length() & 3;
        write(bytes);
        if (length > 0) writePad(length);
    }

    public void write(byte[] data) {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) writeByte(nativePtr, data[i]);
    }

    public void writeShortAt(int position, short value) {
        final byte[] data = {
            (byte)(value & 0xff),
            (byte)((value >> 8) & 0xff)
        };
        writeAt(nativePtr, position, data);
    }

    public void writeIntAt(int position, int value) {
        final byte[] data = {
            (byte)(value & 0xff),
            (byte)((value >>> 8) & 0xff),
            (byte)((value >>> 16) & 0xff),
            (byte)((value >>> 24) & 0xff)
        };
        writeAt(nativePtr, position, data);
    }

    public void writeAt(int position, byte[] data) {
        writeAt(nativePtr, position, data);
    }

    public void write(ByteBuffer data) {
        if (data.isDirect()) {
            writeByteBuffer(nativePtr, data, data.position(), data.remaining());
        }
        else {
            for (int i = data.position(), end = data.limit(); i < end; i++) {
                writeByte(nativePtr, data.get(i));
            }
        }
    }

    public void writePad(int length) {
        writePad(nativePtr, length);
    }

    public void writeFP3232(float value) {
        float tmp;
        tmp = (float)Math.floor(value);
        int integral = (int)tmp;
        tmp = (value - tmp) * (1L<<32);
        int frac = (int)tmp;
        writeInt(integral);
        writeInt(frac);
    }

    public XStreamLock lock() throws IOException {
        lock.lock();
        if (nativePtr == 0 || writer.isClosed()) {
            lock.unlock();
            throw new IOException("Game connection is closed");
        }
        return this;
    }

    public void destroy() {
        writer.close();
        lock.lock();
        try {
            destroy(nativePtr);
            nativePtr = 0;
            closeFd(ancillary);
            ancillary = null;
        } finally { lock.unlock(); }
    }

    @Override
    public void close() throws IOException {
        try {
            int size = length(nativePtr);
            if (size == 0) return;
            // Detach under the original protocol lock, so replies, pointer and
            // key events share one FIFO. Only the writer touches this packet.
            NativePacket packet = new NativePacket(nativePtr, Math.max(size, initialCapacity), ancillary);
            nativePtr = nativeAllocate(socketFd, initialCapacity);
            ancillary = null;
            writer.submit(packet);
        } finally { lock.unlock(); }
    }

    private static void closeFd(ParcelFileDescriptor fd) {
        if (fd != null) {
            try { fd.close(); } catch (IOException ignored) {}
        }
    }

    private void interruptSocket() {
        try { Os.shutdown(socket.getFileDescriptor(), OsConstants.SHUT_RDWR); }
        catch (ErrnoException ignored) {} // An already closed peer needs no wakeup.
    }

    private static final class NativePacket implements OrderedOutput.Packet {
        private final long pointer;
        private final int bytes;
        private final ParcelFileDescriptor ancillary;
        NativePacket(long pointer, int bytes, ParcelFileDescriptor ancillary) {
            this.pointer = pointer;
            this.bytes = bytes;
            this.ancillary = ancillary;
        }
        public int bytes() { return bytes; }
        public void send() throws IOException {
            if (!sendData(pointer)) throw new IOException("Failed to send game connection data");
        }
        public void dispose() {
            destroy(pointer);
            closeFd(ancillary);
        }
    }

    public int length() {
        return length(nativePtr);
    }

    private native long nativeAllocate(int fd, int initialCapacity);

    @CriticalNative
    private static native void setAncillaryFd(long nativePtr, int ancillaryFd);

    @CriticalNative
    private static native void writeByte(long nativePtr, byte value);

    @CriticalNative
    private static native void writeShort(long nativePtr, short value);

    @CriticalNative
    private static native void writeInt(long nativePtr, int value);

    @CriticalNative
    private static native void writeLong(long nativePtr, long value);

    @CriticalNative
    private static native void writePad(long nativePtr, int length);

    private static native void writeAt(long nativePtr, int position, byte[] data);

    private static native void writeByteBuffer(long nativePtr, ByteBuffer data, int offset, int length);

    @CriticalNative
    private static native boolean sendData(long nativePtr);

    @CriticalNative
    private static native void destroy(long nativePtr);

    @CriticalNative
    private static native int length(long nativePtr);
}
