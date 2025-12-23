package memory.page;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class HeapPage implements Page {
    private static final int PAGE_SIZE   = 8192;
    private static final int HEADER_SIZE = 10;
    private static final int SLOT_SIZE   = 4;
    private static final int MAGIC       = 0x00DBDB01;

    private static final int MAGIC_OFF     = 0; // int
    private static final int SLOTCOUNT_OFF = 4; // short
    private static final int LOWER_OFF     = 6; // short
    private static final int UPPER_OFF     = 8; // short

    private final int pageId;
    private final byte[] buffer;
    private final ByteBuffer byteBuffer;

    public HeapPage(int pageId) {
        this.pageId = pageId;
        this.buffer = new byte[PAGE_SIZE];
        this.byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);

        byteBuffer.putInt(MAGIC_OFF, MAGIC);
        byteBuffer.putShort(SLOTCOUNT_OFF, (short) 0);
        byteBuffer.putShort(LOWER_OFF, (short) HEADER_SIZE);
        byteBuffer.putShort(UPPER_OFF, (short) PAGE_SIZE);
    }

    public HeapPage(int pageId, byte[] bytes) {
        if (bytes == null || bytes.length != PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid page size");
        }
        this.pageId = pageId;
        this.buffer = Arrays.copyOf(bytes, PAGE_SIZE);
        this.byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);

        if (!isValid()) {
            throw new IllegalStateException("invalid magic");
        }
        short slotCount = sc();
        short lower = lo();
        short upper = up();

        if (lower < HEADER_SIZE || lower > upper || upper > PAGE_SIZE) {
            throw new IllegalStateException("invalid bounds");
        }
        if (lower != HEADER_SIZE + slotCount * SLOT_SIZE) {
            throw new IllegalStateException("slot directory inconsistent");
        }
    }

    private short sc() { return byteBuffer.getShort(SLOTCOUNT_OFF); }
    private short lo() { return byteBuffer.getShort(LOWER_OFF); }
    private short up() { return byteBuffer.getShort(UPPER_OFF); }

    private int slotPos(int i) { return HEADER_SIZE + i * SLOT_SIZE; }
    private short slotOff(int i) { return byteBuffer.getShort(slotPos(i)); }
    private short slotLen(int i) { return byteBuffer.getShort(slotPos(i) + 2); }
    private void writeSlot(int i, short off, short len) {
        byteBuffer.putShort(slotPos(i), off);
        byteBuffer.putShort(slotPos(i) + 2, len);
    }

    @Override
    public byte[] bytes() {
        return Arrays.copyOf(buffer, PAGE_SIZE);
    }

    @Override
    public int getPageId() {
        return this.pageId;
    }

    @Override
    public int size() {
        return sc() & 0xFFFF;
    }

    @Override
    public boolean isValid() {
        return byteBuffer.getInt(MAGIC_OFF) == MAGIC;
    }

    @Override
    public byte[] read(int index) {
        int count = sc() & 0xFFFF;
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException("bad slot index");
        }

        int off = slotOff(index) & 0xFFFF;
        int len = slotLen(index) & 0xFFFF;

        if (off + len > PAGE_SIZE) {
            throw new IllegalArgumentException("slot range out of page");
        }

        byte[] out = new byte[len];
        System.arraycopy(buffer, off, out, 0, len);
        return out;
    }

    @Override
    public void write(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("invalid data");
        }

        int lower = lo() & 0xFFFF;
        int upper = up() & 0xFFFF;
        int need  = SLOT_SIZE + data.length;
        int free  = upper - lower;

        if (free < need) {
            throw new IllegalArgumentException("not enough space");
        }

        int newUpper = upper - data.length;
        System.arraycopy(data, 0, buffer, newUpper, data.length);

        int idx = sc() & 0xFFFF;
        writeSlot(idx, (short) newUpper, (short) data.length);

        byteBuffer.putShort(SLOTCOUNT_OFF, (short) (idx + 1));
        byteBuffer.putShort(LOWER_OFF, (short) (lower + SLOT_SIZE));
        byteBuffer.putShort(UPPER_OFF, (short) newUpper);
    }
}
