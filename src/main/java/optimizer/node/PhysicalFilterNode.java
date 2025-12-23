package optimizer.node;

import semantic.QueryTree;

public class PhysicalFilterNode extends PhysicalPlanNode {

    private final PhysicalPlanNode input;
    private final QueryTree.QTExpr predicate;

    public PhysicalFilterNode(PhysicalPlanNode input, QueryTree.QTExpr predicate) {
        super("PhysicalFilter");
        this.input = input;
        this.predicate = predicate;
    }

    public PhysicalPlanNode getInput() {
        return input;
    }

    public QueryTree.QTExpr getPredicate() {
        return predicate;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "PhysicalFilter(" + predicate + ")\n"
                + input.prettyPrint(indent + "  ");
    }
}