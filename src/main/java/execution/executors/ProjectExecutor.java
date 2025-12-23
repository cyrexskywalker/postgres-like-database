package execution.executors;

import semantic.QueryTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Executor для SELECT-проекции.
 **/
public class ProjectExecutor implements Executor {

    private final Executor child;
    private final List<QueryTree.QTExpr> targets;

    public ProjectExecutor(Executor child, List<QueryTree.QTExpr> targets) {
        this.child = child;
        this.targets = targets;
    }

    @Override
    public void open() {
        child.open();
    }

    @Override
    public Object next() {
        Object row = child.next();
        if (row == null) return null;

        if (!(row instanceof Map<?, ?> map)) {
            throw new IllegalStateException(
                    "ProjectExecutor expected row as Map<String,Object>"
            );
        }

        List<Object> out = new ArrayList<>(targets.size());

        for (QueryTree.QTExpr expr : targets) {
            out.add(evaluate(expr, map));
        }

        return out;
    }

    @Override
    public void close() {
        child.close();
    }

    private Object evaluate(QueryTree.QTExpr expr, Map<?, ?> row) {
        if (expr instanceof QueryTree.QTColumn c) {
            return row.get(c.column.name());
        }
        if (expr instanceof QueryTree.QTConst c) {
            return c.value;
        }
        throw new UnsupportedOperationException(
                "Unsupported projection expression: " + expr
        );
    }
}