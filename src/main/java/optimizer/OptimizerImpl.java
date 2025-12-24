package optimizer;

import catalog.manager.CatalogManager;
import catalog.model.TableDefinition;
import index.btree.BPlusTreeIndex;
import index.registry.IndexRegistry;
import optimizer.node.*;
import planner.node.*;
import semantic.QueryTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class OptimizerImpl implements Optimizer {

    private static final double SEQ_PAGE_COST = 1.0;
    private static final double RANDOM_PAGE_COST = 4.0;
    private static final double CPU_TUPLE_COST = 0.01;
    private static final double CPU_OPERATOR_COST = 0.0025;

    private static final double DEFAULT_RANGE_SELECTIVITY = 0.3;
    private static final int DEFAULT_TUPLES_PER_PAGE = 100;

    private final CatalogManager catalog;
    private final IndexRegistry indexRegistry;

    public OptimizerImpl(CatalogManager catalog, IndexRegistry indexRegistry) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.indexRegistry = indexRegistry;
    }

    @Override
    public PhysicalPlanNode optimize(LogicalPlanNode logicalPlan) {
        Objects.requireNonNull(logicalPlan, "logicalPlan");

        if (logicalPlan instanceof CreateTableNode ln) {
            return new PhysicalCreateNode(ln.getTableDefinition());
        }

        if (logicalPlan instanceof InsertNode ln) {
            return new PhysicalInsertNode(ln.getTableDefinition(), ln.getValues());
        }

        if (logicalPlan instanceof LogicalCreateIndexNode ci) {
            return new PhysicalCreateIndexNode(ci.indexName(), ci.tableName(), ci.columnName());
        }

        if (logicalPlan instanceof ScanNode ln) {
            return new PhysicalSeqScanNode(ln.getTable());
        }

        if (logicalPlan instanceof FilterNode ln) {
            return optimizeFilter(ln);
        }

        if (logicalPlan instanceof ProjectNode p) {
            PhysicalPlanNode child = optimize(p.getInput());

            var targets = p.getTargets();
            if (targets.size() == 1 && targets.get(0) instanceof QueryTree.QTStar) {
                return child;
            }

            return new PhysicalProjectNode(child, targets);
        }

        throw new UnsupportedOperationException(
                "Unsupported logical node type: " + logicalPlan.getClass().getSimpleName()
        );
    }

    private PhysicalPlanNode optimizeFilter(FilterNode ln) {
        LogicalPlanNode in = ln.getInput();

        if (!(in instanceof ScanNode sn)) {
            PhysicalPlanNode child = optimize(in);
            return new PhysicalFilterNode(child, ln.getPredicate());
        }

        // важно: берём "свежую" TableDefinition из каталога, чтобы pagesCount не залипал
        TableDefinition table = catalog.getTable(sn.getTable().getName());
        QueryTree.QTExpr predicate = ln.getPredicate();

        IndexChoice choice = chooseIndexPath(table, predicate);
        if (choice == null) {
            PhysicalPlanNode scan = new PhysicalSeqScanNode(table);
            return new PhysicalFilterNode(scan, predicate);
        }

        PhysicalPlanNode scan = new PhysicalIndexScanNode(
                table,
                choice.columnName,
                choice.index,
                choice.from,
                choice.to,
                choice.includeFrom,
                choice.includeTo
        );

        if (choice.residual != null) {
            scan = new PhysicalFilterNode(scan, choice.residual);
        }

        double seqCost = estimateSeqScanCost(table);
        double idxCost = estimateIndexScanCost(table, choice.index, choice.estimatedSelectivity);

        if (idxCost <= seqCost) {
            return scan;
        }

        System.out.println("COST seq=" + seqCost + " idx=" + idxCost + " sel=" + choice.estimatedSelectivity);

        PhysicalPlanNode fallback = new PhysicalSeqScanNode(table);
        return new PhysicalFilterNode(fallback, predicate);
    }

    private int estimateRows(TableDefinition table) {
        int pages = Math.max(1, table.pagesCount());
        return pages * DEFAULT_TUPLES_PER_PAGE;
    }

    private double estimateSeqScanCost(TableDefinition table) {
        int pages = Math.max(1, table.pagesCount());
        int rows = estimateRows(table);
        return pages * SEQ_PAGE_COST + rows * CPU_TUPLE_COST;
    }

    private double estimateIndexScanCost(TableDefinition table, BPlusTreeIndex index, double selectivity) {
        int rows = estimateRows(table);

        double traversal = Math.max(1, index.getHeight()) * CPU_OPERATOR_COST;
        double fetch = (rows * clamp01(selectivity)) * (RANDOM_PAGE_COST + CPU_TUPLE_COST);
        return traversal + fetch;
    }

    private static double clamp01(double x) {
        if (x < 0) return 0;
        if (x > 1) return 1;
        return x;
    }

    private IndexChoice chooseIndexPath(TableDefinition table, QueryTree.QTExpr predicate) {
        if (indexRegistry == null) return null;

        List<QueryTree.QTExpr> conjuncts = splitAnd(predicate);
        IndexChoice best = null;

        for (QueryTree.QTExpr c : conjuncts) {
            IndexChoice choice = trySargable(table, c);
            if (choice == null) continue;

            choice.residual = (conjuncts.size() == 1) ? null : predicate;

            if (best == null || choice.estimatedSelectivity < best.estimatedSelectivity) {
                best = choice;
            }
        }

        return best;
    }

    private IndexChoice trySargable(TableDefinition table, QueryTree.QTExpr expr) {
        if (!(expr instanceof QueryTree.QTAExpr a)) return null;

        String op = a.op.toUpperCase(Locale.ROOT);

        Side s = extractColumnConst(a.left, a.right);
        if (s == null) s = extractColumnConst(a.right, a.left);
        if (s == null) return null;

        String columnName = s.column.column.name();
        BPlusTreeIndex index = indexRegistry.get(table.getName(), columnName);
        if (index == null) return null;

        Object v = s.constant.value;
        if (!(v instanceof Comparable<?> cmp)) return null;

        Comparable<?> from;
        Comparable<?> to;
        boolean includeFrom;
        boolean includeTo;
        double sel;

        if ("=".equals(op)) {
            from = cmp;
            to = cmp;
            includeFrom = true;
            includeTo = true;

            int rows = estimateRows(table);
            sel = 1.0 / rows; // Вариант B: "одна строка из N"

        } else if (">".equals(op)) {
            from = cmp;
            to = null;
            includeFrom = false;
            includeTo = true;
            sel = DEFAULT_RANGE_SELECTIVITY;

        } else if (">=".equals(op)) {
            from = cmp;
            to = null;
            includeFrom = true;
            includeTo = true;
            sel = DEFAULT_RANGE_SELECTIVITY;

        } else if ("<".equals(op)) {
            from = null;
            to = cmp;
            includeFrom = true;
            includeTo = false;
            sel = DEFAULT_RANGE_SELECTIVITY;

        } else if ("<=".equals(op)) {
            from = null;
            to = cmp;
            includeFrom = true;
            includeTo = true;
            sel = DEFAULT_RANGE_SELECTIVITY;

        } else {
            return null;
        }

        IndexChoice out = new IndexChoice();
        out.table = table;
        out.columnName = columnName;
        out.index = index;
        out.from = from;
        out.to = to;
        out.includeFrom = includeFrom;
        out.includeTo = includeTo;
        out.estimatedSelectivity = sel;
        return out;
    }

    private static Side extractColumnConst(QueryTree.QTExpr left, QueryTree.QTExpr right) {
        if (left instanceof QueryTree.QTColumn col && right instanceof QueryTree.QTConst c) {
            Side s = new Side();
            s.column = col;
            s.constant = c;
            return s;
        }
        return null;
    }

    private static List<QueryTree.QTExpr> splitAnd(QueryTree.QTExpr e) {
        List<QueryTree.QTExpr> out = new ArrayList<>();
        collectAnd(e, out);
        return out;
    }

    private static void collectAnd(QueryTree.QTExpr e, List<QueryTree.QTExpr> out) {
        if (e instanceof QueryTree.QTBoolExpr b && "AND".equalsIgnoreCase(b.boolop)) {
            for (QueryTree.QTExpr a : b.args) collectAnd(a, out);
        } else {
            out.add(e);
        }
    }

    private static final class Side {
        QueryTree.QTColumn column;
        QueryTree.QTConst constant;
    }

    private static final class IndexChoice {
        TableDefinition table;
        String columnName;
        BPlusTreeIndex index;

        Comparable<?> from;
        Comparable<?> to;
        boolean includeFrom;
        boolean includeTo;

        double estimatedSelectivity;
        QueryTree.QTExpr residual;
    }
}