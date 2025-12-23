package planner.node;

import ast.Expr;
import catalog.model.TableDefinition;

import java.util.List;

/**
 * Логический узел INSERT INTO table VALUES (...).
 * Хранит готовый объект Table и список значений.
 */
public class InsertNode extends LogicalPlanNode {

    private final TableDefinition tableDefinition;
    private final List<Expr> values;  // Expr вместо Object

    public InsertNode(TableDefinition tableDefinition, List<Expr> values) {
        super("Insert");
        this.tableDefinition = tableDefinition;
        this.values = values;
        this.outputColumns = List.of(); // INSERT не возвращает строки
    }

    public TableDefinition getTableDefinition() {
        return tableDefinition;
    }

    public List<Expr> getValues() {
        return values;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "Insert(" + tableDefinition.getName() + ", values=" + values + ")\n";
    }
}