package optimizer.node;

import catalog.model.TableDefinition;

import java.util.List;

public class PhysicalSeqScanNode extends PhysicalPlanNode {

    private final TableDefinition table;
    private final List<String> columns;

    public PhysicalSeqScanNode(TableDefinition table) {
        this(table, List.of("*"));
    }

    public PhysicalSeqScanNode(TableDefinition table, List<String> columns) {
        super("PhysicalSeqScan");
        this.table = table;
        this.columns = columns;
    }

    public TableDefinition getTable() {
        return table;
    }

    public List<String> getColumns() {
        return columns;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "PhysicalSeqScan(" + table.getName() + ", cols=" + columns + ")\n";
    }
}