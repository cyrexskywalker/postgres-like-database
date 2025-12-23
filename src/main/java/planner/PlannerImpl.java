package planner;

import ast.QueryType;
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

        if (queryTree.commandType == null) {
            throw new IllegalArgumentException("QueryType is not set in QueryTree");
        }

        return switch (queryTree.commandType) {
            case SELECT -> planSelect(queryTree);
            case CREATE -> planCreate(queryTree);
            case INSERT -> planInsert(queryTree);
        };
    }

    // ================= SELECT =================

    private LogicalPlanNode planSelect(QueryTree q) {
        if (q.fromTables.isEmpty()) {
            throw new IllegalArgumentException("SELECT requires FROM");
        }

        if (q.fromTables.size() != 1) {
            throw new UnsupportedOperationException(
                    "Only single-table SELECT is supported"
            );
        }

        TableDefinition table = q.fromTables.get(0);

        LogicalPlanNode scan = new ScanNode(table);

        LogicalPlanNode upper = scan;
        if (q.filter != null) {
            upper = new FilterNode(upper, q.filter);
        }

        return new ProjectNode(upper, q.targetList);
    }

    // ================= CREATE TABLE =================

    private LogicalPlanNode planCreate(QueryTree q) {
        if (q.targetList.isEmpty()) {
            throw new IllegalArgumentException("CREATE TABLE requires column definitions");
        }

        // имя таблицы уже известно на этапе semantic analyzer
        // обычно semantic analyzer кладёт TableDefinition-заглушку
        TableDefinition proto = q.fromTables.get(0);

        List<ColumnDefinition> columns = new ArrayList<>();
        int position = 0;

        for (QueryTree.QTExpr expr : q.targetList) {
            if (!(expr instanceof QueryTree.QTColumn col)) {
                throw new IllegalArgumentException(
                        "CREATE TABLE expects column definitions"
                );
            }

            TypeDefinition type = catalogManager.getTypeByName(col.type);

            columns.add(new ColumnDefinition(
                    0,                  // oid будет назначен каталогом
                    0,                  // tableOid будет назначен каталогом
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

    // ================= INSERT =================

    private LogicalPlanNode planInsert(QueryTree q) {
        if (q.fromTables.size() != 1) {
            throw new IllegalArgumentException("INSERT requires exactly one target table");
        }

        TableDefinition table = q.fromTables.get(0);

        List<QueryTree.QTExpr> values = q.targetList;

        return new InsertNode(table, values);
    }
}