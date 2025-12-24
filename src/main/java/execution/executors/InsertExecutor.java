package execution.executors;

import catalog.model.TableDefinition;
import catalog.operation.OperationManager;
import semantic.QueryTree;

import java.util.List;

public class InsertExecutor implements Executor {

    private final OperationManager operationManager;
    private final TableDefinition tableDefinition;
    private final List<QueryTree.QTExpr> values;

    private boolean done;

    public InsertExecutor(OperationManager operationManager,
                          TableDefinition tableDefinition,
                          List<QueryTree.QTExpr> values) {
        this.operationManager = operationManager;
        this.tableDefinition = tableDefinition;
        this.values = values;
    }

    @Override
    public void open() {
        done = false;
    }

    @Override
    public Object next() {
        if (done) return null;
        done = true;

        List<Object> rowValues = values.stream().map(this::evalValue).toList();
        return operationManager.insert(tableDefinition.getName(), rowValues);
    }

    @Override
    public void close() { }

    private Object evalValue(QueryTree.QTExpr expr) {
        if (expr instanceof QueryTree.QTConst c) return c.value;
        throw new IllegalStateException("INSERT supports only constants, got: " + expr);
    }
}