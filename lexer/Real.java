package lexer;

/**
 * file format: utf-8
 *
 * @author JeffreySharp
 *
 * Real 用于存储 实数类型的Token
 * value 存放的是float值
 * toString 返回的是 浮点值
 */
public class Real extends Token {
    public final float value;

    public Real(float v) {
        super(Tag.REAL);
        value = v;
    }

    public String toString() {
        return "" + value;
    }
}