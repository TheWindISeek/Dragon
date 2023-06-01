package parser;

public class Symbol {

    //用于表彰分析已经到了项目的最右端
    public static Symbol RIGHTMOST = new Symbol();
    //用于表彰项目的lookahead 是无关紧要的
    public static Symbol EMPTY = new Symbol();
   public char getValue() {
        return ' ';
    }
}
