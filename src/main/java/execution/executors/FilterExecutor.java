package execution.executors;

import semantic.QueryTree;

import java.util.Map;

/**
 * Исполнитель WHERE-фильтра.
 * Работает с SEMANTIC QueryTree, а не с AST.
 */
public class FilterExecutor implements Executor {

    private final Executor input;
    private final QueryTree.QTExpr predicate;

    public FilterExecutor(Executor input, QueryTree.QTExpr predicate) {
        this.input = input;
        this.predicate = predicate;
    }

    @Override
    public void open() {
        input.open();
    }

    @Override
    public Object next() {
        while (true) {
            Object row = input.next();
            if (row == null) {
                return null;
            }

            if (evaluatePredicate(predicate, row)) {
                return row;
            }
        }
    }

    @Override
    public void close() {
        input.close();
    }

    @SuppressWarnings("unchecked")
    private boolean evaluatePredicate(QueryTree.QTExpr expr, Object row) {
        if (!(row instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Filter expects row as Map<String,Object>");
        }

        if (expr instanceof QueryTree.QTAExpr a) {
            Object left = evalValue(a.left, map);
            Object right = evalValue(a.right, map);

            return switch (a.op) {
                case "="  -> left.equals(right);
                case ">"  -> compare(left, right) > 0;
                case "<"  -> compare(left, right) < 0;
                case ">=" -> compare(left, right) >= 0;
                case "<=" -> compare(left, right) <= 0;
                default -> throw new UnsupportedOperationException("Unsupported operator: " + a.op);
            };
        }

        throw new UnsupportedOperationException("Unsupported predicate: " + expr);
    }

    private Object evalValue(QueryTree.QTExpr expr, Map<?, ?> row) {
        if (expr instanceof QueryTree.QTConst c) {
            return c.value;
        }
        if (expr instanceof QueryTree.QTColumn c) {
            return row.get(c.column.name());
        }
        throw new UnsupportedOperationException("Unsupported expression: " + expr);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private int compare(Object a, Object b) {
        if (a instanceof Comparable ca && b instanceof Comparable cb) {
            return ca.compareTo(cb);
        }
        throw new IllegalArgumentException("Values are not comparable: " + a + ", " + b);
    }
}