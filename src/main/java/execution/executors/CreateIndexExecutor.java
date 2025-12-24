package execution.executors;

import catalog.operation.OperationManager;

public final class CreateIndexExecutor implements Executor {

    private final OperationManager op;
    private final String indexName;
    private final String tableName;
    private final String columnName;

    private boolean done;

    public CreateIndexExecutor(OperationManager op, String indexName, String tableName, String columnName) {
        this.op = op;
        this.indexName = indexName;
        this.tableName = tableName;
        this.columnName = columnName;
    }

    @Override
    public void open() {
        done = false;
    }

    @Override
    public Object next() {
        if (done) return null;
        done = true;

        op.createIndex(indexName, tableName, columnName);
        return null;
    }

    @Override
    public void close() {
    }
}