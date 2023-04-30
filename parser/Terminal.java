package parser;

/**
 * @author JeffreySharp
 *
 * Terminal
 */
public class Terminal extends Symbol{
    //special two terminal -> sigma and end
    public static final Terminal Empty = new Terminal('$');
    public static final Terminal End = new Terminal('#');

    private final char value;

    public Terminal(char value) {
        this.value = value;
    }

    /**
     * get char type of Terminal
     * @return
     */
    public char getValue() {
        return value;
    }

    /**
     * get int type of Terminal
     * @return
     */
    public int getIntValue() {
        return (int)value;
    }

    @Override
    public boolean equals(Object obj) {
        //System.out.println(this.value + " equals " + ((Terminal)obj).value);
        return this.value == ((Terminal)obj).value;
    }
}
