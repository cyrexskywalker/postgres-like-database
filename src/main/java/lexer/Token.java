package lexer;

public class Token {
    private final String lexeme;
    private final int position;
    private final TokenType type;

    public Token(String lexeme, int position, TokenType type) {
        this.lexeme = lexeme;
        this.position = position;
        this.type = type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getPosition() {
        return position;
    }

    public TokenType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type + (lexeme != null ? "(" + lexeme + ")" : "");
    }
}
