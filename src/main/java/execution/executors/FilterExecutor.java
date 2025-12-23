package execution.executors;

import ast.Expr;

public class FilterExecutor implements Executor {
    private final Executor input;
    private final Expr predicate;

    public FilterExecutor(Executor input, Expr predicate) {
        this.input = input;
        this.predicate = predicate;
    }

    @Override
    public void open() {
        input.open();
    }

    @Override
    public Object next() {
        return input.next();
    }

    @Override
    public void close() {
        input.close();
    }
}