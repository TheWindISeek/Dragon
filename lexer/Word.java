package lexer;

/**
 * file format: utf-8
 *
 * @author JeffreySharp
 * <p>
 * Word 用于 管理 保留字 标识符 复合词法单元(&&)
 * 中间代码的书写形式
 * lexeme 是 语素
 * 会被用于存储上述的形式化描述
 */
public class Word extends Token {
    public String lexeme = "";

    public Word(String s, int tag) {
        super(tag);
        lexeme = s;
    }

    public String toString() {
        return lexeme;
    }

    public static final Word
            and = new Word("&&", Tag.AND), or = new Word("||", Tag.OR),
            eq = new Word("==", Tag.EQ), ne = new Word("!=", Tag.NE),
            le = new Word("<=", Tag.LE), ge = new Word(">=", Tag.GE),
            minus = new Word("minus", Tag.MINUS),
            True = new Word("true", Tag.TRUE),
            False = new Word("false", Tag.FALSE),
            temp = new Word("t", Tag.TEMP);
}