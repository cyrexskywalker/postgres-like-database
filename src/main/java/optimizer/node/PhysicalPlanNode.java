package optimizer.node;


/**
 * Абстрактный класс физического плана.
 * Каждый конкретный PhysicalPlanNode описывает, КАК будет выполняться операция:
 * - какой алгоритм сканирования (SeqScan, IndexScan, ...)
 * - как применяется фильтр (Filter)
 * - какие колонки выбираются (Project)
 */
public abstract class PhysicalPlanNode {

    private String nodeType;

    protected PhysicalPlanNode(String nodeType) {
        this.nodeType = nodeType;
    }
    protected PhysicalPlanNode() {}

    public String getNodeType() {
        return nodeType;
    }

    /**
     * Рекурсивная печать дерева плана — удобно для отладки
     */
    public String prettyPrint(String indent) {
        return indent + nodeType + "\n";
    }

    @Override
    public String toString() {
        return nodeType;
    }
}