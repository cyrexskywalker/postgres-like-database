package planner.node;

import ast.TargetEntry;

import java.util.List;
import java.util.stream.Collectors;

public class ProjectNode extends LogicalPlanNode {
    private final LogicalPlanNode input;
    private final List<TargetEntry> targets;

    public ProjectNode(LogicalPlanNode input, List<TargetEntry> targets) {
        super("Project");
        this.input = input;
        this.targets = targets;
        this.outputColumns = targets.stream()
                .map(t -> t.alias != null ? t.alias : t.expr.toString())
                .collect(Collectors.toList());
    }

    public LogicalPlanNode getInput() {
        return input;
    }

    public List<TargetEntry> getTargets() {
        return targets;
    }

    @Override
    public String prettyPrint(String indent) {
        String cols = String.join(", ", outputColumns);
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("Project(").append(cols).append(")\n");
        sb.append(input.prettyPrint(indent + "  "));
        return sb.toString();
    }
}