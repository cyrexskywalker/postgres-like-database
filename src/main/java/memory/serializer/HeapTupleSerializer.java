package memory.serializer;

import memory.model.DataType;
import memory.model.HeapTuple;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class HeapTupleSerializer implements TupleSerializer {
    @Override
    public <T> HeapTuple serialize(T value, DataType dataType) {
        if (value == null) {
            throw new IllegalArgumentException("null value");
        }
        if (dataType == null) {
            throw new IllegalArgumentException("null dataType");
        }

        switch (dataType) {
            case INT64 -> {
                if (!(value instanceof Long l)) {
                    throw new IllegalArgumentException("expected a INT64 value, got " + value.getClass());
                }
                byte[] buf = new byte[Long.BYTES];
                ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).putLong(l);

                return new HeapTuple(buf, DataType.INT64);
            }
            case VARCHAR ->  {
                if (!(value instanceof String s)) {
                    throw new IllegalArgumentException("expected a VARCHAR value, got " + value.getClass());
                }
                byte[] str = s.getBytes(StandardCharsets.UTF_8);
                if (str.length > 255) {
                    throw new IllegalArgumentException("VARCHAR too long: " + str.length);
                }
                byte[] buf = new byte[1 + str.length];
                buf[0] = (byte) (str.length & 0xFF);
                System.arraycopy(str, 0, buf, 1, str.length);
                return new HeapTuple(buf, DataType.VARCHAR);

            }
            default -> throw new UnsupportedOperationException("unsupported data type: " + dataType);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(HeapTuple tuple) {
        if (tuple == null) throw new IllegalArgumentException("null tuple");

        DataType type = tuple.type();
        byte[] data = tuple.data();
        switch (type) {
            case INT64 -> {
                if (data.length != Long.BYTES) {
                    throw new IllegalArgumentException("invalid INT64 length: " + data.length);
                }
                long value = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getLong();
                return (T) Long.valueOf(value);
            }
            case VARCHAR -> {
                if (data.length == 0) {
                    throw new IllegalArgumentException("invalid VARCHAR length 0");
                }
                int len = data[0] & 0xFF;
                if (data.length != 1 + len) {
                    throw new IllegalArgumentException("invalid VARCHAR payload: " + data.length);
                }
                String s = new String(data, 1, len, StandardCharsets.UTF_8);
                return (T) s;
            }
            default -> throw new UnsupportedOperationException("unsupported data type: " + type);
        }
    }
}
