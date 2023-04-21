package parser;

public class Follow {
    //left = S; right = #;
    private NonTerminal left;
    private Terminal right;

    public Follow(NonTerminal left, Terminal right) {
        this.left = left;
        this.right = right;
    }

    public NonTerminal getLeft() {
        return left;
    }

    public Terminal getRight() {
        return right;
    }
}
