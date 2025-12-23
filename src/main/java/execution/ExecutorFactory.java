package execution;

import execution.executors.Executor;
import optimizer.node.PhysicalPlanNode;

public interface ExecutorFactory {
    Executor createExecutor(PhysicalPlanNode plan);

}
