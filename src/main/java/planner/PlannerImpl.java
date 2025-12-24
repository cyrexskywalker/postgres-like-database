package planner;

import catalog.manager.CatalogManager;
import catalog.model.ColumnDefinition;
import catalog.model.TableDefinition;
import catalog.model.TypeDefinition;
import planner.node.*;
import semantic.QueryTree;

import java.util.ArrayList;
import java.util.List;

public class PlannerImpl implements Planner {

    private final CatalogManager catalogManager;

    public PlannerImpl(CatalogManager catalogManager) {
        this.catalogManager = catalogManager;
    }

    @Override
    public LogicalPlanNode plan(QueryTree queryTree) {
        if (queryTree == null) {
            throw new IllegalArgumentException("QueryTree is null");
        }

        if (queryTree.kind == null) {
            throw new IllegalArgumentException("Kind is not set in QueryTree");
        }

        return switch (queryTree.kind) {
            case SELECT -> planSelect(queryTree);
            case CREATE_TABLE, CREATE -> planCreateTable(queryTree);
            case CREATE_INDEX -> planCreateIndex(queryTree);
            case INSERT -> planInsert(queryTree);
        };
    }

    private LogicalPlanNode planSelect(QueryTree q) {
        if (q.fromTables == null || q.fromTables.isEmpty()) {
            throw new IllegalArgumentException("SELECT requires FROM");
        }
        if (q.fromTables.size() != 1) {
            throw new UnsupportedOperationException("Only single-table SELECT is supported");
        }

        TableDefinition table = q.fromTables.get(0);

        LogicalPlanNode scan = new ScanNode(table);

        LogicalPlanNode upper = scan;
        if (q.filter != null) {
            upper = new FilterNode(upper, q.filter);
        }

        List<QueryTree.QTExpr> targets = expandStarTargets(q.targetList, table);

        return new ProjectNode(upper, targets);
    }

    private List<QueryTree.QTExpr> expandStarTargets(List<QueryTree.QTExpr> input, TableDefinition table) {
        boolean hasStar = input.stream().anyMatch(e -> e instanceof QueryTree.QTStar);
        if (!hasStar) return input;

        List<ColumnDefinition> cols = catalogManager.listColumnsSorted(table);

        List<QueryTree.QTExpr> out = new ArrayList<>();
        for (QueryTree.QTExpr e : input) {
            if (e instanceof QueryTree.QTStar) {
                for (ColumnDefinition col : cols) {
                    TypeDefinition t = catalogManager.getTypeByOid(col.typeOid());
                    String typeName = (t == null) ? "UNKNOWN" : t.name();
                    out.add(new QueryTree.QTColumn(col, table, typeName));
                }
            } else {
                out.add(e);
            }
        }
        return out;
    }

    private LogicalPlanNode planCreateTable(QueryTree q) {
        if (q.fromTables == null || q.fromTables.size() != 1) {
            throw new IllegalArgumentException("CREATE TABLE requires exactly one target table");
        }
        if (q.targetList == null || q.targetList.isEmpty()) {
            throw new IllegalArgumentException("CREATE TABLE requires column definitions");
        }

        TableDefinition proto = q.fromTables.get(0);

        List<ColumnDefinition> columns = new ArrayList<>();
        int position = 0;

        for (QueryTree.QTExpr expr : q.targetList) {
            if (!(expr instanceof QueryTree.QTColumn col)) {
                throw new IllegalArgumentException("CREATE TABLE expects column definitions");
            }

            TypeDefinition type = catalogManager.getTypeByName(col.type);
            if (type == null) {
                throw new IllegalArgumentException("Unknown type: " + col.type);
            }

            columns.add(new ColumnDefinition(
                    0,
                    0,
                    type.getOid(),
                    col.column.name(),
                    position++
            ));
        }

        TableDefinition tableDef = new TableDefinition(
                0,
                proto.getName(),
                "USER",
                proto.getName(),
                0
        );

        tableDef.setColumns(columns);

        return new CreateTableNode(tableDef);
    }

    private LogicalPlanNode planCreateIndex(QueryTree q) {
        if (q.indexName == null || q.indexName.isBlank()) {
            throw new IllegalArgumentException("CREATE INDEX: empty index name");
        }
        if (q.indexTableName == null || q.indexTableName.isBlank()) {
            throw new IllegalArgumentException("CREATE INDEX: empty table name");
        }
        if (q.indexColumnName == null || q.indexColumnName.isBlank()) {
            throw new IllegalArgumentException("CREATE INDEX: empty column name");
        }

        return new LogicalCreateIndexNode(q.indexName, q.indexTableName, q.indexColumnName);
    }

    private LogicalPlanNode planInsert(QueryTree q) {
        if (q.fromTables == null || q.fromTables.size() != 1) {
            throw new IllegalArgumentException("INSERT requires exactly one target table");
        }

        TableDefinition table = q.fromTables.get(0);

        List<QueryTree.QTExpr> values = q.targetList;
        if (values == null) values = List.of();

        return new InsertNode(table, values);
    }
}