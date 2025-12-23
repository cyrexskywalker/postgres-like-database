package execution.executors;


import catalog.manager.CatalogManager;
import catalog.model.TableDefinition;

/**
 * Исполнитель CREATE TABLE.
 * Просто вызывает CatalogManager.createTable() с готовой таблицей.
 */
public class CreateTableExecutor implements Executor {

    private final CatalogManager catalogManager;
    private final TableDefinition tableDefinition;

    public CreateTableExecutor(CatalogManager catalogManager, TableDefinition tableDefinition) {
        this.catalogManager = catalogManager;
        this.tableDefinition = tableDefinition;
    }

    @Override
    public void open() { }

    @Override
    public Object next() {
        catalogManager.createTable(
                tableDefinition.getName(),
                tableDefinition.getColumns()
        );
        return null;
    }

    @Override
    public void close() { }
}