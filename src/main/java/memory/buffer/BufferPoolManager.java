package memory.buffer;

import memory.model.BufferSlot;
import memory.page.Page;

import java.util.List;

public interface BufferPoolManager {
    BufferSlot getPage(int pageId);
    void updatePage(int pageId, Page page);
    void pinPage(int pageId);
    void flushPage(int pageId);
    void flushAllPages();
    List<BufferSlot> getDirtyPages();
}
