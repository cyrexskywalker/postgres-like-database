package memory.buffer;

import memory.manager.PageFileManager;
import memory.model.BufferSlot;
import memory.page.Page;
import memory.replacer.Replacer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultBufferPoolManager implements BufferPoolManager {
    private final int poolSize;
    private final PageFileManager io;
    private final Replacer replacer;
    private final Map<Integer, BufferSlot> table;
    private final Path dbPath = Path.of("data", "heap_database.dat").toAbsolutePath(); //storage layer'а нет((

    public DefaultBufferPoolManager(int poolSize, PageFileManager io, Replacer replacer) {
        if (poolSize <= 0) {
            throw new IllegalArgumentException("pool size must be > 0");
        }
        if (io == null || replacer == null) {
            throw new IllegalArgumentException("memory/io/replacer/dbPath must be non-null");
        }
        this.poolSize = poolSize;
        this.io = io;
        this.replacer = replacer;
        this.table = new HashMap<>();
    }

    private void ensureFrame() {
        if (table.size() < poolSize) return;

        BufferSlot victim = replacer.pickVictim();
        if (victim == null) {
            throw new IllegalStateException("no free frame: all pinned");
        }

        if (victim.isDirty()) {
            io.write(victim.getPage(), dbPath);
            victim.setDirty(false);
        }

        replacer.delete(victim.getPageId());
        table.remove(victim.getPageId());
    }

    private void refreshIfUnpinned(BufferSlot slot) {
        if (!slot.isPinned()) {
            replacer.delete(slot.getPageId());
            replacer.push(slot);
        }
    }

    @Override
    public BufferSlot getPage(int pageId) {
        BufferSlot hit = table.get(pageId);
        if (hit != null) {
            hit.incrementUsage();
            refreshIfUnpinned(hit);
            return hit;
        }

        ensureFrame();

        Page page = io.read(pageId, dbPath);
        BufferSlot slot = new BufferSlot(pageId, page);
        slot.setPinned(false);
        slot.setDirty(false);
        slot.incrementUsage();

        table.put(pageId, slot);
        replacer.push(slot);
        return slot;
    }

    @Override
    synchronized public void updatePage(int pageId, Page page) {
        BufferSlot slot = table.get(pageId);
        if (slot == null) {
            ensureFrame();
            slot = new BufferSlot(pageId, page);
            slot.setPinned(false);
            slot.setDirty(true);
            slot.incrementUsage();

            table.put(pageId, slot);
            replacer.push(slot);
        } else {
            slot.setPage(page);
            slot.setDirty(true);
            slot.incrementUsage();
            refreshIfUnpinned(slot);
        }
    }

    @Override
    public void pinPage(int pageId) {
        BufferSlot slot = table.get(pageId);
        if (slot == null) {
            throw new IllegalArgumentException("no such page: " + pageId);
        }
        if (!slot.isPinned()) {
            slot.setPinned(true);
            replacer.delete(pageId);
        }
    }

    @Override
    public synchronized void flushPage(int pageId) {
        BufferSlot slot = table.get(pageId);
        if (slot == null) return;
        if (slot.isDirty()) {
            io.write(slot.getPage(), dbPath);
            slot.setDirty(false);
        }
    }

    @Override
    public synchronized void flushAllPages() {
        for (BufferSlot s : table.values()) {
            if (s.isDirty()) {
                io.write(s.getPage(), dbPath);
                s.setDirty(false);
            }
        }
    }


    @Override
    public List<BufferSlot> getDirtyPages() {
        return table.values().stream()
                .filter(BufferSlot::isDirty)
                .toList();
    }
}