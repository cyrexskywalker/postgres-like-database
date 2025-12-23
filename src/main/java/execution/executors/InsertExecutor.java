package execution.executors;


import ast.AConst;
import ast.Expr;
import catalog.model.TableDefinition;
import catalog.operation.OperationManager;

import java.util.List;

/**
 * Исполнитель для операции INSERT INTO table VALUES (...).
 * Получает объект Table и конкретные значения.
 */
public class InsertExecutor implements Executor {

    private final OperationManager operationManager;
    private final TableDefinition tableDefinition;
    private final List<Expr> values;

    public InsertExecutor(OperationManager operationManager, TableDefinition tableDefinition, List<Expr> values) {
        this.operationManager = operationManager;
        this.tableDefinition = tableDefinition;
        this.values = values;
    }

    @Override
    public void open() { }

    @Override
    public Object next() {
        // Преобразуем Expr в Object (предполагаем AConst)
        List<Object> rowValues = values.stream()
                .map(expr -> ((AConst) expr).value)
                .toList();

        // Используем новый метод insert(OperationManager)
        operationManager.insert(tableDefinition.getName(), rowValues);

        return null;
    }

    @Override
    public void close() { }
}