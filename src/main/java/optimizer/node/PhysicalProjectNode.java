package optimizer.node;

import semantic.QueryTree;

import java.util.List;

public class PhysicalProjectNode extends PhysicalPlanNode {

    private final PhysicalPlanNode input;
    private final List<QueryTree.QTExpr> targets;

    public PhysicalProjectNode(PhysicalPlanNode input, List<QueryTree.QTExpr> targets) {
        super("PhysicalProject");
        this.input = input;
        this.targets = targets;
    }

    public PhysicalPlanNode getInput() {
        return input;
    }

    public List<QueryTree.QTExpr> getTargets() {
        return targets;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "PhysicalProject(" + targets + ")\n"
                + input.prettyPrint(indent + "  ");
    }
}