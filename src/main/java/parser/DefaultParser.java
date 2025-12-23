package parser;

import lexer.Token;
import lexer.TokenType;
import parser.nodes.*;

import java.util.ArrayList;
import java.util.List;

public class DefaultParser implements Parser {
    private List<Token> tokens;
    private int pos;

    @Override
    public AstNode parse(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("empty token stream");
        }
        this.tokens = tokens;
        this.pos = 0;

        SelectStmt select = parseSelect();

        if (check(TokenType.SEMICOLON)) advance();
        if (check(TokenType.EOF)) advance();

        if (!isAtEnd()) {
            Token t = peek();
            throw new IllegalArgumentException("expected end of input, got: " + t.getType() + " at pos " + t.getPosition());
        }
        return select;
    }

    public SelectStmt parseSelect() {
        expect(TokenType.SELECT, "expected SELECT");
        List<ResTarget> targetList = parseTargetList();

        expect(TokenType.FROM, "expected FROM");
        List<RangeVar> fromList = parseFromList();

        Expr where = null;
        if (match(TokenType.WHERE)) {
            where = parseExpr();
        }
        return new SelectStmt(targetList, fromList, where);
    }

    private List<RangeVar> parseFromList() {
        List<RangeVar> list = new ArrayList<>();
        do { list.add(parseRangeVar()); } while (match(TokenType.COMMA));
        return list;
    }

    private RangeVar parseRangeVar() {
        Token name = expect(TokenType.IDENT, "expected table name");
        String alias = null;
        if (match(TokenType.AS)) {
            alias = expect(TokenType.IDENT, "expected table alias").getLexeme();
        } else if (check(TokenType.IDENT)) {
            alias = advance().getLexeme();
        }
        return new RangeVar(null, name.getLexeme(), alias);
    }

    private List<ResTarget> parseTargetList() {
        List<ResTarget> list = new ArrayList<>();
        do { list.add(parseResTarget()); } while (match(TokenType.COMMA));
        return list;
    }

    private ResTarget parseResTarget() {
        if (match(TokenType.STAR)) {
            ColumnRef star = new ColumnRef("*");
            String alias = parseOptionalAlias();
            return new ResTarget(star, alias);
        }
        Expr val = parseExpr();
        String alias = parseOptionalAlias();
        return new ResTarget(val, alias);
    }

    private String parseOptionalAlias() {
        if (match(TokenType.AS)) {
            return expect(TokenType.IDENT, "expected alias after AS").getLexeme();
        }
        if (check(TokenType.IDENT)) {
            return advance().getLexeme();
        }
        return null;
    }

    // OR -> AND -> NOT -> comparison -> add/sub -> mul/div -> primary
    private Expr parseExpr() { return parseOr(); }

    private Expr parseOr() {
        Expr left = parseAnd();
        while (match(TokenType.OR)) {
            Expr right = parseAnd();
            left = new BoolExpr("OR", List.of(left, right));
        }
        return left;
    }

    private Expr parseAnd() {
        Expr left = parseNot();
        while (match(TokenType.AND)) {
            Expr right = parseNot();
            left = new BoolExpr("AND", List.of(left, right));
        }
        return left;
    }

    private Expr parseNot() {
        if (match(TokenType.NOT)) {
            Expr inner = parseNot();
            return new BoolExpr("NOT", List.of(inner));
        }
        return parseComparison();
    }

    private Expr parseComparison() {
        Expr left = parseAddSub();
        while (true) {
            if (match(TokenType.EQ)) { left = new AExpr("=",  left, parseAddSub()); continue; }
            if (match(TokenType.NE)) { left = new AExpr("<>", left, parseAddSub()); continue; }
            if (match(TokenType.LT)) { left = new AExpr("<",  left, parseAddSub()); continue; }
            if (match(TokenType.LE)) { left = new AExpr("<=", left, parseAddSub()); continue; }
            if (match(TokenType.GT)) { left = new AExpr(">",  left, parseAddSub()); continue; }
            if (match(TokenType.GE)) { left = new AExpr(">=", left, parseAddSub()); continue; }
            break;
        }
        return left;
    }

    private Expr parseAddSub() {
        Expr left = parseMulDiv();
        while (true) {
            if (match(TokenType.PLUS))  { left = new AExpr("+", left, parseMulDiv()); continue; }
            if (match(TokenType.MINUS)) { left = new AExpr("-", left, parseMulDiv()); continue; }
            break;
        }
        return left;
    }

    private Expr parseMulDiv() {
        Expr left = parsePrimary();
        while (true) {
            if (match(TokenType.STAR))  { left = new AExpr("*", left, parsePrimary()); continue; }
            if (match(TokenType.SLASH)) { left = new AExpr("/", left, parsePrimary()); continue; }
            break;
        }
        return left;
    }

    private Expr parsePrimary() {
        if (match(TokenType.LPAREN)) {
            Expr e = parseExpr();
            expect(TokenType.RPAREN, "expected ')'");
            return e;
        }
        if (check(TokenType.NUMBER)) {
            String lx = advance().getLexeme();
            return new AConst(Long.parseLong(lx));
        }
        if (check(TokenType.STRING)) {
            return new AConst(advance().getLexeme());
        }
        Token id = expect(TokenType.IDENT, "expected identifier or literal");
        if (match(TokenType.DOT)) {
            Token col = expect(TokenType.IDENT, "expected column after '.'");
            return new ColumnRef(id.getLexeme(), col.getLexeme());
        }
        return new ColumnRef(id.getLexeme());
    }

    private boolean match(TokenType type) {
        if (check(type)) { advance(); return true; }
        return false;
    }

    private Token expect(TokenType type, String msg) {
        if (!check(type)) throw error(peek(), msg + ", got: " + peek().getType());
        return advance();
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().getType() == type;
    }

    private Token advance() {
        if (!isAtEnd()) pos++;
        return previous();
    }

    private boolean isAtEnd() { return pos >= tokens.size(); }

    private Token peek() { return tokens.get(pos); }

    private Token previous() { return tokens.get(pos - 1); }

    private static IllegalArgumentException error(Token t, String msg) {
        return new IllegalArgumentException(msg + " at pos " + t.getPosition());
    }
}