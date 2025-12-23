package memory.io;

import memory.buffer.BufferPoolManager;
import memory.model.BufferSlot;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultDirtyPageWriter implements DirtyPageWriter {
    private final BufferPoolManager bpm;
    private final long bgIntervalMs;
    private final int batchSize;
    private final long checkpointIntervalMs;
    private final ScheduledExecutorService scheduler;

    private final AtomicBoolean bgRunning = new AtomicBoolean(false);
    private final AtomicBoolean cpRunning = new AtomicBoolean(false);

    private ScheduledFuture<?> bgTask;
    private ScheduledFuture<?> cpTask;

    public DefaultDirtyPageWriter(BufferPoolManager bpm,
                                  long bgIntervalMs,
                                  int batchSize,
                                  long checkpointIntervalMs,
                                  ScheduledExecutorService scheduler) {
        this.bpm = Objects.requireNonNull(bpm, "bpm");
        if (bgIntervalMs <= 0) throw new IllegalArgumentException("bgIntervalMs must be > 0");
        if (batchSize <= 0) throw new IllegalArgumentException("batchSize must be > 0");
        if (checkpointIntervalMs <= 0) throw new IllegalArgumentException("checkpointIntervalMs must be > 0");
        this.bgIntervalMs = bgIntervalMs;
        this.batchSize = batchSize;
        this.checkpointIntervalMs = checkpointIntervalMs;
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    @Override
    public synchronized void startBackgroundWriter() {
        if (bgRunning.compareAndSet(false, true)) {
            bgTask = scheduler.scheduleAtFixedRate(
                    this::runBackgroundWriterSafe,
                    bgIntervalMs,
                    bgIntervalMs,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    @Override
    public synchronized void startCheckPointer() {
        if (cpRunning.compareAndSet(false, true)) {
            cpTask = scheduler.scheduleAtFixedRate(
                    this::runCheckPointerSafe,
                    checkpointIntervalMs,
                    checkpointIntervalMs,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    void runBackgroundWriterSafe() {
        try {
            if (!bgRunning.get()) return;
            List<BufferSlot> dirty = bpm.getDirtyPages();
            int flushed = 0;
            for (BufferSlot s : dirty) {
                if (flushed >= batchSize) break;
                bpm.flushPage(s.getPageId());
                flushed++;
            }
        } catch (Throwable t) {
            System.err.println("[DirtyPageWriter] background writer failed: " + t);
        }
    }

    void runCheckPointerSafe() {
        try {
            if (!cpRunning.get()) return;
            bpm.flushAllPages();
        } catch (Throwable t) {
            System.err.println("[DirtyPageWriter] checkpointer failed: " + t);
        }
    }
}