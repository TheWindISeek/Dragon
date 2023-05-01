package parser;

import java.util.HashSet;
import java.util.Set;

public class Follow {
    //left = S; right = #;
    private NonTerminal left;
    private Set<Terminal> right;

    public Follow(NonTerminal left, Terminal right) {
        this.left = left;
        this.right = new HashSet<>();
        this.right.add(right);
    }

    public Follow(NonTerminal left) {
        this.left = left;
        this.right = new HashSet<>();
    }

    public boolean add(Terminal terminal) {
        return right.add(terminal);
    }

    public boolean contain(Terminal terminal) {
        return right.contains(terminal);
    }

    public NonTerminal getLeft() {
        return left;
    }

    public Set<Terminal> getRight() {
        return right;
    }


}
