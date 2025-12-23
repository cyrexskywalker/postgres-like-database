package parser.nodes;

public class ResTarget extends AstNode {
    public final Expr val;
    public final String name;

    public ResTarget(Expr val, String name) {
        this.val = val;
        this.name = name;
    }

    @Override
    public String toString() {
        return (name == null || name.isBlank())
                ? "ResTarget(" + val + ")"
                : "ResTarget(" + val + " AS " + name + ")";
    }
}
