package parser;

import java.util.Objects;

/**
 * @author JeffreySharp
 * @apiNote Item类 表示LR分析中的每一个项目
 */
public class Item {

    //哪个产生式
    private Production production;
    //下一个要被分析的符号
    private Symbol nextSymbol;
    //暂时未使用
    private Symbol lookahead;

    private int num;

    public Item(Production production, Symbol nextSymbol, Symbol lookahead, int num) {
        this.production = production;
        this.nextSymbol = nextSymbol;
        this.lookahead = lookahead;
        this.num = num;
    }

    public Item(Production production, Symbol nextSymbol, Symbol lookahead) {
        this.production = production;
        this.nextSymbol = nextSymbol;
        this.lookahead = lookahead;
    }

    public Item(Production production, Symbol nextSymbol) {
        this.production = production;
        this.nextSymbol = nextSymbol;
    }

    public Item(Production production, Symbol nextSymbol, int num) {
        this.production = production;
        this.nextSymbol = nextSymbol;
        this.num = num;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return production.equals(item.production) && Objects.equals(nextSymbol, item.nextSymbol) && Objects.equals(lookahead, item.lookahead);
    }

    @Override
    public int hashCode() {
        return Objects.hash(production, nextSymbol, lookahead);
    }

    @Override
    public String toString() {
        return "Item{" +
                "production=" + production +
                ", nextSymbol=" + nextSymbol +
                ", lookahead=" + lookahead +
                '}';
    }

    public void setProduction(Production production) {
        this.production = production;
    }

    public void setNextSymbol(Symbol nextSymbol) {
        this.nextSymbol = nextSymbol;
    }

    public void setLookahead(Symbol lookahead) {
        this.lookahead = lookahead;
    }

    public Production getProduction() {
        return production;
    }

    public Symbol getNextSymbol() {
        return nextSymbol;
    }

    public Symbol getLookahead() {
        return lookahead;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
