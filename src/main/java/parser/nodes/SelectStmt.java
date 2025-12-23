package parser.nodes;

import java.util.List;

public class SelectStmt extends AstNode {
    public final List<ResTarget> targetList;
    public final List<RangeVar> fromClause;
    public final Expr whereClause;

    public SelectStmt(List<ResTarget> targetList, List<RangeVar> fromClause, Expr whereClause) {
        this.targetList = targetList;
        this.fromClause = fromClause;
        this.whereClause = whereClause;
    }

    @Override
    public String toString() {
        int t = targetList == null ? 0 : targetList.size();
        int f = fromClause == null ? 0 : fromClause.size();
        String w = (whereClause == null) ? "null" : whereClause.toString();
        return "SelectStmt(targets=" + t + ", from=" + f + ", where=" + w + ")";
    }
}
