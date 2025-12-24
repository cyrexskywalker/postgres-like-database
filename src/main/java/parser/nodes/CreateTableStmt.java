package parser.nodes;

import java.util.List;

public class CreateTableStmt extends AstNode {
    public final String tableName;
    public final List<ColumnDef> columns;

    public CreateTableStmt(String tableName, List<ColumnDef> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

    @Override
    public String toString() {
        return "CreateTableStmt(table=" + tableName + ", cols=" + (columns == null ? 0 : columns.size()) + ")";
    }
}