import catalog.manager.CatalogManager;
import lexer.Lexer;
import lexer.Token;
import parser.Parser;
import parser.nodes.AstNode;
import semantic.QueryTree;
import semantic.SemanticAnalyzer;

import java.util.List;
import java.util.Objects;

public class SqlProcessor {
    private final Lexer lexer;
    private final Parser parser;
    private final SemanticAnalyzer semanticAnalyzer;
    private final CatalogManager catalogManager;

    public SqlProcessor(Lexer lexer,
                        Parser parser,
                        SemanticAnalyzer semanticAnalyzer,
                        CatalogManager catalogManager) {
        this.lexer = Objects.requireNonNull(lexer, "lexer");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.semanticAnalyzer = Objects.requireNonNull(semanticAnalyzer, "semanticAnalyzer");
        this.catalogManager = Objects.requireNonNull(catalogManager, "catalogManager");
    }

    public QueryTree process(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL is empty");
        }

        List<Token> tokens = lexer.tokenize(sql);
        System.out.println("Tokens: " + tokens);

        AstNode ast = parser.parse(tokens);
        System.out.println("AST: " + ast);

        QueryTree queryTree = semanticAnalyzer.analyze(ast, catalogManager);
        System.out.println("QueryTree:\n" + queryTree);

        return queryTree;
    }
}