package index.btree;

import index.IndexType;
import index.TID;
import memory.manager.PageFileManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BPlusTreeIndexImpl implements BPlusTreeIndex {

    private final String indexName;
    private final String columnName;
    private final int order;
    private final PageFileManager pageManager;

    private int rootPageId;
    private int height;

    private final Map<Integer, BPlusTreeNode> nodes = new HashMap<>();
    private int nextPageId = 0;

    public BPlusTreeIndexImpl(String indexName, String columnName, int order, PageFileManager pageManager) {
        this.indexName = indexName;
        this.columnName = columnName;
        this.order = order;
        this.pageManager = pageManager;

        BPlusTreeNode root = new BPlusTreeNode();
        root.pageId = allocatePage();
        root.isLeaf = true;
        root.keys = new Comparable[2 * order];
        root.pointers = new Object[2 * order];
        root.numKeys = 0;

        nodes.put(root.pageId, root);
        rootPageId = root.pageId;
        height = 1;
    }

    @Override
    public void insert(Comparable key, TID tid) {
        if (key == null) throw new IllegalArgumentException("key is null");
        if (tid == null) throw new IllegalArgumentException("tid is null");

        BPlusTreeNode leaf = findLeaf(key);
        insertIntoLeaf(leaf, key, tid);

        if (leaf.numKeys >= 2 * order) {
            split(leaf);
        }
    }

    private void insertIntoLeaf(BPlusTreeNode leaf, Comparable key, TID tid) {
        int i = 0;
        while (i < leaf.numKeys && cmp(leaf.keys[i], key) < 0) {
            i++;
        }

        for (int j = leaf.numKeys; j > i; j--) {
            leaf.keys[j] = leaf.keys[j - 1];
            leaf.pointers[j] = leaf.pointers[j - 1];
        }

        leaf.keys[i] = key;
        leaf.pointers[i] = tid;
        leaf.numKeys++;
    }

    @Override
    public List<TID> search(Comparable key) {
        if (key == null) throw new IllegalArgumentException("key is null");

        List<TID> result = new ArrayList<>();
        BPlusTreeNode leaf = findLeaf(key);

        for (int i = 0; i < leaf.numKeys; i++) {
            if (cmp(leaf.keys[i], key) == 0) {
                result.add((TID) leaf.pointers[i]);
            }
        }
        return result;
    }

    @Override
    public List<TID> rangeSearch(Comparable from, Comparable to, boolean inclusive) {
        List<TID> result = new ArrayList<>();

        if (from != null && to != null && cmp(from, to) > 0) {
            return result;
        }

        BPlusTreeNode node = (from == null) ? leftmostLeaf() : findLeaf(from);

        while (node != null) {
            for (int i = 0; i < node.numKeys; i++) {
                Comparable k = node.keys[i];

                if (from != null) {
                    int cFrom = cmp(k, from);
                    if (cFrom < 0 || (!inclusive && cFrom == 0)) {
                        continue;
                    }
                }

                if (to != null) {
                    int cTo = cmp(k, to);
                    if (cTo > 0 || (!inclusive && cTo == 0)) {
                        return result;
                    }
                }

                result.add((TID) node.pointers[i]);
            }
            node = node.rightSiblingPageId == -1 ? null : readNode(node.rightSiblingPageId);
        }

        return result;
    }

    @Override
    public List<TID> searchGreaterThan(Comparable value, boolean inclusive) {
        if (value == null) throw new IllegalArgumentException("value is null");

        List<TID> result = new ArrayList<>();
        BPlusTreeNode node = findLeaf(value);

        while (node != null) {
            for (int i = 0; i < node.numKeys; i++) {
                Comparable k = node.keys[i];
                int c = cmp(k, value);

                if (c > 0 || (inclusive && c == 0)) {
                    result.add((TID) node.pointers[i]);
                }
            }
            node = node.rightSiblingPageId == -1 ? null : readNode(node.rightSiblingPageId);
        }

        return result;
    }

    @Override
    public List<TID> searchLessThan(Comparable value, boolean inclusive) {
        if (value == null) throw new IllegalArgumentException("value is null");

        List<TID> result = new ArrayList<>();
        BPlusTreeNode node = leftmostLeaf();

        while (node != null) {
            for (int i = 0; i < node.numKeys; i++) {
                Comparable k = node.keys[i];
                int c = cmp(k, value);

                if (c < 0 || (inclusive && c == 0)) {
                    result.add((TID) node.pointers[i]);
                } else {
                    return result;
                }
            }
            node = node.rightSiblingPageId == -1 ? null : readNode(node.rightSiblingPageId);
        }

        return result;
    }

    @Override
    public List<TID> scanAll() {
        List<TID> result = new ArrayList<>();
        BPlusTreeNode node = leftmostLeaf();

        while (node != null) {
            for (int i = 0; i < node.numKeys; i++) {
                result.add((TID) node.pointers[i]);
            }
            node = node.rightSiblingPageId == -1 ? null : readNode(node.rightSiblingPageId);
        }

        return result;
    }

    private void split(BPlusTreeNode node) {
        if (node.isLeaf) splitLeaf(node);
        else splitInternal(node);
    }

    private void splitLeaf(BPlusTreeNode leaf) {
        int mid = order;

        BPlusTreeNode right = new BPlusTreeNode();
        right.pageId = allocatePage();
        right.isLeaf = true;
        right.keys = new Comparable[2 * order];
        right.pointers = new Object[2 * order];

        for (int i = mid; i < leaf.numKeys; i++) {
            right.keys[i - mid] = leaf.keys[i];
            right.pointers[i - mid] = leaf.pointers[i];
            leaf.keys[i] = null;
            leaf.pointers[i] = null;
        }

        right.numKeys = leaf.numKeys - mid;
        leaf.numKeys = mid;

        right.rightSiblingPageId = leaf.rightSiblingPageId;
        right.leftSiblingPageId = leaf.pageId;
        leaf.rightSiblingPageId = right.pageId;

        if (right.rightSiblingPageId != -1) {
            BPlusTreeNode oldRight = readNode(right.rightSiblingPageId);
            if (oldRight != null) oldRight.leftSiblingPageId = right.pageId;
        }

        nodes.put(right.pageId, right);

        Comparable separator = right.keys[0];

        if (leaf.parentPageId == -1) {
            BPlusTreeNode root = new BPlusTreeNode();
            root.pageId = allocatePage();
            root.isLeaf = false;
            root.keys = new Comparable[2 * order];
            root.pointers = new Object[2 * order + 1];

            root.keys[0] = separator;
            root.pointers[0] = leaf.pageId;
            root.pointers[1] = right.pageId;
            root.numKeys = 1;

            leaf.parentPageId = root.pageId;
            right.parentPageId = root.pageId;

            nodes.put(root.pageId, root);
            rootPageId = root.pageId;
            height++;
        } else {
            right.parentPageId = leaf.parentPageId;
            BPlusTreeNode parent = readNode(leaf.parentPageId);
            insertIntoInternal(parent, separator, right.pageId);
        }
    }

    private void splitInternal(BPlusTreeNode node) {
        int mid = order;

        Comparable promote = node.keys[mid];

        BPlusTreeNode right = new BPlusTreeNode();
        right.pageId = allocatePage();
        right.isLeaf = false;
        right.keys = new Comparable[2 * order];
        right.pointers = new Object[2 * order + 1];

        int rightKeys = 0;
        for (int i = mid + 1; i < node.numKeys; i++) {
            right.keys[rightKeys++] = node.keys[i];
            node.keys[i] = null;
        }

        int rightPtrs = 0;
        for (int i = mid + 1; i <= node.numKeys; i++) {
            right.pointers[rightPtrs++] = node.pointers[i];
            node.pointers[i] = null;
        }

        right.numKeys = rightKeys;
        node.numKeys = mid;

        for (int i = 0; i <= right.numKeys; i++) {
            Object ptr = right.pointers[i];
            if (ptr instanceof Integer pid) {
                BPlusTreeNode child = readNode(pid);
                if (child != null) child.parentPageId = right.pageId;
            }
        }

        nodes.put(right.pageId, right);

        if (node.parentPageId == -1) {
            BPlusTreeNode root = new BPlusTreeNode();
            root.pageId = allocatePage();
            root.isLeaf = false;
            root.keys = new Comparable[2 * order];
            root.pointers = new Object[2 * order + 1];

            root.keys[0] = promote;
            root.pointers[0] = node.pageId;
            root.pointers[1] = right.pageId;
            root.numKeys = 1;

            node.parentPageId = root.pageId;
            right.parentPageId = root.pageId;

            nodes.put(root.pageId, root);
            rootPageId = root.pageId;
            height++;
        } else {
            right.parentPageId = node.parentPageId;
            BPlusTreeNode parent = readNode(node.parentPageId);
            insertIntoInternal(parent, promote, right.pageId);
        }
    }

    private void insertIntoInternal(BPlusTreeNode parent, Comparable key, int rightPageId) {
        int i = 0;
        while (i < parent.numKeys && cmp(parent.keys[i], key) < 0) {
            i++;
        }

        for (int j = parent.numKeys; j > i; j--) {
            parent.keys[j] = parent.keys[j - 1];
        }
        for (int j = parent.numKeys + 1; j > i + 1; j--) {
            parent.pointers[j] = parent.pointers[j - 1];
        }

        parent.keys[i] = key;
        parent.pointers[i + 1] = rightPageId;
        parent.numKeys++;

        BPlusTreeNode rightChild = readNode(rightPageId);
        if (rightChild != null) rightChild.parentPageId = parent.pageId;

        if (parent.numKeys >= 2 * order) {
            split(parent);
        }
    }

    private BPlusTreeNode findLeaf(Comparable key) {
        BPlusTreeNode node = readNode(rootPageId);
        while (!node.isLeaf) {
            int i = 0;
            while (i < node.numKeys && cmp(key, node.keys[i]) >= 0) {
                i++;
            }
            node = readNode((int) node.pointers[i]);
        }
        return node;
    }

    private BPlusTreeNode leftmostLeaf() {
        BPlusTreeNode node = readNode(rootPageId);
        while (!node.isLeaf) {
            node = readNode((int) node.pointers[0]);
        }
        return node;
    }

    private int allocatePage() {
        return nextPageId++;
    }

    private BPlusTreeNode readNode(int pageId) {
        return nodes.get(pageId);
    }

    @Override
    public String getName() {
        return indexName;
    }

    @Override
    public String getColumnName() {
        return columnName;
    }

    @Override
    public IndexType getType() {
        return IndexType.BTREE;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static int cmp(Comparable a, Comparable b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }

    private static class BPlusTreeNode {
        int pageId;
        int parentPageId = -1;
        boolean isLeaf;
        int numKeys;
        Comparable[] keys;
        Object[] pointers;
        int leftSiblingPageId = -1;
        int rightSiblingPageId = -1;
    }
}