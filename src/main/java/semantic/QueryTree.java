package semantic;

import ast.QueryType;
import catalog.model.ColumnDefinition;
import catalog.model.TableDefinition;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class QueryTree {

    public final List<TableDefinition> fromTables;
    public final List<QTExpr> targetList;
    public final QTExpr filter;
    public QueryType commandType;

    public QueryTree(List<TableDefinition> fromTables,
                     List<QTExpr> targetList,
                     QTExpr filter) {
        this.fromTables = Objects.requireNonNull(fromTables, "fromTables");
        this.targetList = Objects.requireNonNull(targetList, "targetList");
        this.filter = filter;
    }

    public interface QTExpr { }

    public static class QTColumn implements QTExpr {
        public final ColumnDefinition column;
        public final TableDefinition table;
        public final String type;

        public QTColumn(ColumnDefinition column, TableDefinition table) {
            this.column = column;
            this.table = table;
            this.type = "UNKNOWN";
        }

        public QTColumn(ColumnDefinition column, TableDefinition table, String typeName) {
            this.column = column;
            this.table = table;
            this.type = typeName != null ? typeName : "UNKNOWN";
        }

        public TableDefinition table() { return table; }

        @Override
        public String toString() {
            return "Column(" + table.getName() + "." + column.name() + ":" + type + ")";
        }
    }

    public static class QTConst implements QTExpr {
        public final Object value;
        public final String type;

        public QTConst(Object value, String type) {
            this.value = value;
            this.type = type != null ? type : "UNKNOWN";
        }

        @Override
        public String toString() {
            String v = (value instanceof String) ? "'" + value + "'" : String.valueOf(value);
            return "Const(" + v + ":" + type + ")";
        }
    }

    public static class QTAExpr implements QTExpr {
        public final String op;
        public final QTExpr left;
        public final QTExpr right;

        public QTAExpr(String op, QTExpr left, QTExpr right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Override
        public String toString() {
            return "AExpr(" + op + ", " + left + ", " + right + ")";
        }
    }

    public static class QTBoolExpr implements QTExpr {
        public final String op;
        public final List<QTExpr> args;

        public QTBoolExpr(String op, List<QTExpr> args) {
            this.op = op;
            this.args = args;
        }

        @Override
        public String toString() {
            StringJoiner sj = new StringJoiner(", ", "BoolExpr(" + op + ":[", "])");
            for (QTExpr e : args) sj.add(String.valueOf(e));
            return sj.toString();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("QueryTree\n");
        sb.append("  fromTables:\n");
        for (TableDefinition t : fromTables) {
            sb.append("    ").append(t.getName()).append("\n");
        }
        sb.append("  targetList:\n");
        for (QTExpr e : targetList) {
            sb.append("    ").append(e).append("\n");
        }
        sb.append("  filter:\n");
        if (filter == null) {
            sb.append("    null\n");
        } else {
            sb.append("    ").append(filter).append("\n");
        }
        return sb.toString();
    }
}