package parser.nodes;

public class AConst extends Expr {
    public final Object value;

    public AConst(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        if (value == null) return "AConst(NULL)";
        if (value instanceof String s) return "AConst('" + s + "')";
        return "AConst(" + value + ")";
    }
}
