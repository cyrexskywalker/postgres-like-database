package lexer;

public enum TokenType {
    // ключевые слова
    SELECT,
    FROM,
    WHERE,
    AND,
    OR,
    NOT,
    AS,

    CREATE,
    TABLE,
    INSERT,
    INTO,
    VALUES,
    INDEX,
    ON,

    // идентификаторы/имена
    IDENT,

    // литералы
    NUMBER,
    STRING,

    // операторы
    EQ,
    NE,
    LT,
    LE,
    GT,
    GE,
    PLUS,
    MINUS,
    STAR,
    SLASH,

    // разделители
    COMMA,
    DOT,
    LPAREN,
    RPAREN,
    SEMICOLON,

    // специальные
    EOF
}
