package parser;

import lexer.Token;
import parser.nodes.AstNode;

import java.util.List;

public interface Parser {
    AstNode parse(List<Token> tokens);

}
