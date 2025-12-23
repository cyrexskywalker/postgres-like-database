package parser.nodes;

public class AExpr extends Expr {
    public final String op;
    public final Expr left;
    public final Expr right;

    public AExpr(String op, Expr left, Expr right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        if (right == null) {
            return "AExpr(" + op + ", " + left + ")";
        }
        return "AExpr(" + op + ", " + left + ", " + right + ")";
    }
}
