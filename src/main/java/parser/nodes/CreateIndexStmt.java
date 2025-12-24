package parser.nodes;

public class CreateIndexStmt extends AstNode {
    public final String indexName;
    public final String tableName;
    public final String columnName;

    public CreateIndexStmt(String indexName, String tableName, String columnName) {
        this.indexName = indexName;
        this.tableName = tableName;
        this.columnName = columnName;
    }

    @Override
    public String toString() {
        return "CreateIndexStmt(index=" + indexName + ", table=" + tableName + ", col=" + columnName + ")";
    }
}