package semantic;

import catalog.model.ColumnDefinition;
import catalog.model.TableDefinition;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class QueryTree {

    public enum Kind {
        SELECT,
        CREATE_TABLE,
        CREATE,
        INSERT,
        CREATE_INDEX
    }

    public final Kind kind;

    public final List<TableDefinition> fromTables;
    public final List<QTExpr> targetList;
    public final QTExpr filter;

    public final String indexName;
    public final String indexTableName;
    public final String indexColumnName;
    public final ColumnDefinition indexColumn;

    private QueryTree(Kind kind,
                      List<TableDefinition> fromTables,
                      List<QTExpr> targetList,
                      QTExpr filter,
                      String indexName,
                      String indexTableName,
                      String indexColumnName,
                      ColumnDefinition indexColumn) {
        this.kind = kind;
        this.fromTables = fromTables;
        this.targetList = targetList;
        this.filter = filter;
        this.indexName = indexName;
        this.indexTableName = indexTableName;
        this.indexColumnName = indexColumnName;
        this.indexColumn = indexColumn;
    }

    public static QueryTree select(List<TableDefinition> fromTables, List<QTExpr> targets, QTExpr filter) {
        return new QueryTree(Kind.SELECT, fromTables, targets, filter, null, null, null, null);
    }

    public static QueryTree create(List<TableDefinition> fromTables, List<QTExpr> cols) {
        return new QueryTree(Kind.CREATE_TABLE, fromTables, cols, null, null, null, null, null);
    }

    public static QueryTree insert(List<TableDefinition> fromTables, List<QTExpr> values) {
        return new QueryTree(Kind.INSERT, fromTables, values, null, null, null, null, null);
    }

    public static QueryTree createIndex(String indexName, TableDefinition table, ColumnDefinition column) {
        return new QueryTree(
                Kind.CREATE_INDEX,
                List.of(table),
                List.of(),
                null,
                indexName,
                table.getName(),
                column.name(),
                column
        );
    }

    public sealed interface QTExpr permits QTConst, QTColumn, QTStar, QTAExpr, QTBoolExpr { }

    public static final class QTConst implements QTExpr {
        public final Object value;
        public final String type;

        public QTConst(Object value, String type) {
            this.value = value;
            this.type = type;
        }

        @Override
        public String toString() {
            return "Const(" + value + ":" + type + ")";
        }
    }

    public static final class QTColumn implements QTExpr {
        public final ColumnDefinition column;
        public final TableDefinition table;
        public final String type;

        public QTColumn(ColumnDefinition column, TableDefinition table, String type) {
            this.column = column;
            this.table = table;
            this.type = type;
        }

        @Override
        public String toString() {
            String tn = (table == null) ? "?" : table.getName();
            String cn = (column == null) ? "?" : column.name();
            String ty = (type == null) ? "UNKNOWN" : type;
            return "Column(" + tn + "." + cn + ":" + ty + ")";
        }
    }

    public static final class QTStar implements QTExpr {
        public final TableDefinition table;

        public QTStar(TableDefinition table) {
            this.table = table;
        }

        @Override
        public String toString() {
            String tn = (table == null) ? "?" : table.getName();
            return "Star(" + tn + ".*)";
        }
    }

    public static final class QTAExpr implements QTExpr {
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

    public static final class QTBoolExpr implements QTExpr {
        public final String boolop;
        public final List<QTExpr> args;

        public QTBoolExpr(String boolop, List<QTExpr> args) {
            this.boolop = boolop;
            this.args = args;
        }

        @Override
        public String toString() {
            StringJoiner sj = new StringJoiner(", ");
            if (args != null) {
                for (QTExpr a : args) sj.add(String.valueOf(a));
            }
            return "BoolExpr(" + boolop + ":[" + sj + "])";
        }
    }

    public String prettyPrint(String indent) {
        String i0 = indent == null ? "" : indent;
        String i1 = i0 + "  ";
        String i2 = i1 + "  ";

        StringBuilder sb = new StringBuilder();
        sb.append(i0).append("QueryTree").append("\n");
        sb.append(i1).append("kind: ").append(kind).append("\n");

        sb.append(i1).append("fromTables:").append("\n");
        if (fromTables == null || fromTables.isEmpty()) {
            sb.append(i2).append("(empty)").append("\n");
        } else {
            for (TableDefinition t : fromTables) {
                sb.append(i2).append(t == null ? "null" : t.getName()).append("\n");
            }
        }

        sb.append(i1).append("targetList:").append("\n");
        if (targetList == null || targetList.isEmpty()) {
            sb.append(i2).append("(empty)").append("\n");
        } else {
            for (QTExpr e : targetList) {
                appendExpr(sb, e, i2);
            }
        }

        sb.append(i1).append("filter:").append("\n");
        if (filter == null) {
            sb.append(i2).append("null").append("\n");
        } else {
            appendExpr(sb, filter, i2);
        }

        if (kind == Kind.CREATE_INDEX) {
            sb.append(i1).append("indexName: ").append(indexName).append("\n");
            sb.append(i1).append("indexTable: ").append(indexTableName).append("\n");
            sb.append(i1).append("indexColumn: ").append(indexColumnName).append("\n");
        }

        return sb.toString();
    }

    private static void appendExpr(StringBuilder sb, QTExpr expr, String indent) {
        Objects.requireNonNull(sb, "sb");
        String i0 = indent == null ? "" : indent;
        String i1 = i0 + "  ";

        if (expr == null) {
            sb.append(i0).append("null").append("\n");
            return;
        }

        if (expr instanceof QTConst c) {
            sb.append(i0).append(c).append("\n");
            return;
        }

        if (expr instanceof QTColumn c) {
            sb.append(i0).append(c).append("\n");
            return;
        }

        if (expr instanceof QTStar s) {
            sb.append(i0).append(s).append("\n");
            return;
        }

        if (expr instanceof QTAExpr a) {
            sb.append(i0).append("AExpr(").append(a.op).append(")").append("\n");
            sb.append(i1).append("left:").append("\n");
            appendExpr(sb, a.left, i1 + "  ");
            sb.append(i1).append("right:").append("\n");
            appendExpr(sb, a.right, i1 + "  ");
            return;
        }

        if (expr instanceof QTBoolExpr b) {
            sb.append(i0).append("BoolExpr(").append(b.boolop).append(")").append("\n");
            sb.append(i1).append("args:").append("\n");
            if (b.args == null || b.args.isEmpty()) {
                sb.append(i1).append("  ").append("(empty)").append("\n");
            } else {
                for (QTExpr e : b.args) {
                    appendExpr(sb, e, i1 + "  ");
                }
            }
            return;
        }

        sb.append(i0).append(String.valueOf(expr)).append("\n");
    }

    @Override
    public String toString() {
        return prettyPrint("");
    }
}