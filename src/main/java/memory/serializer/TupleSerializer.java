package memory.serializer;

import memory.model.DataType;
import memory.model.HeapTuple;

public interface TupleSerializer {
    <T> HeapTuple serialize(T value, DataType dataType);

    <T> T deserialize(HeapTuple tuple);
}
