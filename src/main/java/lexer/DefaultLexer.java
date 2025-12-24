package lexer;

import java.util.ArrayList;
import java.util.List;

public class DefaultLexer implements Lexer {
    private String sql;
    private int position;
    private int length;

    @Override
    public List<Token> tokenize(String sql) {
        this.sql = sql != null ? sql : "";
        this.position = 0;
        this.length = this.sql.length();

        List<Token> tokens = new ArrayList<>();

        while (!isAtEnd()) {
            char c = peek();

            if (Character.isWhitespace(c)) {
                position++;
                continue;
            }

            int start = position;

            //идентификатор или ключевое слово
            if (Character.isLetter(c) || c == '_') {
                position++;
                while (position < length) {
                    char d = peek();
                    if (Character.isLetterOrDigit(d) || d == '_') position++;
                    else break;
                }
                String lexeme = sql.substring(start, position);
                String upper  = lexeme.toUpperCase();
                TokenType type = switch (upper) {
                    case "SELECT" -> TokenType.SELECT;
                    case "FROM"   -> TokenType.FROM;
                    case "WHERE"  -> TokenType.WHERE;
                    case "AND"    -> TokenType.AND;
                    case "OR"     -> TokenType.OR;
                    case "NOT"    -> TokenType.NOT;
                    case "AS"     -> TokenType.AS;

                    case "CREATE" -> TokenType.CREATE;
                    case "TABLE"  -> TokenType.TABLE;
                    case "INSERT" -> TokenType.INSERT;
                    case "INTO"   -> TokenType.INTO;
                    case "VALUES" -> TokenType.VALUES;

                    case "INDEX"  -> TokenType.INDEX;
                    case "ON"     -> TokenType.ON;

                    default       -> TokenType.IDENT;
                };
                tokens.add(new Token(lexeme, start, type));
                continue;
            }

            //число
            if (Character.isDigit(c)) {
                while (position < length && Character.isDigit(peek())) {
                    position++;
                }
                String lexeme = sql.substring(start, position);
                tokens.add(new Token(lexeme, start, TokenType.NUMBER));
                continue;
            }

            //строка в одинарных кавычках
            if (c == '\'') {
                position++;
                StringBuilder sb = new StringBuilder();
                boolean closed = false;
                while (position < length) {
                    char d = sql.charAt(position++);
                    if (d == '\'') {
                        if (position < length && sql.charAt(position) == '\'') {
                            sb.append('\'');
                            position++;
                        } else {
                            closed = true;
                            break;
                        }
                    } else {
                        sb.append(d);
                    }
                }
                if (!closed) throw new IllegalArgumentException("Unterminated string at " + start);
                tokens.add(new Token(sb.toString(), start, TokenType.STRING));
                continue;
            }

            //двухсимвольные операторы
            if (position + 1 < length) {
                String two = sql.substring(position, position + 2);
                switch (two) {
                    case "<=" -> { tokens.add(new Token(two, start, TokenType.LE)); position += 2; continue; }
                    case ">=" -> { tokens.add(new Token(two, start, TokenType.GE)); position += 2; continue; }
                    case "<>", "!=" -> { tokens.add(new Token(two, start, TokenType.NE)); position += 2; continue; }
                }
            }

            //односимвольные
            switch (c) {
                case '=' -> tokens.add(new Token("=", start, TokenType.EQ));
                case '<' -> tokens.add(new Token("<", start, TokenType.LT));
                case '>' -> tokens.add(new Token(">", start, TokenType.GT));
                case '+' -> tokens.add(new Token("+", start, TokenType.PLUS));
                case '-' -> tokens.add(new Token("-", start, TokenType.MINUS));
                case '*' -> tokens.add(new Token("*", start, TokenType.STAR));
                case '/' -> tokens.add(new Token("/", start, TokenType.SLASH));
                case ',' -> tokens.add(new Token(",", start, TokenType.COMMA));
                case '.' -> tokens.add(new Token(".", start, TokenType.DOT));
                case '(' -> tokens.add(new Token("(", start, TokenType.LPAREN));
                case ')' -> tokens.add(new Token(")", start, TokenType.RPAREN));
                case ';' -> tokens.add(new Token(";", start, TokenType.SEMICOLON));
                default  -> throw new IllegalArgumentException("Unexpected char '" + c + "' at " + start);
            }
            position++;
        }

        tokens.add(new Token("", length, TokenType.EOF));
        return tokens;
    }

    private boolean isAtEnd() { return position >= length; }
    private char peek() { return isAtEnd() ? '\0' : sql.charAt(position); }
    private char peekNext() {
        int next = position + 1;
        return (next >= length) ? '\0' : sql.charAt(next);
    }
    private char advance() { return isAtEnd() ? '\0' : sql.charAt(position++); }
    public boolean match(char expected) {
        if (peek() != expected) return false;
        position++;
        return true;
    }
}