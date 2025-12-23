package optimizer;

import catalog.manager.CatalogManager;
import optimizer.node.*;
import planner.node.*;
import semantic.QueryTree;

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
            return new PhysicalInsertNode(
                    ln.getTableDefinition(),
                    ln.getValues()
            );
        }

        if (logicalPlan instanceof ScanNode ln) {
            return new PhysicalSeqScanNode(ln.getTable());
        }

        if (logicalPlan instanceof FilterNode ln) {
            PhysicalPlanNode child = optimize(ln.getInput());
            return new PhysicalFilterNode(child, ln.getPredicate());
        }

        if (logicalPlan instanceof ProjectNode ln) {
            PhysicalPlanNode child = optimize(ln.getInput());
            return new PhysicalProjectNode(child, ln.getTargets());
        }

        throw new UnsupportedOperationException(
                "Unsupported logical node type: " +
                        logicalPlan.getClass().getSimpleName()
        );
    }
}