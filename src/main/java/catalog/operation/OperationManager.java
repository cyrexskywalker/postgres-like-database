package catalog.operation;

import java.util.List;

public interface OperationManager {

    void insert(String tableName, List<Object> values);

    List<Object> select(String tableName, List<String> columnNames);

}
