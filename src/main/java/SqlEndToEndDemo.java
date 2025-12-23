import catalog.manager.DefaultCatalogManager;
import catalog.model.ColumnDefinition;
import catalog.model.TableDefinition;
import catalog.model.TypeDefinition;
import catalog.operation.DefaultOperationManager;
import execution.ExecutorFactoryImpl;
import execution.QueryExecutionEngineImpl;
import execution.executors.Executor;
import lexer.DefaultLexer;
import lexer.Lexer;
import lexer.Token;
import memory.buffer.DefaultBufferPoolManager;
import memory.manager.HeapPageFileManager;
import memory.replacer.ClockReplacer;
import optimizer.OptimizerImpl;
import optimizer.node.PhysicalPlanNode;
import parser.DefaultParser;
import parser.Parser;
import parser.nodes.AstNode;
import planner.PlannerImpl;
import planner.node.LogicalPlanNode;
import semantic.DefaultSemanticAnalyzer;
import semantic.QueryTree;
import semantic.SemanticAnalyzer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SqlEndToEndDemo {

    public static void main(String[] args) {
        try {
            run();
        } catch (Throwable t) {
            System.err.println("\n=== FATAL ERROR ===");
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void run() throws Exception {
        System.out.println("=== INIT STORAGE ===");

        Path dataRoot = Files.createTempDirectory("db-demo");
        System.out.println("Data dir: " + dataRoot);

        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bpm =
                new DefaultBufferPoolManager(
                        10,
                        pageFileManager,
                        new ClockReplacer()
                );

        DefaultCatalogManager catalog =
                new DefaultCatalogManager(dataRoot.resolve("catalog"), bpm);

        DefaultOperationManager operationManager =
                new DefaultOperationManager(
                        catalog,
                        new DefaultOperationManager.CatalogAccess() {
                            @Override
                            public List<ColumnDefinition> listColumnsSorted(TableDefinition table) {
                                return catalog.listColumnsSorted(table);
                            }

                            @Override
                            public TypeDefinition getTypeByOid(int typeOid) {
                                return catalog.getTypeByOid(typeOid);
                            }

                            @Override
                            public void updatePagesCount(int tableOid, int newPagesCount) {
                                catalog.updatePagesCount(tableOid, newPagesCount);
                            }
                        },
                        bpm,
                        dataRoot.resolve("data")
                );

        Lexer lexer = new DefaultLexer();
        Parser parser = new DefaultParser();
        SemanticAnalyzer semanticAnalyzer = new DefaultSemanticAnalyzer();
        PlannerImpl planner = new PlannerImpl(catalog);
        OptimizerImpl optimizer = new OptimizerImpl(catalog);

        ExecutorFactoryImpl executorFactory =
                new ExecutorFactoryImpl(catalog, operationManager);

        QueryExecutionEngineImpl engine =
                new QueryExecutionEngineImpl();

        execSql(
                "SELECT 1",
                lexer,
                parser,
                semanticAnalyzer,
                planner,
                optimizer,
                executorFactory,
                engine
        );
    }

    private static void execSql(
            String sql,
            Lexer lexer,
            Parser parser,
            SemanticAnalyzer semanticAnalyzer,
            PlannerImpl planner,
            OptimizerImpl optimizer,
            ExecutorFactoryImpl executorFactory,
            QueryExecutionEngineImpl engine
    ) {
        System.out.println("\n====================================");
        System.out.println("SQL: " + sql);

        List<Token> tokens = lexer.tokenize(sql);
        System.out.println("[TOKENS]");
        System.out.println(tokens);

        var ast = parser.parse(tokens);
        System.out.println("[AST]");
        System.out.println(ast);

        QueryTree qt = semanticAnalyzer.analyze(ast, null);
        System.out.println("[QUERY TREE]");
        System.out.println(qt);

        LogicalPlanNode logicalPlan = planner.plan(qt);
        System.out.println("[LOGICAL PLAN]");
        System.out.println(logicalPlan.prettyPrint(""));

        PhysicalPlanNode physicalPlan = optimizer.optimize(logicalPlan);
        System.out.println("[PHYSICAL PLAN]");
        System.out.println(physicalPlan.prettyPrint(""));

        Executor executor = executorFactory.createExecutor(physicalPlan);

        List<Object> result = engine.execute(executor);
        System.out.println("[RESULT]");
        result.forEach(System.out::println);
    }
}