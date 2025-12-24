package execution.executors;

import semantic.QueryTree;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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
            if (row == null) return null;
            if (evaluatePredicate(predicate, row)) return row;
        }
    }

    @Override
    public void close() {
        input.close();
    }

    private boolean evaluatePredicate(QueryTree.QTExpr expr, Object row) {
        if (!(row instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Filter expects row as Map<String,Object>");
        }

        if (expr instanceof QueryTree.QTBoolExpr b) {
            String op = b.boolop.toUpperCase(Locale.ROOT);
            return switch (op) {
                case "AND" -> b.args.stream().allMatch(e -> evaluatePredicate(e, row));
                case "OR" -> b.args.stream().anyMatch(e -> evaluatePredicate(e, row));
                case "NOT" -> {
                    if (b.args.size() != 1) {
                        throw new IllegalStateException("NOT expects exactly 1 argument, got: " + b.args.size());
                    }
                    yield !evaluatePredicate(b.args.get(0), row);
                }
                default -> throw new UnsupportedOperationException("Unsupported boolean operator: " + b.boolop);
            };
        }

        if (expr instanceof QueryTree.QTAExpr a) {
            Object left = evalValue(a.left, map);
            Object right = evalValue(a.right, map);

            return switch (a.op) {
                case "=" -> equalsNormalized(left, right);
                case "<>", "!=" -> !equalsNormalized(left, right);
                case ">" -> compare(left, right) > 0;
                case "<" -> compare(left, right) < 0;
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
        if (expr instanceof QueryTree.QTAExpr a) {
            Object l = evalValue(a.left, row);
            Object r = evalValue(a.right, row);

            return switch (a.op) {
                case "+" -> toLong(l) + toLong(r);
                case "-" -> toLong(l) - toLong(r);
                case "*" -> toLong(l) * toLong(r);
                case "/" -> {
                    long div = toLong(r);
                    if (div == 0) throw new IllegalArgumentException("division by zero");
                    yield toLong(l) / div;
                }
                default -> throw new UnsupportedOperationException("Unsupported expression operator: " + a.op);
            };
        }

        throw new UnsupportedOperationException("Unsupported expression: " + expr);
    }

    private static boolean equalsNormalized(Object a, Object b) {
        if (a == null || b == null) return a == b;
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).longValue() == ((Number) b).longValue();
        }
        return Objects.equals(a, b);
    }

    private static long toLong(Object v) {
        if (v == null) throw new IllegalArgumentException("NULL in numeric expression");
        if (v instanceof Long x) return x;
        if (v instanceof Integer x) return x.longValue();
        if (v instanceof Short x) return x.longValue();
        if (v instanceof Byte x) return x.longValue();
        if (v instanceof Number x) return x.longValue();
        throw new IllegalArgumentException("Expected numeric value, got: " + v.getClass().getSimpleName() + " (" + v + ")");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static int compare(Object a, Object b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Cannot compare NULL values: " + a + ", " + b);
        }
        if (a instanceof Number && b instanceof Number) {
            long la = ((Number) a).longValue();
            long lb = ((Number) b).longValue();
            return Long.compare(la, lb);
        }
        if (a instanceof Comparable ca && b instanceof Comparable cb) {
            return ca.compareTo(cb);
        }
        throw new IllegalArgumentException("Values are not comparable: " + a + ", " + b);
    }
}