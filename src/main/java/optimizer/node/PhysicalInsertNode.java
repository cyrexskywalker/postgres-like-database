package optimizer.node;


import ast.Expr;
import catalog.model.TableDefinition;

import java.util.List;

/**
 * Физический узел INSERT INTO table VALUES (...).
 * Хранит готовый объект Table и список значений.
 */
public class PhysicalInsertNode extends PhysicalPlanNode {

    private final TableDefinition tableDefinition;
    private final List<Expr> values;

    public PhysicalInsertNode(TableDefinition tableDefinition, List<Expr> values) {
        super("PhysicalInsert");
        this.tableDefinition = tableDefinition;
        this.values = values;
    }

    public TableDefinition getTableDefinition() {
        return tableDefinition;
    }

    public List<Expr> getValues() {
        return values;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "PhysicalInsert(" + tableDefinition.getName() + ")\n";
    }
}