package catalog.operation;

import catalog.manager.CatalogManager;
import catalog.model.ColumnDefinition;
import catalog.model.TableDefinition;
import catalog.model.TypeDefinition;
import index.TID;
import index.btree.BPlusTreeIndex;
import index.btree.BPlusTreeIndexImpl;
import memory.buffer.BufferPoolManager;
import memory.manager.PageFileManager;
import memory.model.BufferSlot;
import memory.page.HeapPage;
import memory.page.Page;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import index.registry.IndexRegistry;


public class DefaultOperationManager implements OperationManager {

    public interface CatalogAccess {
        List<ColumnDefinition> listColumnsSorted(TableDefinition table);

        TypeDefinition getTypeByOid(int typeOid);

        void updatePagesCount(int tableOid, int newPagesCount);
    }

    private final CatalogManager catalog;
    private final CatalogAccess catalogAccess;
    private final BufferPoolManager bpm;
    private final Path dataRoot;
    private final IndexRegistry indexRegistry;

    private final PageFileManager pageManagerForIndexes;

    public DefaultOperationManager(CatalogManager catalog,
                                   CatalogAccess catalogAccess,
                                   BufferPoolManager bpm,
                                   Path dataRoot,
                                   IndexRegistry indexRegistry,
                                   PageFileManager pageManagerForIndexes) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.catalogAccess = Objects.requireNonNull(catalogAccess, "catalogAccess");
        this.bpm = Objects.requireNonNull(bpm, "bpm");
        this.dataRoot = Objects.requireNonNull(dataRoot, "dataRoot");
        this.indexRegistry = indexRegistry;
        this.pageManagerForIndexes = pageManagerForIndexes;
    }

    @Override
    public TID insert(String tableName, List<Object> values) {
        TableDefinition td = requireTable(tableName);

        List<ColumnDefinition> cols = catalogAccess.listColumnsSorted(td);
        if (values == null || values.size() != cols.size()) {
            throw new IllegalArgumentException("values size mismatch: expected " + cols.size());
        }

        byte[] tuple = serializeRow(cols, values);
        ensureDir(dataRoot);

        int pages = td.pagesCount();

        for (int pid = 0; pid < pages; pid++) {
            BufferSlot slot = bpm.getPage(pid);
            Page p = slot.getPage();

            try {
                int slotId = p.size();
                p.write(tuple);
                bpm.updatePage(pid, p);
                bpm.flushPage(pid);

                TID tid = new TID(pid, slotId);
                if (indexRegistry != null) {
                    indexRegistry.onInsert(tableName, cols, values, tid);
                }
                return tid;
            } catch (IllegalArgumentException ignored) {
            }
        }

        HeapPage np = new HeapPage(pages);
        int slotId = np.size();
        np.write(tuple);

        bpm.updatePage(pages, np);
        bpm.flushPage(pages);

        catalogAccess.updatePagesCount(td.getOid(), pages + 1);

        TID tid = new TID(pages, slotId);
        if (indexRegistry != null) {
            indexRegistry.onInsert(tableName, cols, values, tid);
        }
        return tid;
    }

    @Override
    public List<Object> select(String tableName, List<String> columnNames) {
        TableDefinition td = requireTable(tableName);

        List<ColumnDefinition> allCols = catalogAccess.listColumnsSorted(td);
        List<ColumnDefinition> needCols =
                (columnNames == null || columnNames.isEmpty())
                        ? allCols
                        : mapByNames(allCols, columnNames);

        List<Object> out = new ArrayList<>();
        int pages = td.pagesCount();

        for (int pid = 0; pid < pages; pid++) {
            BufferSlot slot = bpm.getPage(pid);
            Page p = slot.getPage();

            for (int i = 0; i < p.size(); i++) {
                byte[] tuple = p.read(i);

                Map<String, Object> fullRow = deserializeRowToMap(allCols, tuple);

                Map<String, Object> projected = new LinkedHashMap<>();
                for (ColumnDefinition c : needCols) {
                    projected.put(c.name(), fullRow.get(c.name()));
                }

                out.add(projected);
            }
        }

        return out;
    }


    @Override
    public Object selectByTid(String tableName, TID tid) {
        if (tid == null) throw new IllegalArgumentException("tid is null");

        TableDefinition td = requireTable(tableName);
        List<ColumnDefinition> allCols = catalogAccess.listColumnsSorted(td);

        int pageId = tid.pageId();
        int slotId = tid.slotId();

        if (pageId < 0 || pageId >= td.pagesCount()) {
            return null;
        }

        BufferSlot slot = bpm.getPage(pageId);
        Page p = slot.getPage();

        if (slotId < 0 || slotId >= p.size()) {
            return null;
        }

        byte[] tuple = p.read(slotId);
        return deserializeRowToMap(allCols, tuple);
    }

    @Override
    public void createIndex(String indexName, String tableName, String columnName) {
        var table = catalog.getTable(tableName);
        var col = catalog.getColumn(table, columnName);

        int order = 16;

        BPlusTreeIndex index = new BPlusTreeIndexImpl(
                indexName,
                col.name(),
                order,
                pageManagerForIndexes
        );

        indexRegistry.register(table.getName(), col.name(), index);
    }

    private TableDefinition requireTable(String name) {
        TableDefinition td = catalog.getTable(name);
        if (td == null) throw new IllegalArgumentException("table not found: " + name);
        return td;
    }

    private static void ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            throw new IllegalStateException("cannot create dir: " + dir, e);
        }
    }

    private List<ColumnDefinition> mapByNames(List<ColumnDefinition> all, List<String> names) {
        Map<String, ColumnDefinition> byName = all.stream()
                .collect(Collectors.toMap(ColumnDefinition::name, c -> c, (a, b) -> a, LinkedHashMap::new));
        List<ColumnDefinition> out = new ArrayList<>(names.size());
        for (String n : names) {
            ColumnDefinition c = byName.get(n);
            if (c == null) throw new IllegalArgumentException("unknown column: " + n);
            out.add(c);
        }
        return out;
    }

    private byte[] serializeRow(List<ColumnDefinition> cols, List<Object> values) {
        int total = 0;
        byte[][] parts = new byte[cols.size()][];
        for (int i = 0; i < cols.size(); i++) {
            parts[i] = serializeField(cols.get(i), values.get(i));
            total += parts[i].length;
        }
        byte[] buf = new byte[total];
        int off = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, buf, off, part.length);
            off += part.length;
        }
        return buf;
    }

    private Map<String, Object> deserializeRowToMap(List<ColumnDefinition> allCols, byte[] tuple) {
        Map<String, Object> out = new LinkedHashMap<>();
        int off = 0;
        for (ColumnDefinition c : allCols) {
            TypeDefinition t = catalogAccess.getTypeByOid(c.typeOid());
            if ("INT64".equalsIgnoreCase(t.name())) {
                long v = leLong(tuple, off);
                off += 8;
                out.put(c.name(), v);
            } else if (t.name().startsWith("VARCHAR")) {
                int len = tuple[off] & 0xFF;
                off += 1;
                String s = new String(tuple, off, len, StandardCharsets.UTF_8);
                off += len;
                out.put(c.name(), s);
            } else {
                throw new IllegalArgumentException("unsupported type: " + t.name());
            }
        }
        return out;
    }

    private byte[] serializeField(ColumnDefinition c, Object v) {
        TypeDefinition t = catalogAccess.getTypeByOid(c.typeOid());
        if ("INT64".equalsIgnoreCase(t.name())) {
            if (!(v instanceof Long l)) {
                throw new IllegalArgumentException("expected Long for column " + c.name());
            }
            ByteBuffer bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            bb.putLong(l);
            return bb.array();
        } else if (t.name().startsWith("VARCHAR")) {
            if (!(v instanceof String s)) {
                throw new IllegalArgumentException("expected String for column " + c.name());
            }
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            if (b.length > 255) throw new IllegalArgumentException("VARCHAR too long (>255 bytes)");
            byte[] out = new byte[1 + b.length];
            out[0] = (byte) (b.length & 0xFF);
            System.arraycopy(b, 0, out, 1, b.length);
            return out;
        }
        throw new IllegalArgumentException("unsupported type: " + t.name());
    }

    private static long leLong(byte[] a, int off) {
        return ((long) a[off] & 0xFF)
                | (((long) a[off + 1] & 0xFF) << 8)
                | (((long) a[off + 2] & 0xFF) << 16)
                | (((long) a[off + 3] & 0xFF) << 24)
                | (((long) a[off + 4] & 0xFF) << 32)
                | (((long) a[off + 5] & 0xFF) << 40)
                | (((long) a[off + 6] & 0xFF) << 48)
                | (((long) a[off + 7] & 0xFF) << 56);
    }
}