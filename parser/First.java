package parser;

import java.util.HashSet;
import java.util.Set;

public class First {
    private String left;
    private Set<Terminal> right;

    //we only know left
    public First(String left) {
        this.left = left;
        this.right = new HashSet<>();
    }

    //I don't know this is  deep versus shallow copy.
    public First(String left, Set<Terminal> right) {
        this.left = left;
        this.right = right;
    }

    public boolean Add(Terminal terminal) {
        return right.add(terminal);
    }

    public boolean Contain(Terminal terminal) {
        return right.contains(terminal);
    }
}
