package planner.node;


import catalog.model.TableDefinition;

import java.util.List;

/**
 * Логический узел CREATE TABLE.
 * Хранит готовый объект Table.
 */
public class CreateTableNode extends LogicalPlanNode {

    private final TableDefinition tableDefinition;

    public CreateTableNode(TableDefinition tableDefinition) {
        super("CreateTable");
        this.tableDefinition = tableDefinition;
        this.outputColumns = List.of(); // CREATE не возвращает данных
    }

    public TableDefinition getTableDefinition() {
        return tableDefinition;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "CreateTable(" + tableDefinition.getName() + ")\n";
    }
}