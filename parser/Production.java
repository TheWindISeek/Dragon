package parser;

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

    public String getRight() {
        return right;
    }
}
