package parser.nodes;

public class RangeVar extends AstNode {
    public final String schemaname;
    public final String relname;
    public final String alias;

    public RangeVar(String schemaname, String relname, String alias) {
        this.schemaname = schemaname;
        this.relname = relname;
        this.alias = alias;
    }

    @Override
    public String toString() {
        String base = (schemaname == null || schemaname.isBlank())
                ? relname
                : schemaname + "." + relname;
        return (alias == null || alias.isBlank())
                ? "RangeVar(" + base + ")"
                : "RangeVar(" + base + " AS " + alias + ")";
    }
}
