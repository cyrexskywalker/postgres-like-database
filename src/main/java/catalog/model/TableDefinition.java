package catalog.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TableDefinition {

    // ======= CATALOG FIELDS (persisted) =======

    private final int oid;
    private final String name;
    private final String type;
    private final String fileNode;
    private int pagesCount;

    // ======= RUNTIME FIELDS (NOT persisted) =======

    private final List<ColumnDefinition> columns = new ArrayList<>();

    // ======= CONSTRUCTORS =======

    public TableDefinition(int oid, String name, String type, String fileNode, int pagesCount) {
        this.oid = oid;
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.fileNode = Objects.requireNonNull(fileNode, "fileNode");
        this.pagesCount = pagesCount;
    }

    // ======= GETTERS / SETTERS =======

    public int oid() {
        return oid;
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    public String fileNode() {
        return fileNode;
    }

    public int pagesCount() {
        return pagesCount;
    }

    public void setPagesCount(int pagesCount) {
        this.pagesCount = pagesCount;
    }

    public List<ColumnDefinition> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    /**
     * Заменяем список колонок, сохраняя неизменяемый вид наружу.
     */
    public void setColumns(List<ColumnDefinition> columnDefinitions) {
        Objects.requireNonNull(columnDefinitions, "columnDefinitions");
        columns.clear();
        columns.addAll(columnDefinitions);
    }

    // ======= SERIALIZATION =======

    public byte[] toBytes() {
        byte[] n = utf8(name);
        byte[] t = utf8(type);
        byte[] fn = utf8(fileNode);

        int size = 4           // getOid
                + 2 + n.length
                + 2 + t.length
                + 2 + fn.length
                + 4;           // pagesCount

        ByteBuffer bb = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(oid);
        putStr(bb, n);
        putStr(bb, t);
        putStr(bb, fn);
        bb.putInt(pagesCount);
        return bb.array();
    }

    public static TableDefinition fromBytes(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int oid = bb.getInt();
        String name = getStr(bb);
        String type = getStr(bb);
        String fileNode = getStr(bb);
        int pagesCount = bb.getInt();
        return new TableDefinition(oid, name, type, fileNode, pagesCount);
    }

    // ======= INTERNAL UTILS =======

    private static byte[] utf8(String s) {
        if (s == null) throw new IllegalArgumentException("string is null");
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        if (b.length > 0xFFFF) throw new IllegalArgumentException("string too long");
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