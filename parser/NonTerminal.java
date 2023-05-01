package parser;

/**
 * @author JeffreySharp
 *
 * Non-Terminal
 */
public class NonTerminal extends Symbol{
    //COMMON used for uniform the operation of follow
    public static final NonTerminal Begin = new NonTerminal((char)1);

    private final char value;


    public NonTerminal(char value) {
        this.value = value;
    }

    public char getValue() {
        return value;
    }

    public int getIntValue() {
        return (int)value;
    }

    //rewrite equals method
    @Override
    public boolean equals(Object obj) {
        return this.value == ((NonTerminal)obj).value;
    }

    @Override
    public String toString() {
        return ""+value;
    }
}
