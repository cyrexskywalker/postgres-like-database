package catalog.operation;

import index.TID;

import java.util.List;

public interface OperationManager {

    void insert(String tableName, List<Object> values);

    List<Object> select(String tableName, List<String> columnNames);

    Object selectByTid(String tableName, TID tid);
}
