package optimizer.node;

import ast.Expr;

public class PhysicalFilterNode extends PhysicalPlanNode {
    private final PhysicalPlanNode input;
    private final Expr predicate;

    public PhysicalFilterNode(PhysicalPlanNode input, Expr predicate) {
        super("PhysicalFilter");
        this.input = input;
        this.predicate = predicate;
    }

    public PhysicalPlanNode getInput() {
        return input;
    }

    public Expr getPredicate() {
        return predicate;
    }

    @Override
    public String prettyPrint(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("PhysicalFilter(").append(predicate).append(")\n");
        sb.append(input.prettyPrint(indent + "  "));
        return sb.toString();
    }
}