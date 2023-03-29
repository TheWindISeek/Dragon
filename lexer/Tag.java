package lexer;
/**
 * file format: utf-8
 * @author JeffreySharp
 *
 * Tag 定义了各个词法单元对应的常量
 * */
public class Tag {
	public final static int 
		AND = 256, BASIC = 257, BREAK = 258, DO = 259, ELSE = 260,
		EQ = 261, FALSE = 262, GE = 263, ID = 264, IF = 265,
		INDEX = 266, LE = 267, MINUS = 268, NE = 269, NUM = 270,
		OR = 271, REAL = 272, TEMP = 273, TRUE = 274, WHILE = 275;

	//从256开始
	public final static String[] tags = {
			"AND", "BASIC", "BREAK", "DO", "ELSE",
			"EQ", "FALSE", "GE", "ID", "IF",
			"INDEX", "LE", "MINUS", "NE", "NUM",
			"OR", "REAL", "TEMP", "TRUE", "WHILE"
	};

	public static String getTag(int tag) throws IndexOutOfBoundsException{
		if(tag <= 255)
 			return "" + (char)tag;
		 else//数组越界的异常
			return tags[tag-256];
	}
}
