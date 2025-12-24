package optimizer;

import optimizer.node.PhysicalPlanNode;
import planner.node.LogicalPlanNode;

public interface Optimizer {
    PhysicalPlanNode optimize(LogicalPlanNode logicalPlan);
}
