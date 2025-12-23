package execution.executors;

import catalog.table.Table;
import index.TID;
import index.btree.BPlusTreeIndex;

import java.util.Iterator;
import java.util.List;

public class BTreeIndexScanExecutor implements Executor {

    private final BPlusTreeIndex index;
    private final Comparable<?> rangeFrom;  // нижняя граница (может быть null)
    private final Comparable<?> rangeTo;    // верхняя граница (может быть null)
    private final Table table;

    private Iterator<TID> tidIterator;
    private boolean isOpen;

    /**
     * конструктор для range-запроса.
     * используется для BETWEEN, >, <, >=, <=
     */
    public BTreeIndexScanExecutor(BPlusTreeIndex index,
                                  Comparable<?> rangeFrom,
                                  Comparable<?> rangeTo,
                                  Table table) {
        this.index = index;
        this.rangeFrom = rangeFrom;
        this.rangeTo = rangeTo;
        this.table = table;
    }

    /**
     * конструктор для поиска по равенству.
     * эквивалентно rangeSearch(key, key).
     */
    public BTreeIndexScanExecutor(BPlusTreeIndex index,
                                  Comparable<?> value,
                                  Table table) {
        this(index, value, value, table);
    }

    @Override
    public void open() {
        List<TID> tids;

        if (rangeFrom != null || rangeTo != null) {
            tids = index.rangeSearch(rangeFrom, rangeTo, true);
        } else {
            tids = index.scanAll();
        }

        this.tidIterator = tids.iterator();
        this.isOpen = true;
    }

    @Override
    public Object next() {
        if (!isOpen || !tidIterator.hasNext()) {
            return null;
        }

        TID tid = tidIterator.next();
        return table.read(tid);
    }

    @Override
    public void close() {
        tidIterator = null;
        isOpen = false;
    }
}