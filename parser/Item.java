package parser;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author JeffreySharp
 * @apiNote Item类 表示LR分析中的每一个项目
 * LR0 production nextSymbol
 * LR1 production nextSymbol lookahead
 */
public class Item {

    //哪个产生式
    private Production production;
    //下一个要被分析的符号
    private Symbol nextSymbol;
    //下一个要被分析的符号占产生式的哪个地方
    private int index;
    //将被用于LR1分析 用于产生式的选择
    private Set<Symbol> lookahead;
    //项目的编号
    private int num;

    public Item(Production production, Symbol nextSymbol, int index, Symbol lookahead, int num) {
        this.production = production;
        this.nextSymbol = nextSymbol;
        this.index = index;
        this.lookahead = new HashSet<>();
        this.lookahead.add(lookahead);
        this.num = num;
    }
    public Item(Production production, Symbol nextSymbol, Symbol lookahead, int num) {
        this.production = production;
        this.nextSymbol = nextSymbol;
        this.lookahead = new HashSet<>();
        this.lookahead.add(lookahead);
        this.num = num;
    }

    public Item(Production production, Symbol nextSymbol, Symbol lookahead) {
        this.production = production;
        this.nextSymbol = nextSymbol;
        this.lookahead = new HashSet<>();
        this.lookahead.add(lookahead);
    }

    public Item(Production production, Symbol nextSymbol) {
        this.production = production;
        this.nextSymbol = nextSymbol;
        this.lookahead = new HashSet<>();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Item(Production production, Symbol nextSymbol, int index) {
        this.production = production;
        this.nextSymbol = nextSymbol;
        this.index = index;
        this.lookahead = new HashSet<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return production.equals(item.production) && Objects.equals(nextSymbol, item.nextSymbol) && Objects.equals(index, item.index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(production, nextSymbol, index);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Item{" + "production=").append(production).append(", nextSymbol=").append(nextSymbol).append(", lookahead = {");
        for(Symbol symbol: lookahead) {
            sb.append(symbol.getValue()).append(" ");
        }
        sb.append("}}");
        return sb.toString();
    }

    public void setProduction(Production production) {
        this.production = production;
    }

    public void setNextSymbol(Symbol nextSymbol) {
        this.nextSymbol = nextSymbol;
    }

    public void setLookahead(Set<Symbol> lookahead) {
        this.lookahead = lookahead;
    }

    public boolean addLookahead(Symbol symbol) {
       return this.lookahead.add(symbol);
    }

    public Production getProduction() {
        return production;
    }

    public Symbol getNextSymbol() {
        return nextSymbol;
    }

    public Set<Symbol> getLookahead() {
        return lookahead;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
