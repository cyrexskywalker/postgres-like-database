package optimizer.node;

import catalog.model.TableDefinition;
import semantic.QueryTree;

import java.util.List;

/**
 * Физический узел INSERT INTO table VALUES (...).
 * Хранит готовый объект Table и список значений.
 */
public class PhysicalInsertNode extends PhysicalPlanNode {

    private final TableDefinition tableDefinition;
    private final List<QueryTree.QTExpr> values;

    public PhysicalInsertNode(TableDefinition tableDefinition, List<QueryTree.QTExpr> values) {
        super("PhysicalInsert");
        this.tableDefinition = tableDefinition;
        this.values = values;
    }

    public TableDefinition getTableDefinition() {
        return tableDefinition;
    }

    public List<QueryTree.QTExpr> getValues() {
        return values;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "PhysicalInsert(" + tableDefinition.getName() + ")\n";
    }
}