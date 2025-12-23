package semantic;

import catalog.manager.CatalogManager;
import parser.nodes.AstNode;

public interface SemanticAnalyzer {
    QueryTree analyze(AstNode ast, CatalogManager catalog);
}
