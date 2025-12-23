package execution.executors;

import catalog.model.TableDefinition;
import catalog.operation.OperationManager;
import semantic.QueryTree;

import java.util.List;

/**
 * Исполнитель INSERT INTO table VALUES (...).
 * Работает с SEMANTIC QueryTree, а не с AST.
 */
public class InsertExecutor implements Executor {

    private final OperationManager operationManager;
    private final TableDefinition tableDefinition;
    private final List<QueryTree.QTExpr> values;

    public InsertExecutor(OperationManager operationManager,
                          TableDefinition tableDefinition,
                          List<QueryTree.QTExpr> values) {
        this.operationManager = operationManager;
        this.tableDefinition = tableDefinition;
        this.values = values;
    }

    @Override
    public void open() { }

    @Override
    public Object next() {
        List<Object> rowValues = values.stream()
                .map(this::evalValue)
                .toList();

        operationManager.insert(tableDefinition.getName(), rowValues);
        return null;
    }

    @Override
    public void close() { }

    private Object evalValue(QueryTree.QTExpr expr) {
        if (expr instanceof QueryTree.QTConst c) {
            return c.value;
        }
        throw new IllegalStateException(
                "INSERT supports only constant values, got: " + expr
        );
    }
}