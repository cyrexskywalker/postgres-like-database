package optimizer.node;

import catalog.model.TableDefinition;
import index.btree.BPlusTreeIndex;

public class PhysicalIndexScanNode extends PhysicalPlanNode {

    private final TableDefinition table;
    private final String columnName;
    private final BPlusTreeIndex index;

    private final Comparable from;
    private final Comparable to;
    private final boolean includeFrom;
    private final boolean includeTo;

    public PhysicalIndexScanNode(TableDefinition table,
                                 String columnName,
                                 BPlusTreeIndex index,
                                 Comparable from,
                                 Comparable to,
                                 boolean includeFrom,
                                 boolean includeTo) {
        super("PhysicalIndexScan");
        this.table = table;
        this.columnName = columnName;
        this.index = index;
        this.from = from;
        this.to = to;
        this.includeFrom = includeFrom;
        this.includeTo = includeTo;
    }

    public TableDefinition getTable() {
        return table;
    }

    public String getColumnName() {
        return columnName;
    }

    public BPlusTreeIndex getIndex() {
        return index;
    }

    public Comparable getFrom() {
        return from;
    }

    public Comparable getTo() {
        return to;
    }

    public boolean isIncludeFrom() {
        return includeFrom;
    }

    public boolean isIncludeTo() {
        return includeTo;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "PhysicalIndexScan(" + table.getName() +
                ", col=" + columnName +
                ", from=" + from +
                ", to=" + to +
                ", incFrom=" + includeFrom +
                ", incTo=" + includeTo +
                ")\n";
    }
}