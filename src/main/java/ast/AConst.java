package ast;

public class AConst extends Expr {
    public Object value;       // значение: число, строка, boolean, null

    public AConst(Object val) {
        this.value = val;
    }

    @Override
    public String toString() {
        return "......";
    }
}