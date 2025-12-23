package optimizer.node;

import ast.TargetEntry;

import java.util.List;
import java.util.stream.Collectors;

public class PhysicalProjectNode extends PhysicalPlanNode {
    private final PhysicalPlanNode input;
    private final List<TargetEntry> targets;

    public PhysicalProjectNode(PhysicalPlanNode input, List<TargetEntry> targets) {
        super("PhysicalProject");
        this.input = input;
        this.targets = targets;
    }

    public PhysicalPlanNode getInput() {
        return input;
    }

    public List<TargetEntry> getTargets() {
        return targets;
    }

    @Override
    public String prettyPrint(String indent) {
        String cols = targets.stream().map(Object::toString).collect(Collectors.joining(", "));
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("PhysicalProject(").append(cols).append(")\n");
        sb.append(input.prettyPrint(indent + "  "));
        return sb.toString();
    }
}