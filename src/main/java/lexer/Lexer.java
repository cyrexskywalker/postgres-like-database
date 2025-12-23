package lexer;

import java.util.List;


public interface Lexer {
    List<Token> tokenize(String sql);

}
