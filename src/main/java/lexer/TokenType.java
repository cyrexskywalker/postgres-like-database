package lexer;

public enum TokenType {
    //ключевые слова
    SELECT,
    FROM,
    WHERE,
    AND,
    OR,
    NOT,
    AS,

    //идентификаторы/имена
    IDENT,

    //литералы
    NUMBER,
    STRING,

    //операторы
    EQ,             //  =
    NE,             //  <>, !=
    LT,             //  <
    LE,             //  <=
    GT,             //  >
    GE,             //  >=
    PLUS,           //  +
    MINUS,          //  -
    STAR,           //  *
    SLASH,          //  /

    //разделители
    COMMA,
    DOT,
    LPAREN,    // левая скобочка :(
    RPAREN,    // правая скобочка :)
    SEMICOLON,

    //специальные
    EOF
}
