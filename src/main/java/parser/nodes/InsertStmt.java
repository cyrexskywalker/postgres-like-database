package parser.nodes;

import java.util.List;

public class InsertStmt extends AstNode {
    public final String tableName;
    public final List<Expr> values;

    public InsertStmt(String tableName, List<Expr> values) {
        this.tableName = tableName;
        this.values = values;
    }

    @Override
    public String toString() {
        return "InsertStmt(table=" + tableName + ", values=" + (values == null ? 0 : values.size()) + ")";
    }
}