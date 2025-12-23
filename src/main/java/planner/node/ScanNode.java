package planner.node;

import ast.RangeTblEntry;

import java.util.List;

public class ScanNode extends LogicalPlanNode {
    private final RangeTblEntry table;

    public ScanNode(RangeTblEntry table) {
        super("Scan");
        this.table = table;
        this.outputColumns = List.of("*");
    }

    public RangeTblEntry getTable() {
        return table;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "Scan(" + table + ")\n";
    }
}