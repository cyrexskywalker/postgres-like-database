package execution;

import catalog.manager.CatalogManager;
import catalog.operation.OperationManager;
import execution.executors.*;
import optimizer.node.*;

public class ExecutorFactoryImpl implements ExecutorFactory {

    private final CatalogManager catalogManager;
    private final OperationManager operationManager;

    public ExecutorFactoryImpl(CatalogManager catalogManager, OperationManager operationManager) {
        this.catalogManager = catalogManager;
        this.operationManager = operationManager;
    }

    @Override
    public Executor createExecutor(PhysicalPlanNode plan) {
        if (plan instanceof PhysicalCreateNode n) {
            return new CreateTableExecutor(catalogManager, n.getTableDefinition());
        }
        if (plan instanceof PhysicalInsertNode n) {
            return new InsertExecutor(operationManager, n.getTableDefinition(), n.getValues());
        }
        if (plan instanceof PhysicalSeqScanNode n) {
            return new SeqScanExecutor(
                    operationManager,
                    n.getTable().getName(),
                    n.getColumns()
            );
        }
        if (plan instanceof PhysicalFilterNode n) {
            Executor child = createExecutor(n.getInput());
            return new FilterExecutor(child, n.getPredicate());
        }
        if (plan instanceof PhysicalProjectNode n) {
            Executor child = createExecutor(n.getInput());
            return new ProjectExecutor(child, n.getTargets());
        }
        throw new UnsupportedOperationException("Unsupported physical plan node: " + plan.getClass().getSimpleName());
    }
}