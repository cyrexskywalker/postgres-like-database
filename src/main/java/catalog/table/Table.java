package catalog.table;

import catalog.operation.OperationManager;
import index.TID;

import java.util.Objects;

/**
 * Runtime-представление таблицы.
 * Используется executors (SeqScan / IndexScan).
 */
public class Table {

    private final String tableName;
    private final OperationManager operationManager;

    public Table(String tableName, OperationManager operationManager) {
        this.tableName = Objects.requireNonNull(tableName, "tableName");
        this.operationManager = Objects.requireNonNull(operationManager, "operationManager");
    }

    /**
     * Чтение строки по физическому адресу (TID).
     */
    public Object read(TID tid) {
        return operationManager.selectByTid(tableName, tid);
    }

    public String getTableName() {
        return tableName;
    }
}