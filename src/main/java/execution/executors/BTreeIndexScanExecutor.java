package execution.executors;

import catalog.operation.OperationManager;
import index.TID;
import index.btree.BPlusTreeIndex;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class BTreeIndexScanExecutor implements Executor {

    private final OperationManager op;
    private final String tableName;
    private final BPlusTreeIndex index;

    private final Comparable<?> from;
    private final Comparable<?> to;
    private final boolean includeFrom;
    private final boolean includeTo;

    private Iterator<TID> it;

    public BTreeIndexScanExecutor(OperationManager op,
                                  String tableName,
                                  BPlusTreeIndex index,
                                  Comparable<?> from,
                                  Comparable<?> to,
                                  boolean includeFrom,
                                  boolean includeTo) {
        this.op = Objects.requireNonNull(op, "op");
        this.tableName = Objects.requireNonNull(tableName, "tableName");
        this.index = Objects.requireNonNull(index, "index");
        this.from = from;
        this.to = to;
        this.includeFrom = includeFrom;
        this.includeTo = includeTo;
    }

    @Override
    public void open() {
        List<TID> tids;

        if (from == null && to == null) {
            tids = index.scanAll();
        } else if (from != null && to != null && includeFrom && includeTo && Objects.equals(from, to)) {
            tids = index.search(from);
        } else if (from != null && to == null) {
            tids = index.searchGreaterThan(from, includeFrom);
        } else if (from == null) {
            tids = index.searchLessThan(to, includeTo);
        } else {
            if (!includeFrom || !includeTo) {
                tids = index.rangeSearch(from, to, true);
                tids = filterExclusiveBounds(tids);
            } else {
                tids = index.rangeSearch(from, to, true);
            }
        }

        this.it = tids.iterator();
    }

    @Override
    public Object next() {
        if (it == null || !it.hasNext()) return null;
        TID tid = it.next();
        return op.selectByTid(tableName, tid);
    }

    @Override
    public void close() {
        it = null;
    }

    private List<TID> filterExclusiveBounds(List<TID> tids) {
        if (from == null && to == null) return tids;

        return tids.stream().filter(tid -> {
            Object rowObj = op.selectByTid(tableName, tid);
            if (!(rowObj instanceof java.util.Map<?, ?> row)) return false;

            Object v = row.get(index.getColumnName());
            if (!(v instanceof Comparable<?> c)) return false;

            if (from != null) {
                int cmp = cmp((Comparable) c, (Comparable) from);
                if (cmp < 0) return false;
                if (!includeFrom && cmp == 0) return false;
            }
            if (to != null) {
                int cmp = cmp((Comparable) c, (Comparable) to);
                if (cmp > 0) return false;
                if (!includeTo && cmp == 0) return false;
            }
            return true;
        }).toList();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static int cmp(Comparable a, Comparable b) {
        return a.compareTo(b);
    }
}