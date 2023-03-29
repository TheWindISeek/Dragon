package lexer;
/**
 * file format: utf-8
 * @author JeffreySharp
 *
 * Token 用于识别出的语法单元
 * tag 用于显示
 * 当小于255时 tag存储的是	字符对应的ASCII值
 * 大于时 请参考Tag 中的类型
 * */
public class Token {
	public final int tag;
	public Token(int t) { tag = t; }
	public String toString() { return "" + (char)tag; }
}