package catalog.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public record TableDefinition(int oid, String name, String type, String fileNode, int pagesCount) {

    public byte[] toBytes() {
        byte[] n = utf8(name);
        byte[] t = utf8(type);
        byte[] fn = utf8(fileNode);

        int size = 4           // oid
                + 2 + n.length // name
                + 2 + t.length // type
                + 2 + fn.length// fileNode
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

    private static byte[] utf8(String s) {
        if (s == null) throw new IllegalArgumentException("string is null");
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        if (b.length > 0xFFFF) throw new IllegalArgumentException("string too long: " + b.length);
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