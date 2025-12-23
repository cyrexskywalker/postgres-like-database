package parser.nodes;

import java.util.List;

public class BoolExpr extends Expr {
    public final String boolop;
    public final List<Expr> args;

    public BoolExpr(String boolop, List<Expr> args) {
        this.boolop = boolop;
        this.args = args;
    }

    @Override
    public String toString() {
        return "BoolExpr(" + boolop + ", args=" + (args == null ? 0 : args.size()) + ")";
    }
}
