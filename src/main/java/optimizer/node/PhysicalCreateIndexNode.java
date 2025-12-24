package optimizer.node;

public class PhysicalCreateIndexNode extends PhysicalPlanNode {
    private final String indexName;
    private final String tableName;
    private final String columnName;

    public PhysicalCreateIndexNode(String indexName, String tableName, String columnName) {
        super("PhysicalCreateIndex");
        this.indexName = indexName;
        this.tableName = tableName;
        this.columnName = columnName;
    }

    public String indexName() { return indexName; }
    public String tableName() { return tableName; }
    public String columnName() { return columnName; }

    @Override
    public String prettyPrint(String indent) {
        return indent + "PhysicalCreateIndex(" + indexName + " ON " + tableName + "(" + columnName + "))\n";
    }
}