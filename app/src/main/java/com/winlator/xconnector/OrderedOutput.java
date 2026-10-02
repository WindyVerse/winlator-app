package com.winlator.xconnector;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.function.Consumer;

/** One bounded FIFO per connection. Submission always consumes packet ownership. */
public final class OrderedOutput implements AutoCloseable {
    public interface Packet {
        int bytes();
        void send() throws IOException;
        void dispose();
    }

    private final ArrayDeque<Packet> pending = new ArrayDeque<>();
    private final int maxBytes;
    private final int maxPackets;
    private final Runnable interruptSend;
    private final Runnable releaseConnection;
    private final Consumer<IOException> reportFailure;
    private int bytes;
    private int packets;
    private boolean closed;

    public OrderedOutput(int maxBytes, int maxPackets, Runnable interruptSend,
                         Runnable releaseConnection, Consumer<IOException> reportFailure) {
        this.maxBytes = maxBytes;
        this.maxPackets = maxPackets;
        this.interruptSend = interruptSend;
        this.releaseConnection = releaseConnection;
        this.reportFailure = reportFailure;
        Thread writer = new Thread(this::drain, "pc-xoutput");
        writer.setDaemon(true);
        writer.start();
    }

    public synchronized boolean isClosed() { return closed; }

    public void submit(Packet packet) throws IOException {
        IOException error;
        synchronized (this) {
            if (closed) error = new IOException("Game connection is closed");
            else if (packet.bytes() > maxBytes - bytes || packets >= maxPackets)
                error = new IOException("Game connection output backlog exceeded its limit");
            else {
                bytes += packet.bytes();
                packets++;
                pending.addLast(packet);
                notifyAll();
                return;
            }
        }
        packet.dispose();
        stop(error);
        throw error;
    }

    private void stop(IOException error) {
        synchronized (this) {
            if (closed) return;
            closed = true;
            // Shutdown runs before waking the writer: it cannot close and reuse
            // the socket descriptor while another thread is interrupting it.
            interruptSend.run();
            notifyAll();
        }
        if (error != null) reportFailure.accept(error);
    }

    @Override public void close() { stop(null); }

    private void drain() {
        try {
            while (true) {
                Packet packet;
                synchronized (this) {
                    while (!closed && pending.isEmpty()) wait();
                    if (closed) return;
                    packet = pending.removeFirst();
                }
                try { packet.send(); }
                finally {
                    packet.dispose();
                    synchronized (this) { bytes -= packet.bytes(); packets--; }
                }
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            stop(new IOException("Game connection writer interrupted", error));
        } catch (IOException error) { stop(error); }
        finally {
            close();
            synchronized (this) {
                while (!pending.isEmpty()) pending.removeFirst().dispose();
            }
            releaseConnection.run();
        }
    }
}
