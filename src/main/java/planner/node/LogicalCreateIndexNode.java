package planner.node;

public class LogicalCreateIndexNode extends LogicalPlanNode {
    private final String indexName;
    private final String tableName;
    private final String columnName;

    public LogicalCreateIndexNode(String indexName, String tableName, String columnName) {
        super("CreateIndex");
        this.indexName = indexName;
        this.tableName = tableName;
        this.columnName = columnName;
    }

    public String indexName() { return indexName; }
    public String tableName() { return tableName; }
    public String columnName() { return columnName; }

    @Override
    public String prettyPrint(String indent) {
        return indent + "CreateIndex(" + indexName + " ON " + tableName + "(" + columnName + "))\n";
    }
}