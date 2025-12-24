package index.registry;

import catalog.model.ColumnDefinition;
import index.TID;
import index.btree.BPlusTreeIndex;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DefaultIndexRegistry implements IndexRegistry {

    private final Map<String, Map<String, BPlusTreeIndex>> byTable = new HashMap<>();

    @Override
    public BPlusTreeIndex get(String tableName, String columnName) {
        if (tableName == null || columnName == null) return null;
        Map<String, BPlusTreeIndex> m = byTable.get(norm(tableName));
        if (m == null) return null;
        return m.get(norm(columnName));
    }

    @Override
    public void register(String tableName, String columnName, BPlusTreeIndex index) {
        if (tableName == null || columnName == null || index == null) {
            throw new IllegalArgumentException("bad args");
        }
        byTable.computeIfAbsent(norm(tableName), k -> new HashMap<>())
                .put(norm(columnName), index);
    }

    @Override
    public void onInsert(String tableName, List<ColumnDefinition> columns, List<Object> values, TID tid) {
        if (tableName == null || columns == null || values == null || tid == null) return;
        Map<String, BPlusTreeIndex> m = byTable.get(norm(tableName));
        if (m == null || m.isEmpty()) return;

        for (int i = 0; i < columns.size() && i < values.size(); i++) {
            ColumnDefinition c = columns.get(i);
            BPlusTreeIndex idx = m.get(norm(c.name()));
            if (idx == null) continue;

            Object v = values.get(i);
            if (!(v instanceof Comparable cmp)) {
                throw new IllegalArgumentException("index key is not Comparable for column: " + c.name());
            }
            idx.insert(cmp, tid);
        }
    }

    private static String norm(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}