package planner;

import ast.Expr;
import ast.QueryTree;
import ast.RangeTblEntry;
import ast.TargetEntry;
import catalog.manager.CatalogManager;
import catalog.model.ColumnDefinition;
import catalog.model.TableDefinition;
import catalog.model.TypeDefinition;
import planner.node.*;

import java.util.ArrayList;
import java.util.List;

public class PlannerImpl implements Planner {

    private final CatalogManager catalogManager;

    public PlannerImpl(CatalogManager catalogManager) {
        this.catalogManager = catalogManager;
    }

    @Override
    public LogicalPlanNode plan(QueryTree queryTree) {
        if (queryTree == null) throw new IllegalArgumentException("QueryTree is null");

        return switch (queryTree.commandType) {
            case CREATE -> planCreate(queryTree);
            case INSERT -> planInsert(queryTree);
            case SELECT -> planSelect(queryTree);
        };
    }

    private LogicalPlanNode planSelect(QueryTree q) {
        if (q.rangeTable == null || q.rangeTable.isEmpty())
            throw new IllegalArgumentException("SELECT requires at least one table in FROM");

        if (q.rangeTable.size() != 1)
            throw new UnsupportedOperationException("Only single-table SELECT is supported in this planner");

        RangeTblEntry rte = q.rangeTable.get(0);

        LogicalPlanNode scan = new ScanNode(rte);

        LogicalPlanNode upper = scan;
        if (q.whereClause != null) {
            upper = new FilterNode(upper, q.whereClause);
        }

        return new ProjectNode(upper, q.targetList);
    }

    private LogicalPlanNode planCreate(QueryTree q) {
        String tableName = extractTableName(q);

        List<ColumnDefinition> columns = new ArrayList<>();
        int position = 0;
        for (TargetEntry te : q.targetList) {
            TypeDefinition type = catalogManager.getTypeByName(te.resultType);
            columns.add(new ColumnDefinition(
                    type.getOid(),
                    te.alias,
                    position++
            ));
        }

        TableDefinition tableDef = new TableDefinition(0, tableName, "USER", tableName, 0);
        tableDef.setColumns(columns);

        return new CreateTableNode(tableDef);
    }

    private LogicalPlanNode planInsert(QueryTree q) {
        String tableName = extractTableName(q);
        TableDefinition tableDef = catalogManager.getTable(tableName);

        List<Expr> values = q.targetList.stream()
                .map(te -> te.expr)
                .toList();

        return new InsertNode(tableDef, values);
    }

    private String extractTableName(QueryTree q) {
        if (q.rangeTable != null && !q.rangeTable.isEmpty() && q.rangeTable.get(0).tableName != null) {
            return q.rangeTable.get(0).tableName;
        }
        throw new IllegalArgumentException("Cannot determine table name");
    }
}