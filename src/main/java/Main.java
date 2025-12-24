import catalog.manager.CatalogManager;
import catalog.manager.DefaultCatalogManager;
import catalog.operation.DefaultOperationManager;
import catalog.operation.OperationManager;
import execution.ExecutorFactory;
import execution.ExecutorFactoryImpl;
import execution.QueryExecutionEngineImpl;
import execution.executors.Executor;
import index.registry.DefaultIndexRegistry;
import index.registry.IndexRegistry;
import lexer.DefaultLexer;
import lexer.Lexer;
import parser.DefaultParser;
import parser.Parser;
import planner.Planner;
import planner.PlannerImpl;
import optimizer.Optimizer;
import optimizer.OptimizerImpl;
import optimizer.node.PhysicalPlanNode;
import planner.node.LogicalPlanNode;
import semantic.DefaultSemanticAnalyzer;
import semantic.SemanticAnalyzer;
import semantic.QueryTree;
import memory.buffer.BufferPoolManager;
import memory.buffer.DefaultBufferPoolManager;
import memory.manager.HeapPageFileManager;
import memory.manager.PageFileManager;
import memory.model.BufferSlot;
import memory.replacer.ClockReplacer;
import memory.replacer.Replacer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        var dbRoot = Path.of("data").toAbsolutePath();

        PageFileManager pfm = new HeapPageFileManager();
        Replacer replacer = new ClockReplacer();
        BufferPoolManager bpm = new DefaultBufferPoolManager(64, pfm, replacer);

        CatalogManager catalog = new DefaultCatalogManager(dbRoot.resolve("catalog"), bpm);

        IndexRegistry indexRegistry = new DefaultIndexRegistry();


        OperationManager op = new DefaultOperationManager(
                catalog,
                (DefaultCatalogManager) catalog,
                bpm,
                dbRoot.resolve("tables"),
                indexRegistry,
                pfm
        );

        Lexer lexer = new DefaultLexer();
        Parser parser = new DefaultParser();
        SemanticAnalyzer semantic = new DefaultSemanticAnalyzer();
        Planner planner = new PlannerImpl(catalog);
        Optimizer optimizer = new OptimizerImpl(catalog, indexRegistry);
        ExecutorFactory executorFactory = new ExecutorFactoryImpl(catalog, op);
        QueryExecutionEngineImpl engine = new QueryExecutionEngineImpl();

        SqlProcessor sqlProcessor = new SqlProcessor(lexer, parser, semantic, catalog);

        System.out.println("DB REPL started. Type 'exit' to quit.");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("> ");
                String sql = br.readLine();
                if (sql == null) break;
                sql = sql.trim();
                if (sql.isEmpty()) continue;
                if (sql.equalsIgnoreCase("exit") || sql.equalsIgnoreCase("quit")) break;

                try {
                    QueryTree qt = sqlProcessor.process(sql);

                    LogicalPlanNode logical = planner.plan(qt);
                    System.out.println("LogicalPlan:\n" + logical.prettyPrint(""));

                    PhysicalPlanNode physical = optimizer.optimize(logical);
                    System.out.println("PhysicalPlan:\n" + physical.prettyPrint(""));

                    Executor exec = executorFactory.createExecutor(physical);
                    List<Object> result = engine.execute(exec);

                    if (result.isEmpty()) {
                        System.out.println("(ok)");
                    } else {
                        for (Object row : result) {
                            System.out.println(row);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            }
        }
    }


}