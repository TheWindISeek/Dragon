package symbols;

import lexer.*;
/**
 *	file format: utf-8
 * @author JeffreySharp
 *
 * Type 用于 强类型中的各种类型的识别与显示
 * 这里给出了int, float, char, bool
 * 将这几种类型都使用 static  制作了 静态对象
 * 同时numeric 实习了数值类型 (int float char)的判断
 * max 用于类型提升 当无法转换时返回 null
 * */
public class Type extends Word {
	public int width = 0;
	/**
	 * s -> 用于toString
	 * tag -> 哪个语言类型
	 * w -> width 当前类型的宽度
	 * */
	public Type(String s, int tag, int w) {
		super(s, tag);
		width = w;
	}

	public static final Type
		Int = new Type("int", Tag.BASIC, 4),
		Float = new Type("float", Tag.BASIC, 8),
		Char = new Type("char", Tag.BASIC, 1),
		Bool = new Type("bool", Tag.BASIC, 1);

	public static boolean numeric(Type p) {
		if(p == Type.Char || p == Type.Int || p == Type.Float)
			return true;
		else
			return false;
	}

	public static Type max(Type p1, Type p2) {
		if(!numeric(p1) || !numeric(p2))
			return null;
		else if (p1 == Type.Float || p2 == Type.Float)
			return Type.Float;
		else if (p1 == Type.Int || p2 == Type.Int)
			return Type.Int;
		else
			return Type.Char;
	}
}