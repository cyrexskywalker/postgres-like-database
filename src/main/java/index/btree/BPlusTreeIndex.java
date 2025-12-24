package index.btree;

import index.Index;
import index.TID;

import java.util.List;

public interface BPlusTreeIndex extends Index {
    List<TID> search(Comparable key);
    List<TID> rangeSearch(Comparable from, Comparable to, boolean inclusive);
    List<TID> searchGreaterThan(Comparable value, boolean inclusive);
    List<TID> searchLessThan(Comparable value, boolean inclusive);
    List<TID> scanAll();
    int getHeight();
    int getOrder();
}