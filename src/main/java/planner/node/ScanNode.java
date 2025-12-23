package planner.node;

import catalog.model.TableDefinition;

import java.util.List;

public class ScanNode extends LogicalPlanNode {

    private final TableDefinition table;

    public ScanNode(TableDefinition table) {
        super("SeqScan");
        this.table = table;
        this.outputColumns = List.of("*");
    }

    public TableDefinition getTable() {
        return table;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "SeqScan(" + table.getName() + ")\n";
    }
}