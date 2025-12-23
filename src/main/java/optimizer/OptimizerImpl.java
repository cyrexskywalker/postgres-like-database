package optimizer;

import ast.RangeTblEntry;
import ast.TargetEntry;
import catalog.manager.CatalogManager;
import catalog.model.TableDefinition;
import optimizer.node.*;
import planner.node.*;

import java.util.List;

public class OptimizerImpl implements Optimizer {

    private final CatalogManager catalog;

    public OptimizerImpl(CatalogManager catalog) {
        this.catalog = catalog;
    }

    @Override
    public PhysicalPlanNode optimize(LogicalPlanNode logicalPlan) {
        if (logicalPlan instanceof CreateTableNode ln) {
            return new PhysicalCreateNode(ln.getTableDefinition());
        }

        if (logicalPlan instanceof InsertNode ln) {
            return new PhysicalInsertNode(ln.getTableDefinition(), ln.getValues());
        }

        if (logicalPlan instanceof ScanNode ln) {
            RangeTblEntry rte = ln.getTable();
            TableDefinition table = catalog.getTable(rte.tableName);
            return new PhysicalSeqScanNode(table);
        }

        if (logicalPlan instanceof FilterNode ln) {
            PhysicalPlanNode child = optimize(ln.getInput());
            return new PhysicalFilterNode(child, ln.getPredicate());
        }

        if (logicalPlan instanceof ProjectNode ln) {
            PhysicalPlanNode child = optimize(ln.getInput());
            List<TargetEntry> targets = ln.getTargets();
            return new PhysicalProjectNode(child, targets);
        }

        throw new UnsupportedOperationException(
                "Unsupported logical node type: " + logicalPlan.getClass().getSimpleName()
        );
    }
}