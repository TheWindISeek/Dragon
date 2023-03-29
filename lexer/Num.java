package lexer;
/**
 * file format: utf-8
 * @author JeffreySharp
 *
 * Num 整数类型的Token
 * value 存放这个整数的值
 * toString 返回整数的字符串
 * */
public class Num extends Token {
	public final int value;
	public Num(int v) { super(Tag.NUM); value = v; }
	public String toString() { return "" + value; }
}
