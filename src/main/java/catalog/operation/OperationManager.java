package catalog.operation;

import index.TID;
import java.util.List;

public interface OperationManager {
    TID insert(String tableName, List<Object> values);
    List<Object> select(String tableName, List<String> columnNames);
    Object selectByTid(String tableName, TID tid);
    void createIndex(String indexName, String tableName, String columnName);
}