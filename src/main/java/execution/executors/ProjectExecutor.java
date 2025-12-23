package execution.executors;

import ast.ColumnRef;
import ast.TargetEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProjectExecutor implements Executor {

    private final Executor child;
    private final List<TargetEntry> targets;

    public ProjectExecutor(Executor child, List<TargetEntry> targets) {
        this.child = child;
        this.targets = targets;
    }

    @Override
    public void open() { child.open(); }

    @Override
    public Object next() {
        Object row = child.next();
        if (row == null) return null;

        Map<?,?> map = (Map<?,?>) row;

        List<Object> out = new ArrayList<>(targets.size());
        for (TargetEntry t : targets) {
            if (t.expr instanceof ColumnRef cr) {
                out.add(map.get(cr.column));
            } else {
                out.add(null);
            }
        }
        return out;
    }

    @Override
    public void close() { child.close(); }
}