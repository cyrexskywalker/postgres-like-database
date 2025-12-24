package index.registry;

import catalog.model.ColumnDefinition;
import index.TID;
import index.btree.BPlusTreeIndex;

import java.util.List;

public interface IndexRegistry {
    BPlusTreeIndex get(String tableName, String columnName);
    void register(String tableName, String columnName, BPlusTreeIndex index);
    void onInsert(String tableName, List<ColumnDefinition> columns, List<Object> values, TID tid);
}