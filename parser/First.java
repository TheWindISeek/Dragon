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

    public boolean add(Terminal terminal) {
        return right.add(terminal);
    }

    public boolean contain(Terminal terminal) {
        return right.contains(terminal);
    }

    public Set<Terminal> getRight() {
        return right;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("First{" + "left='").append(left).append('\'').append(", right={");
        for(Terminal terminal: right) {
            sb.append(terminal.getValue());
        }
        sb.append("}}");
        return sb.toString();
    }

    public String getLeft() {
        return left;
    }
}
