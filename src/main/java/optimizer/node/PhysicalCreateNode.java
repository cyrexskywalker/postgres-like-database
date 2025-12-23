package optimizer.node;


import catalog.model.TableDefinition;

/**
 * CREATE TABLE node — создание таблицы в хранилище.
 * Содержит уже готовый объект Table.
 */
public class PhysicalCreateNode extends PhysicalPlanNode {

    private final TableDefinition tableDefinition;

    public PhysicalCreateNode(TableDefinition tableDefinition) {
        super("PhysicalCreate");
        this.tableDefinition = tableDefinition;
    }

    public TableDefinition getTableDefinition() {
        return tableDefinition;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "PhysicalCreate(" + tableDefinition.getName() + ")\n";
    }
}