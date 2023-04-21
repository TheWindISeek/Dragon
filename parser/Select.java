package parser;

import java.util.HashSet;
import java.util.Set;

public class Select {
    //S->BbS = {c,b}
    private Production left;
    private Set<Terminal> right;

    public Select(Production left) {
        this.left = left;
        this.right = new HashSet<>();
    }

    public Production getLeft() {
        return left;
    }

    public Set<Terminal> getRight() {
        return right;
    }

    public boolean Add(Terminal terminal) {
        return right.add(terminal);
    }

    public boolean Contain(Terminal terminal) {
        return right.contains(terminal);
    }
}
