package planner.node;

import semantic.QueryTree;

import java.util.List;
import java.util.stream.Collectors;

public class ProjectNode extends LogicalPlanNode {

    private final LogicalPlanNode input;
    private final List<QueryTree.QTExpr> targets;

    public ProjectNode(LogicalPlanNode input, List<QueryTree.QTExpr> targets) {
        super("Project");
        this.input = input;
        this.targets = targets;

        // имена выходных колонок — чисто для explain / prettyPrint
        this.outputColumns = targets.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    public LogicalPlanNode getInput() {
        return input;
    }

    public List<QueryTree.QTExpr> getTargets() {
        return targets;
    }

    @Override
    public String prettyPrint(String indent) {
        String cols = String.join(", ", outputColumns);
        StringBuilder sb = new StringBuilder();
        sb.append(indent)
                .append("Project(")
                .append(cols)
                .append(")\n");
        sb.append(input.prettyPrint(indent + "  "));
        return sb.toString();
    }
}