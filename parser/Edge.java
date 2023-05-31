package parser;

/**
 * @author JeffreySharp
 * @apiNote 用于存储状态转换的DFA中的边
 */
public class Edge {

    //从哪个状态进行转换
    private int from;
    //最终到哪个状态
    private int to;
    //中间需要使用什么符号
    private Symbol symbol;

    public Edge(int from, int to, Symbol symbol) {
        this.from = from;
        this.to = to;
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return "Edge{" +
                "from=" + from +
                ", to=" + to +
                ", symbol=" + symbol +
                '}';
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}
