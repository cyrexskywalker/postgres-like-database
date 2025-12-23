package catalog.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ColumnDefinition {

    private final int oid;
    private final int tableOid;
    private final int typeOid;
    private final String name;
    private final int position;

    // Используется при загрузке из каталога
    public ColumnDefinition(int oid, int tableOid, int typeOid, String name, int position) {
        this.oid = oid;
        this.tableOid = tableOid;
        this.typeOid = typeOid;
        this.name = Objects.requireNonNull(name, "name");
        this.position = position;
    }

    // Используется при CREATE TABLE (без getOid и tableOid)
    public ColumnDefinition(int typeOid, String name, int position) {
        this.oid = 0;
        this.tableOid = 0;
        this.typeOid = typeOid;
        this.name = Objects.requireNonNull(name, "name");
        this.position = position;
    }

    // ======= GETTERS =======

    public int oid() {
        return oid;
    }

    public int tableOid() {
        return tableOid;
    }

    public int typeOid() {
        return typeOid;
    }

    public String name() {
        return name;
    }

    public int position() {
        return position;
    }

    // ======= SERIALIZATION =======

    public byte[] toBytes() {
        byte[] n = utf8(name);

        int size = 4           // getOid
                + 4            // tableOid
                + 4            // typeOid
                + 2 + n.length // name
                + 4;           // position

        ByteBuffer bb = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(oid);
        bb.putInt(tableOid);
        bb.putInt(typeOid);
        putStr(bb, n);
        bb.putInt(position);
        return bb.array();
    }

    public static ColumnDefinition fromBytes(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int oid = bb.getInt();
        int tableOid = bb.getInt();
        int typeOid = bb.getInt();
        String name = getStr(bb);
        int position = bb.getInt();
        return new ColumnDefinition(oid, tableOid, typeOid, name, position);
    }

    // ======= INTERNAL UTILS =======

    private static byte[] utf8(String s) {
        if (s == null) throw new IllegalArgumentException("string is null");
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        if (b.length > 0xFFFF) {
            throw new IllegalArgumentException("string too long: " + b.length);
        }
        return b;
    }

    private static void putStr(ByteBuffer bb, byte[] b) {
        bb.putShort((short) (b.length & 0xFFFF));
        bb.put(b);
    }

    private static String getStr(ByteBuffer bb) {
        int len = bb.getShort() & 0xFFFF;
        byte[] b = new byte[len];
        bb.get(b);
        return new String(b, StandardCharsets.UTF_8);
    }
}