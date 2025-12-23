package semantic;

import catalog.manager.CatalogManager;
import catalog.model.ColumnDefinition;
import catalog.model.TableDefinition;
import parser.nodes.*;

import java.util.*;

public class DefaultSemanticAnalyzer implements SemanticAnalyzer {

    @Override
    public QueryTree analyze(AstNode ast, CatalogManager catalog) {
        if (!(ast instanceof SelectStmt select)) {
            throw new SemanticException("Only SELECT is supported in this analyzer");
        }
        FromContext fromCtx = resolveFrom(select, catalog);
        List<QueryTree.QTExpr> targets = new ArrayList<>();
        for (ResTarget rt : select.targetList) {
            Expr val = rt.val;
            QueryTree.QTExpr qexpr = resolveExpr(val, fromCtx, catalog);
            targets.add(qexpr);
        }
        QueryTree.QTExpr filter = null;
        if (select.whereClause != null) {
            filter = resolveExpr(select.whereClause, fromCtx, catalog);
            if (!isBooleanLike(filter)) {
                throw new SemanticException("WHERE clause is not boolean");
            }
        }
        return new QueryTree(fromCtx.tables, targets, filter);
    }

    private FromContext resolveFrom(SelectStmt select, CatalogManager catalog) {
        if (select.fromClause == null || select.fromClause.isEmpty()) {
            throw new SemanticException("SELECT must have FROM clause");
        }
        Map<String, TableDefinition> aliasToTable = new LinkedHashMap<>();
        List<TableDefinition> tables = new ArrayList<>();
        for (RangeVar rv : select.fromClause) {
            String rel = rv.relname;
            if (rel == null || rel.isBlank()) {
                throw new SemanticException("RangeVar has empty relname");
            }
            TableDefinition td = catalog.getTable(rel);
            if (td == null) {
                throw new SemanticException("Unknown table: " + rel);
            }
            String alias = (rv.alias != null && !rv.alias.isBlank()) ? rv.alias : td.name();
            if (aliasToTable.containsKey(alias)) {
                throw new SemanticException("Duplicate table alias: " + alias);
            }
            aliasToTable.put(alias, td);
            tables.add(td);
        }
        return new FromContext(aliasToTable, tables);
    }

    private static class FromContext {
        final Map<String, TableDefinition> aliasToTable;
        final List<TableDefinition> tables;

        FromContext(Map<String, TableDefinition> aliasToTable, List<TableDefinition> tables) {
            this.aliasToTable = aliasToTable;
            this.tables = tables;
        }
    }

    private QueryTree.QTExpr resolveExpr(Expr e, FromContext fromCtx, CatalogManager catalog) {
        if (e instanceof ColumnRef cr) {
            return resolveColumnRef(cr, fromCtx, catalog);
        }
        if (e instanceof AConst ac) {
            return constToQT(ac);
        }
        if (e instanceof AExpr ax) {
            QueryTree.QTExpr l = resolveExpr(ax.left, fromCtx, catalog);
            QueryTree.QTExpr r = resolveExpr(ax.right, fromCtx, catalog);
            checkBinaryOpTypes(ax.op, l, r);
            return new QueryTree.QTAExpr(ax.op, l, r);
        }
        if (e instanceof BoolExpr be) {
            List<QueryTree.QTExpr> args = new ArrayList<>();
            for (Expr child : be.args) {
                args.add(resolveExpr(child, fromCtx, catalog));
            }
            if ("AND".equalsIgnoreCase(be.boolop) || "OR".equalsIgnoreCase(be.boolop)) {
                for (QueryTree.QTExpr q : args) {
                    if (!isBooleanLike(q)) {
                        throw new SemanticException(be.boolop + " argument is not boolean");
                    }
                }
            }
            if ("NOT".equalsIgnoreCase(be.boolop)) {
                if (args.size() != 1) {
                    throw new SemanticException("NOT must have exactly one operand");
                }
                if (!isBooleanLike(args.get(0))) {
                    throw new SemanticException("NOT argument is not boolean");
                }
            }
            return new QueryTree.QTBoolExpr(be.boolop, args);
        }
        throw new SemanticException("unsupported expression node: " + e.getClass().getSimpleName());
    }

    private QueryTree.QTExpr resolveColumnRef(ColumnRef cr, FromContext fromCtx, CatalogManager catalog) {
        String tableAlias = cr.table;
        String colName = cr.column;
        if (colName == null || colName.isBlank()) {
            throw new SemanticException("empty column name");
        }
        if (tableAlias != null && !tableAlias.isBlank()) {
            TableDefinition td = fromCtx.aliasToTable.get(tableAlias);
            if (td == null) {
                throw new SemanticException("unknown table alias: " + tableAlias);
            }
            ColumnDefinition cd = findColumnInTable(catalog, td, colName);
            return new QueryTree.QTColumn(cd, td, mapTypeName(cd.typeOid()));
        }
        QueryTree.QTColumn found = null;
        for (var entry : fromCtx.aliasToTable.entrySet()) {
            TableDefinition td = entry.getValue();
            ColumnDefinition cd = findColumnInTableOrNull(catalog, td, colName);
            if (cd != null) {
                if (found != null) {
                    throw new SemanticException("ambiguous column reference: " + colName);
                }
                found = new QueryTree.QTColumn(cd, td, mapTypeName(cd.typeOid()));
            }
        }
        if (found == null) {
            throw new SemanticException("column not found: " + colName);
        }
        return found;
    }

    private ColumnDefinition findColumnInTable(CatalogManager catalog, TableDefinition td, String colName) {
        ColumnDefinition cd = findColumnInTableOrNull(catalog, td, colName);
        if (cd == null) {
            throw new SemanticException("column " + colName + " not found in table " + td.name());
        }
        return cd;
    }

    private ColumnDefinition findColumnInTableOrNull(CatalogManager catalog, TableDefinition td, String colName) {
        try {
            return catalog.getColumn(td, colName);
        } catch (Exception ignored) {
            return null;
        }
    }

    private QueryTree.QTExpr constToQT(AConst ac) {
        Object v = ac.value;
        if (v == null) return new QueryTree.QTConst(null, "NULL");
        if (v instanceof Number) return new QueryTree.QTConst(v, "INT64");
        if (v instanceof String) return new QueryTree.QTConst(v, "VARCHAR");
        if (v instanceof Boolean) return new QueryTree.QTConst(v, "BOOLEAN");
        return new QueryTree.QTConst(String.valueOf(v), "UNKNOWN");
    }

    private String mapTypeName(int typeOid) {
        return switch (typeOid) {
            case 1 -> "INT64";
            case 2 -> "VARCHAR";
            default -> "UNKNOWN";
        };
    }

    private void checkBinaryOpTypes(String op, QueryTree.QTExpr left, QueryTree.QTExpr right) {
        String lop = op.toUpperCase(Locale.ROOT);
        if (isComparison(lop)) {
            if (isEquality(lop)) {
                if (!sameOrCompatibleTypes(typeOf(left), typeOf(right))) {
                    throw new SemanticException("type mismatch in equality: " + typeOf(left) + " " + op + " " + typeOf(right));
                }
                return;
            } else {
                if (!(isNumeric(typeOf(left)) && isNumeric(typeOf(right)))) {
                    throw new SemanticException("non-numeric types in comparison: " + typeOf(left) + " " + op + " " + typeOf(right));
                }
                return;
            }
        }
        if (isArithmetic(lop)) {
            if (!(isNumeric(typeOf(left)) && isNumeric(typeOf(right)))) {
                throw new SemanticException("arithmetic on non-numeric types: " + typeOf(left) + " " + op + " " + typeOf(right));
            }
        }
    }

    private boolean isComparison(String op) {
        return switch (op) {
            case "=", "<>", "!=", "<", "<=", ">", ">=" -> true;
            default -> false;
        };
    }

    private boolean isEquality(String op) {
        return "=".equals(op) || "<>".equals(op) || "!=".equals(op);
    }

    private boolean isArithmetic(String op) {
        return "+".equals(op) || "-".equals(op) || "*".equals(op) || "/".equals(op);
    }

    private boolean isNumeric(String t) {
        return "INT".equalsIgnoreCase(t) || "INT64".equalsIgnoreCase(t)
                || "NUMERIC".equalsIgnoreCase(t) || "DOUBLE".equalsIgnoreCase(t);
    }

    private boolean sameOrCompatibleTypes(String a, String b) {
        if (a == null || b == null) return false;
        if (a.equalsIgnoreCase(b)) return true;
        if ((a.equalsIgnoreCase("INT") && b.equalsIgnoreCase("INT64"))
                || (a.equalsIgnoreCase("INT64") && b.equalsIgnoreCase("INT"))) {
            return true;
        }
        return false;
    }

    private boolean isBooleanLike(QueryTree.QTExpr e) {
        if (e instanceof QueryTree.QTAExpr qa) {
            if (isComparison(qa.op.toUpperCase(Locale.ROOT))) return true;
        }
        if (e instanceof QueryTree.QTBoolExpr) return true;
        if (e instanceof QueryTree.QTConst c && "BOOLEAN".equalsIgnoreCase(c.type)) return true;
        return false;
    }

    private String typeOf(QueryTree.QTExpr e) {
        if (e instanceof QueryTree.QTConst c) return c.type;
        if (e instanceof QueryTree.QTColumn qc) return qc.type;
        if (e instanceof QueryTree.QTAExpr qa) {
            String op = qa.op.toUpperCase(Locale.ROOT);
            if (isComparison(op)) return "BOOLEAN";
            if (isArithmetic(op)) {
                String lt = typeOf(qa.left);
                String rt = typeOf(qa.right);
                return (isNumeric(lt) && isNumeric(rt)) ? "INT64" : "UNKNOWN";
            }
            return "UNKNOWN";
        }
        if (e instanceof QueryTree.QTBoolExpr) return "BOOLEAN";
        return "UNKNOWN";
    }

    public static class SemanticException extends RuntimeException {
        public SemanticException(String msg) { super(msg); }
    }
}