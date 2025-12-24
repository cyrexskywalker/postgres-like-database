package index;

public interface Index {
    void insert(Comparable key, TID tid);
    String getName();
    IndexType getType();
    String getColumnName();
}