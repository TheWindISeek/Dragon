package parser;

import java.util.Objects;

public class Production {
    //left part of production
    private NonTerminal left;
    //right of production
    private String right;
    //num is used for the analysis table
    private int num;

    public Production(NonTerminal left, String right, int num) {
        this.left = left;
        this.right = right;
        this.num = num;
    }

    public int getNum() {
        return num;
    }

    public NonTerminal getLeft() {
        return left;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Production that = (Production) o;
        return num == that.num && left.equals(that.left) && right.equals(that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right, num);
    }

    public String getRight() {
        return right;
    }

    @Override
    public String toString() {
        return left.toString() + "->" + right;
    }
}
