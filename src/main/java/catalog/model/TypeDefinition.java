package catalog.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public record TypeDefinition(int oid, String name, int byteLength) {

    public byte[] toBytes() {
        byte[] n = utf8(name);
        int size = 4           // oid
                + 2 + n.length // name
                + 4;           // byteLength

        ByteBuffer bb = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(oid);
        putStr(bb, n);
        bb.putInt(byteLength);
        return bb.array();
    }

    public static TypeDefinition fromBytes(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int oid = bb.getInt();
        String name = getStr(bb);
        int byteLength = bb.getInt();
        return new TypeDefinition(oid, name, byteLength);
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