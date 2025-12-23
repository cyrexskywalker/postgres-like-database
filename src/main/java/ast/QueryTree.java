package ast;

import java.util.ArrayList;
import java.util.List;

public class QueryTree {
    public List<RangeTblEntry> rangeTable;  // таблицы, участвующие в запросе
    public List<TargetEntry> targetList;    // что выбираем
    public Expr whereClause;                // условие отбора
    public QueryType commandType;           // SELECT, INSERT, etc.


    public QueryTree() {
        this.rangeTable = new ArrayList<>();
        this.targetList = new ArrayList<>();
    }

}
