package catalog.manager;

import catalog.model.ColumnDefinition;
import catalog.model.TableDefinition;
import catalog.model.TypeDefinition;
import memory.buffer.BufferPoolManager;
import memory.model.BufferSlot;
import memory.page.HeapPage;
import memory.page.Page;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class DefaultCatalogManager implements CatalogManager {

    final int PAGE_SIZE = 8192;
    private enum Kind { TABLE, COLUMN, TYPE }

    private static final String TABLES_FILE  = "table_definitions.dat";
    private static final String COLUMNS_FILE = "column_definitions.dat";
    private static final String TYPES_FILE   = "types_definitions.dat";

    private final Path root;
    private final BufferPoolManager bpm;

    private final Map<Integer, TableDefinition> tablesByOid = new HashMap<>();
    private final Map<String,  TableDefinition> tablesByName = new HashMap<>();
    private final Map<Integer, List<ColumnDefinition>> columnsByTableOid = new HashMap<>();
    private final Map<Integer, TypeDefinition>  typesByOid = new HashMap<>();
    private final Map<String,  TypeDefinition>  typesByName = new HashMap<>();

    private final AtomicInteger nextTableOid  = new AtomicInteger(1);
    private final AtomicInteger nextColumnOid = new AtomicInteger(1);
    private final AtomicInteger nextTypeOid   = new AtomicInteger(1);

    public DefaultCatalogManager(Path root, BufferPoolManager bpm) throws IOException {
        this.root = Objects.requireNonNull(root, "root");
        this.bpm  = Objects.requireNonNull(bpm, "bpm");
        Files.createDirectories(root);

        ensureCatalogFiles();

        loadDefinitions(TYPES_FILE, Kind.TYPE);
        if (typesByOid.isEmpty()) {
            bootstrapBuiltinTypes();
            persistBuiltinTypes();
        }

        loadDefinitions(TABLES_FILE, Kind.TABLE);
        loadDefinitions(COLUMNS_FILE, Kind.COLUMN);
    }

    @Override
    public synchronized TableDefinition createTable(String name, List<ColumnDefinition> columns) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("table name is empty");
        if (tablesByName.containsKey(name))
            throw new IllegalArgumentException("table already exists: " + name);
        Objects.requireNonNull(columns, "columns");
        if (columns.isEmpty())
            throw new IllegalArgumentException("table must have at least one column");

        int tableOid = nextTableOid.getAndIncrement();
        String fileNode = tableOid + ".dat";

        TableDefinition td = new TableDefinition(
                tableOid,
                name,
                "TABLE",
                fileNode,
                0
        );

        List<ColumnDefinition> cols = new ArrayList<>(columns.size());
        for (int i = 0; i < columns.size(); i++) {
            ColumnDefinition c0 = columns.get(i);
            int colOid = nextColumnOid.getAndIncrement();
            ColumnDefinition c = new ColumnDefinition(
                    colOid,
                    tableOid,
                    c0.typeOid(),
                    c0.name(),
                    i
            );
            cols.add(c);
        }

        appendRecord(root.resolve(TABLES_FILE),  td.toBytes());
        for (ColumnDefinition c : cols) {
            appendRecord(root.resolve(COLUMNS_FILE), c.toBytes());
        }

        Path dataFile = root.resolve(fileNode);
        try {
            if (!Files.exists(dataFile)) {
                Files.createFile(dataFile);
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to create data file: " + dataFile, e);
        }

        tablesByOid.put(td.oid(), td);
        tablesByName.put(td.name(), td);
        cols.sort(Comparator.comparingInt(ColumnDefinition::position));
        columnsByTableOid.put(td.oid(), cols);

        return td;
    }

    @Override
    public synchronized TableDefinition getTable(String tableName) {
        TableDefinition td = tablesByName.get(tableName);
        if (td == null) throw new IllegalArgumentException("no such table: " + tableName);
        return td;
    }

    @Override
    public synchronized ColumnDefinition getColumn(TableDefinition table, String columnName) {
        List<ColumnDefinition> cols = columnsByTableOid.getOrDefault(table.oid(), List.of());
        for (ColumnDefinition c : cols) {
            if (c.name().equals(columnName)) return c;
        }
        throw new IllegalArgumentException("no such column: " + columnName + " in table " + table.name());
    }

    @Override
    public synchronized List<TableDefinition> listTables() {
        List<TableDefinition> out = new ArrayList<>(tablesByOid.values());
        out.sort(Comparator.comparing(TableDefinition::oid));
        return out;
    }

    private void ensureCatalogFiles() throws IOException {
        for (String f : List.of(TABLES_FILE, COLUMNS_FILE, TYPES_FILE)) {
            Path p = root.resolve(f);
            if (!Files.exists(p)) Files.createFile(p);
        }
    }

    private void bootstrapBuiltinTypes() {
        TypeDefinition int64 = new TypeDefinition(nextTypeOid.getAndIncrement(), "INT64", 8);
        TypeDefinition varchar255 = new TypeDefinition(nextTypeOid.getAndIncrement(), "VARCHAR_255", -1);

        typesByOid.put(int64.oid(), int64);
        typesByName.put(int64.name(), int64);

        typesByOid.put(varchar255.oid(), varchar255);
        typesByName.put(varchar255.name(), varchar255);
    }

    private void persistBuiltinTypes() throws IOException {
        Path file = root.resolve(TYPES_FILE);
        for (TypeDefinition t : typesByOid.values()) {
            appendRecord(file, t.toBytes());
        }
    }

    private void loadDefinitions(String fileName, Kind kind) throws IOException {
        Path file = root.resolve(fileName);
        if (!Files.exists(file)) return;

        long size = Files.size(file);
        if (size == 0) return;
        if (size % PAGE_SIZE != 0) {
            throw new IllegalStateException(
                    "corrupted catalog file (size % page != 0): " + file
            );
        }

        int pages = (int) (size / PAGE_SIZE);

        int maxTable = 0, maxColumn = 0, maxType = 0;

        for (int pid = 0; pid < pages; pid++) {

            BufferSlot slot = bpm.getPage(pid);
            Page p = slot.getPage();

            if (!p.isValid()) {
                throw new IllegalStateException(
                        "invalid page signature in " + file + ", pageId=" + pid
                );
            }

            for (int i = 0; i < p.size(); i++) {
                byte[] rec = p.read(i);

                switch (kind) {
                    case TABLE -> {
                        TableDefinition td = TableDefinition.fromBytes(rec);
                        tablesByOid.put(td.oid(), td);
                        tablesByName.put(td.name(), td);
                        if (td.oid() > maxTable) maxTable = td.oid();
                    }
                    case COLUMN -> {
                        ColumnDefinition cd = ColumnDefinition.fromBytes(rec);
                        columnsByTableOid
                                .computeIfAbsent(cd.tableOid(), k -> new ArrayList<>())
                                .add(cd);
                        if (cd.oid() > maxColumn) maxColumn = cd.oid();
                    }
                    case TYPE -> {
                        TypeDefinition ty = TypeDefinition.fromBytes(rec);
                        typesByOid.put(ty.oid(), ty);
                        typesByName.put(ty.name(), ty);
                        if (ty.oid() > maxType) maxType = ty.oid();
                    }
                }
            }
        }

        switch (kind) {
            case TABLE  -> nextTableOid.set(maxTable + 1);
            case COLUMN -> nextColumnOid.set(maxColumn + 1);
            case TYPE   -> nextTypeOid.set(maxType + 1);
        }

        if (kind == Kind.COLUMN) {
            for (List<ColumnDefinition> list : columnsByTableOid.values()) {
                list.sort(Comparator.comparingInt(ColumnDefinition::position));
            }
        }
    }

    private void appendRecord(Path file, byte[] rec) {
        try {
            if (!Files.exists(file)) {
                Path parent = file.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.createFile(file);
            }

            long fileSize = Files.size(file);
            int lastPageId = (fileSize == 0) ? -1 : (int) (fileSize / PAGE_SIZE) - 1;

            if (lastPageId < 0) {
                HeapPage page = new HeapPage(0);
                if (!tryWrite(page, rec)) {
                    throw new IllegalArgumentException(
                            "record too large for empty page: " + rec.length
                    );
                }
                bpm.updatePage(0, page);
                bpm.flushPage(0);
                return;
            }

            BufferSlot tailSlot = bpm.getPage(lastPageId);
            Page tail = tailSlot.getPage();

            if (!(tail instanceof HeapPage hp)) {
                HeapPage newPage = new HeapPage(lastPageId + 1);
                if (!tryWrite(newPage, rec)) {
                    throw new IllegalArgumentException(
                            "record too large (" + rec.length + " bytes)"
                    );
                }
                bpm.updatePage(lastPageId + 1, newPage);
                bpm.flushPage(lastPageId + 1);
                return;
            }

            if (tryWrite(hp, rec)) {
                bpm.updatePage(lastPageId, hp);
                bpm.flushPage(lastPageId);
            } else {
                HeapPage newPage = new HeapPage(lastPageId + 1);
                if (!tryWrite(newPage, rec)) {
                    throw new IllegalArgumentException(
                            "record too large for page (" + rec.length + " bytes)"
                    );
                }
                bpm.updatePage(lastPageId + 1, newPage);
                bpm.flushPage(lastPageId + 1);
            }

        } catch (IOException e) {
            throw new RuntimeException("appendRecord I/O error: " + file, e);
        }
    }

    private boolean tryWrite(HeapPage page, byte[] rec) {
        try {
            page.write(rec);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}