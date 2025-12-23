package memory.replacer;

import memory.Pair;
import memory.model.BufferSlot;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClockReplacer implements Replacer {
    private static final int MAX_USAGE = 5;
    private final LinkedHashMap<Integer, Pair<Integer, BufferSlot>> slots = new LinkedHashMap<>();

    @Override
    public synchronized void push(BufferSlot bufferSlot) {
        if (bufferSlot.isPinned()) return;

        int id = bufferSlot.getPageId();
        Pair<Integer, BufferSlot> pair = slots.get(id);

        if (pair != null) {
            pair.first = Math.min(MAX_USAGE, pair.first + 1);
            pair.second = bufferSlot;
            pair.second.incrementUsage();
        } else {
            slots.put(id, new Pair<>(1, bufferSlot));
            bufferSlot.incrementUsage();
        }
    }

    @Override
    public synchronized void delete(int pageId) {
        slots.remove(pageId);
    }

    @Override
    public synchronized BufferSlot pickVictim() {
        if (slots.isEmpty()) return null;

        Iterator<Map.Entry<Integer, Pair<Integer, BufferSlot>>> it;

        while (true) {
            it = slots.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Pair<Integer, BufferSlot>> entry = it.next();
                Pair<Integer, BufferSlot> pair = entry.getValue();
                BufferSlot slot = pair.second;

                if (slot.isPinned()) continue;

                if (pair.first > 0) {
                    pair.first--;
                    continue;
                }

                it.remove();
                return slot;
            }

            boolean allPinned = slots.values().stream().allMatch(p -> p.second.isPinned());
            if (allPinned) return null;
        }
    }

    public synchronized void update(int slotId) {
        Pair<Integer, BufferSlot> pair = slots.get(slotId);
        if (pair == null) return;
        if (pair.second.isPinned()) return;

        pair.first = Math.min(MAX_USAGE, pair.first + 1);
        pair.second.incrementUsage();
    }
}