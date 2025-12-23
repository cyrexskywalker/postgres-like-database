package execution.executors;

import catalog.operation.OperationManager;

import java.util.Iterator;
import java.util.List;

public class SeqScanExecutor implements Executor {

    private final OperationManager op;
    private final String tableName;
    private final List<String> columns;

    private Iterator<Object> it;

    public SeqScanExecutor(OperationManager op, String tableName, List<String> columns) {
        this.op = op;
        this.tableName = tableName;
        this.columns = columns;
    }

    @Override
    public void open() {
        List<Object> rows = op.select(tableName, columns);
        this.it = rows.iterator();
    }

    @Override
    public Object next() {
        if (it == null || !it.hasNext()) return null;
        return it.next();
    }

    @Override
    public void close() { }
}