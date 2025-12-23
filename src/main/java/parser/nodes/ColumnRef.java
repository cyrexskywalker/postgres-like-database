package parser.nodes;

public class ColumnRef extends Expr {
    public final String table;   // может быть null
    public final String column;

    public ColumnRef(String table, String column) {
        this.table = table;
        this.column = column;
    }
    public ColumnRef(String column) {
        this(null, column);
    }

    @Override
    public String toString() {
        return (table == null || table.isBlank())
                ? "ColumnRef(" + column + ")"
                : "ColumnRef(" + table + "." + column + ")";
    }
}
