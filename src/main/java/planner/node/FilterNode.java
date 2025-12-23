package planner.node;

import semantic.QueryTree;

public class FilterNode extends LogicalPlanNode {
    private final LogicalPlanNode input;
    private final QueryTree.QTExpr predicate;

    public FilterNode(LogicalPlanNode input, QueryTree.QTExpr predicate) {
        super("Filter");
        this.input = input;
        this.predicate = predicate;
        this.outputColumns = input.getOutputColumns();
    }

    public LogicalPlanNode getInput() {
        return input;
    }

    public QueryTree.QTExpr getPredicate() {
        return predicate;
    }

    @Override
    public String prettyPrint(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("Filter(").append(predicate).append(")\n");
        sb.append(input.prettyPrint(indent + "  "));
        return sb.toString();
    }
}