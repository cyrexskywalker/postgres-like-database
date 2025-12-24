package execution.executors;

import catalog.operation.OperationManager;

import java.util.Iterator;
import java.util.List;

public final class SeqScanExecutor implements Executor {

    private final OperationManager op;
    private final String tableName;
    private final List<String> columns;

    private java.util.Iterator<Object> it;

    public SeqScanExecutor(OperationManager op, String tableName, List<String> columns) {
        this.op = op;
        this.tableName = tableName;
        this.columns = columns;
    }

    @Override
    public void open() {
        List<String> cols = normalize(columns);
        List<Object> rows = op.select(tableName, cols);
        this.it = rows.iterator();
    }

    @Override
    public Object next() {
        if (it == null || !it.hasNext()) return null;
        return it.next();
    }

    @Override
    public void close() {
        it = null;
    }

    private static List<String> normalize(List<String> cols) {
        if (cols == null || cols.isEmpty()) return null;
        if (cols.size() == 1 && "*".equals(cols.get(0))) return null;
        if (cols.stream().anyMatch("*"::equals)) return null;
        return cols;
    }
}