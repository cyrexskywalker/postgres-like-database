package catalog.manager;

import catalog.model.ColumnDefinition;
import catalog.model.TableDefinition;
import catalog.model.TypeDefinition;

import java.util.List;

public interface CatalogManager {

    TableDefinition createTable(String name, List<ColumnDefinition> columns);

    TableDefinition getTable(String tableName);

    ColumnDefinition getColumn(TableDefinition table, String columnName);

    List<TableDefinition> listTables();

    TypeDefinition getTypeByName(String resultType);
}
