package parser;

import java.util.*;

/**
 * @author JeffreySharp
 * @apiNote LR1分析程序的构建
 * 这个程序还存在一个很严重的问题
 * 就是在goto的时候
 * 有一个lookahead的转移
 * 实际上这个转移的意思是只把之前的转移过去
 * 但是实际上我转移的时候，把其他项目中这个产生式的lookahead也转移过去了
 * 所以会出现错误的情况
 *
 * 要解决这个严重的问题 需要做以下修改
 * 首先我需要在goto的时候新建一个对象
 * 接着每次只把该复制的给到他
 * 然后进行添加
 * 但是这样会影响我之后的识别判断
 * 因为我比较是直接比较的是是否含有这个对象本身 而不是这个对象的内容
 * 然后就是我的哈希函数也得进行修改
 * 说实话 有点麻烦了
 * 因此我决定不改了
 * 就这样吧
 *
 * 这个文件从LR0代码复制而来
 * 接下来将按照LR1的方式进行修改
 *
 * Closure(I) =
 *      repeat
 *          for I 中的 任意项 A->alpha . X beta , z
 *              for 任意产生式 X -> γ
 *                  for 任意w 属于first(beta z)
 *                      I = I + {X-> . γ, w}
 *       until I 没有改变
 *       return I
 *
 *
 *
 * Goto(I, X) =
 *      J <- empty
 *      for I 中的任意项 A -> alpha . X beta , z
 *          将(A -> alpha X . beta, z) 加入到J中
 *      return Closure(J)
 *
 *
 * construct() =
 *
 */
public class LALR1 {
    Map<Integer, Set<Item>> states = new HashMap<>();//状态的编号 -> 状态编号里面的项目
    List<Set<Item>> setItemList = new ArrayList<>();//这里存放所有的set<Item>
    List<Edge> edgeList = new ArrayList<>();//这里存放所有的edge
    Map<Integer, Item> itemMap = new HashMap<>();//用于存放所有的item 便于之后的管理

    Set<Edge> edgeSet = new HashSet<>();//边的集合
    NonTerminal beginSymbol = NonTerminal.Begin;//开始符号 用于做增广文法
    Map<Integer, Production> productions = new HashMap<>();//产生式int => production

    Set<Terminal> terminals = new HashSet<>();//所有的终结符

    Set<NonTerminal> nonTerminals = new HashSet<>();//所有的非终结符号

    List<First> firsts = new ArrayList<>();//所有的 firsts 集合

    Map<Symbol, First> firstMap = new HashMap<>();//终结符和所有非终结符号对应的 first

    Map<String, First> stringFirstMap = new HashMap<>();//产生式的一部分对应first string => first

    int beginState = 1; //状态默认从1开始 由于起始状态必为1 故可这么操作

    Map<Integer, Entry> tables = new HashMap<>();//分析表

    //cpoy set from "from" to "to"
    private <T>void copySet(Set<T> to, Set<T> from) {
        for (T item :from) {
            to.add(item);
        }
    }
    //copy set from "from" to "to", use isChanged to adjust whether set is altered or not.
    private <T>boolean copySet(Set<T> to, Set<T> from, boolean isChanged) {
        for (T item :from) {
            isChanged |= to.add(item);
        }
        return isChanged;
    }

    /**
     * gene first set from productions
     */
    private void geneFirst() {
        //all terminals's first = {terminal}
        for(Terminal t: terminals) {
            First first = new First(t.toString());
            first.add(t);

            firsts.add(first);
            firstMap.put(t, first);
        }

        //get all first(X)
        for(NonTerminal nt: nonTerminals) {
            firstMap.put(nt, new First(nt.toString()));
        }

        for(Production production: productions.values()) {
            //if X -> a...
            Symbol symbol = getSymbols(production.getRight().charAt(0));
            if(symbol instanceof Terminal) {
                First first = firstMap.get(production.getLeft());
                first.add((Terminal) symbol);

                //if right part only have one symbols which is terminal, then we should skip this case.
                if(production.getRight().length() == 1) continue;
                //right part also should be added to first list
                First rightFirst = new First(production.getRight());

                rightFirst.add((Terminal) symbol);
                stringFirstMap.put(production.getRight(), rightFirst);
            }
        }

        //adjust whether set is changed or not.
        boolean isChanged = true;
        //生成产生式右部的first 产生式右部的全部而不是只是一部分 因为生成的是产生式右部的代码 所以每次我都只需要先生成产生式右部的first 然后将这个first添加到产生式左部的first集合中去
        while (isChanged){
            isChanged = false;
            for(Production production: productions.values()) {
                //if X -> Y1 Y2 Y3 ....
                int index = 0;
                Symbol symbol = getSymbols(production.getRight().charAt(index));

                if(symbol instanceof NonTerminal) {
                    First X = stringFirstMap.get(production.getRight());
                    if(X == null) {
                        System.out.println("这是右边的字符串" + production.getRight());
                        if(production.getRight().length() == 1)
                            X = firstMap.get(symbol);//假设右边只有一个字符 那么我们获取的就是一个非终结符号
                        else
                            X = new First(production.getRight());
                    }
                    First Y = firstMap.get((NonTerminal) symbol);

                    boolean end = false;
                    for(Terminal t:Y.getRight()) {
                        if(t.equals(Terminal.Empty)) {
                            end = true;
                            continue;
                        }
                        isChanged |= X.add(t);
                    }

                    while (end) {
                        //find all non-terminal
                        if(index + 1 == production.getRight().length()) {
                            break;
                        }
                        symbol = getSymbols(production.getRight().charAt(++index));
                        //the terminal should be included in the first
                        if(symbol instanceof Terminal) {
                            isChanged |= X.add((Terminal) symbol);
                            end = false;
                            break;
                        }
                        Y = firstMap.get((NonTerminal) symbol);
                        end = false;
                        for (Terminal t: Y.getRight()) {
                            if(t.equals(Terminal.Empty)) {
                                end = true;
                                continue;
                            }
                            isChanged |= X.add(t);
                        }
                    }

                    //all Y have sigma
                    if(end) {
                        isChanged |= X.add(Terminal.Empty);
                    }
                    //right part which is Y1Y2Y3...
                    if (production.getRight().length() > 1) // 只有当产生式右部的长度超过1 的时候 在让产生式的右边与X相等
                        stringFirstMap.put(production.getRight(), X);
                    //left part which is X
                    First first = firstMap.get(production.getLeft());
                    copySet(first.getRight(), X.getRight(), isChanged);
                }
            }
        }

        //现在我需要计算产生式右部一部分的first集合 该如何计算呢


        //add all first to firsts
        for(Map.Entry<Symbol, First> entry: firstMap.entrySet()) {
            if(entry.getKey() instanceof NonTerminal)
                firsts.add(entry.getValue());
        }
        for(Map.Entry<String, First> entry: stringFirstMap.entrySet()) {
            firsts.add(entry.getValue());
        }
    }

    private First geneFirst(String right) {
        First firstList = new First(right);
        int index = 0;
        char c = right.charAt(index);
        Symbol symbol = getSymbols(c);

        boolean end = false;
        for(Terminal t: firstMap.get(symbol).getRight()) {
            if(t.equals(Terminal.Empty)) {
                end = true;
                continue;
            }
            firstList.add(t);
        }

        while (end) {
            //find all non-terminal
            if(index + 1 == right.length()) {
                break;
            }
            symbol = getSymbols(right.charAt(++index));
            //the terminal should be included in the first
            if(symbol instanceof Terminal) {
                firstList.add((Terminal) symbol);
                end = false;
                break;
            }

            end = false;
            for (Terminal t: firstMap.get((NonTerminal) symbol).getRight()) {
                if(t.equals(Terminal.Empty)) {
                    end = true;
                    continue;
                }
                firstList.add(t);
            }
        }

        //all Y have sigma
        if(end) {
            firstList.add(Terminal.Empty);
        }

        System.out.println("右部残缺的first list" + firstList);
        return firstList;
    }

    //get symbol from char
    private Symbol getSymbols(char c) {
        for (Terminal terminal : terminals) {
            if (terminal.getValue() == c) {
                return terminal;
            }
        }

        for (NonTerminal nonTerminal : nonTerminals) {
            if (nonTerminal.getValue() == c) {
                return nonTerminal;
            }
        }
        return Terminal.End;
    }

    /**
     * generate terminals, nontermianls, productions using grammar
     * which is
     * String grammar = "S AaS\nS BbS\nS d\nA a\nB $\nB c";
     *
     * @param grammar
     */
    private void geneProduction(String grammar) {
        String[] splits = grammar.split("\n");

        StringBuffer symbols = new StringBuffer();
        int index = 0;

        for (String s_production : splits) {
            String[] strings = s_production.split(" ");

            //0->NonTermianl 1->right
            String left = strings[0];
            String right = strings[1];

            //add nonterminal and production to set
            Symbol symbol = getSymbols(left.charAt(0));

            //already have
            if (!(symbol instanceof NonTerminal)) {
                symbol = new NonTerminal(left.charAt(0));
                if (nonTerminals.size() == 0) {//这里是否还需要这样做就不一定了
                    //制作一个增广文法 beginSymbol -> left
                    Production production = new Production(beginSymbol, left+"#", index++);
                    productions.put(production.getNum(), production);

                    //初始状态
                    Set<Item> items = new HashSet<>();
                    Item item = new Item(production, symbol, Symbol.EMPTY);
                    items.add(item);//这里初次添加项目项目集合
                    setItemList.add(items);
                    int key = Objects.hash(production, symbol, Symbol.EMPTY);
                    itemMap.put(key, item);
                    states.put(beginState, items);
                }
                nonTerminals.add((NonTerminal) symbol);
            }

            //add production and nonterminal
            Production production = new Production((NonTerminal) symbol, right, index++);
//            productions.add(production);
            productions.put(production.getNum(), production);
            symbols.append(right);
            nonTerminals.add(beginSymbol);//将增广文法的符号也添加进去
        }

        //add terminal
        for (int i = 0; i < symbols.length(); i++) {
            char c = symbols.charAt(i);
            Symbol symbol = getSymbols(c);

            //we finally process sigma
            if (symbol instanceof NonTerminal) continue;
            if (c == '$') continue;

            //if not found
            if (symbol.equals(Terminal.End)) {
                terminals.add(new Terminal(c));
            }
        }

        //add end and empty, while as empty maybe have been added.
        terminals.add(Terminal.Empty);
        terminals.add(Terminal.End);
    }
    /**
     * 根据产生式生成所有的items 方便后续set的判断
     * 生成item的时候可能更加需要将所有可能的情况都给包括进去  包括lookahead的所有情况
     */
    private void geneItems() {
        int index, sz, hashCode;
        Item item;
        for (Production production : productions.values()) {
            index = 0;
            sz = production.getRight().length();
            while (index < sz) { //其实在这里我生成所有的式子的时候 直接把所有的都生成了
                    //根据 产生式    当前将要识别的符号  index
                    hashCode = Objects.hash(production, getSymbols(production.getRight().charAt(index)), index);

                    item = new Item(production, getSymbols(production.getRight().charAt(index)), index);//新建一个项目的时候记录他跑到了哪个index
                    itemMap.put(hashCode, item);

                    System.out.printf("%-10d\t %s \n", hashCode, item);
                index++;
            }

            //最右端先给写上
            hashCode = Objects.hash(production, Symbol.RIGHTMOST, production.getRight().length());
            item = new Item(production, Symbol.RIGHTMOST, Symbol.EMPTY, production.getRight().length());//lookahead为空
            itemMap.put(hashCode, item);

            System.out.printf("%-10d\t %s \n", hashCode, item);
        }
    }

    /**
     * 求状态I的闭包 LR1版本
     * @param I 状态I 其中包含项目
     * @return closure(I)
     * for I 中的任意项 A->alpha. X beta, z
     *      for 任意产生式 X -> γ
     *          关键在于这里的 FIRST(beta z)
     *          for 任意 w 属于 FIRST(beta z)
     *              I <= I + {(X => . γ, w )}
     */
    public Set<Item> Closure(Set<Item> I) {
        boolean isChanged = true;
        List<Item> list = new ArrayList<>();
        while (isChanged) {
            isChanged = false;
            list.clear();
            for (Item item : I) { // A-> alpha .X beta, z
                if (item.getNextSymbol().equals(Symbol.RIGHTMOST)) continue;//下一个符号是文法末尾 不需要求解
                if (item.getNextSymbol() instanceof Terminal) continue;//下一个符号是终结符号 不可能X->γ

                NonTerminal nextSymbol = (NonTerminal) item.getNextSymbol();//获得下一个符号
                String productionRight = item.getProduction().getRight();
                for (Production production : productions.values()) { //对于所有的产生式
                    if (nextSymbol.equals(production.getLeft())) {//找到产生式 X-> γ 有个问题 lookahead符号是不是可以有多个


                        //index -> X的位置 如果index+1 == length 那么说明x后面没有字符了
                        //四种情况 beta 为空 z 为空 也就是  =>  index == getRight().length()   && z == Symbol.EMPTY
                        //如果 beta为空 z不为空  那么就直接看 first(z)
                        //如果 beta不为空 那当z为空的时候 看 first(beta)
                        //如果 beta 不为空 那么当z不为空的时候 看first(beta z)

                        //重新梳理一下 我新建item的时候并没有为他新建 lookahead 当然这个项目可能仍然会在之前被查到
                        //因为lookahead里面可能会有许多项 实际对其进行考虑的时候 我是都要进行考虑的

//                        String productionRight = production.getRight();//当前项目的产生式右部的字符串
                        Set<Symbol> lookahead = item.getLookahead();//当前项目的lookahead是谁
                        Symbol newNextSymbol = getSymbols(production.getRight().charAt(0));//X -> γ 中的γ
                        int index = item.getIndex();//当前项目分析到了产生式的哪个位置


                        boolean isBetaEmpty = index + 1 == productionRight.length();
                        boolean isLookAheadEmpty = lookahead.size() == 1 && lookahead.contains(Symbol.EMPTY);

                        System.out.println("当前产生式\t" + productionRight + "当前下标\t" + index + "当前是否到了最右边\t" + isLookAheadEmpty);

                        int hashCode = Objects.hash(production, newNextSymbol, 0);//新加下一个项目 当前产生式X -> γ  γ 分析到的位置
                        Item tempItem = itemMap.get(hashCode);

                        if(isBetaEmpty && isLookAheadEmpty) { // 两者都为空

                            System.out.println("beta为空 右边也扫描到了最右端");//应该不会出现这种情况不过以往万一

                        } else if (isBetaEmpty && !isLookAheadEmpty) { // 扫描到最后了 但是还可以看lookahead
                            System.out.println("isBetaEmpty and !isLookAheadEmpty" + lookahead);

                            //对于所有可能的symbol 我们去找他里面符号的first
                            for(Symbol symbol: lookahead) {
                                for(Terminal t: firstMap.get(symbol).getRight()) {
                                    tempItem.addLookahead(t);
                                }
                            }
                        } else if(!isBetaEmpty && isLookAheadEmpty) { // 没扫描打最后 z为空

                            //获取右端字符串
                            String restString = productionRight.substring(index+1);
                            System.out.println("剩余的字符串" + restString);
                            //获取右端字符串
                            for(Terminal terminal: geneFirst(restString).getRight()) {
                                tempItem.addLookahead(terminal);
                            }

                        } else { // 两者都不为空
                            //获取右端字符串
                            String restString = productionRight.substring(index+1);
                            System.out.println("剩余的字符串" + restString);//也就是我这里其实没有为他生成对应的first语句
                            //获取右端字符串
                            for(Terminal terminal: geneFirst(restString).getRight()) {
                                if(terminal.equals(Terminal.Empty)) {//如果first里面有空 就将first(z)也加入到其中
                                    for(Symbol symbol: lookahead) {
                                        for(Terminal t: firstMap.get(symbol).getRight()) {
                                            tempItem.addLookahead(t);
                                        }
                                    }
                                    continue;
                                }
                                tempItem.addLookahead(terminal);
                            }
                        }

                        System.out.println(tempItem);
                        list.add(tempItem);//将这个新加的项目添加到添加列表中
                    }
                }
            }
            for (Item item : list) {
                isChanged |= I.add(item);//判断是否发生了改变
            }
        }
        return I;
    }

    /**
     * goto(I,X)
     *
     * @param I 项目集合
     * @param X 文法符号 终结符号或者非终结符号
     * @return 将圆点移动到所有项的符号X之后
     */
    public Set<Item> Goto(Set<Item> I, Symbol X) {
        System.out.println("\n\n\n进入goto函数 给定的X是"+ X.getValue());
        print(I);

        Set<Item> J = new HashSet<>();
        if (X.equals(Symbol.RIGHTMOST)) return J;//如果都扫描到最右边了 那么很显然走不下去了
        for (Item item : I) {
            //如果可以跳到下一个状态
            if (item.getNextSymbol().equals(X)) {
                if (X.equals(Terminal.End)) continue;
                String s = item.getProduction().getRight();
                int index = item.getIndex()+1;
                Symbol c = index == s.length() ? Symbol.RIGHTMOST : getSymbols(s.charAt(index));
                int hash = Objects.hash(item.getProduction(), c, index);
                Item item1 = itemMap.get(hash);
                for(Symbol symbol: item.getLookahead()) {
                    item1.addLookahead(symbol);
                }
                System.out.println("当前状态下又添加了一个" + item1);
                J.add(itemMap.get(hash));
            }
        }


        System.out.println("计算完可到达的J");
        print(J);
        System.out.println("\n\n\n");
        return Closure(J);
    }

    /**
     * 打印状态中的所有项目
     *
     * @param I 给定的状态
     */
    void print(Set<Item> I) {
        for (Item item : I) {
            String s = item.getProduction().getRight();
            int find = s.indexOf(item.getNextSymbol().getValue());
            if (find == -1) {
                System.out.printf("%c -> %5s .\t", item.getProduction().getLeft().getValue(), s);
                for(Symbol symbol: item.getLookahead()) {
                    System.out.printf(" %c,", symbol.getValue());
                }
                System.out.println();
            } else {
                String left = "", right = "";
                if (find != 0) {
                    left = s.substring(0, find);
                }
                right = s.substring(find);
                System.out.printf("%c -> %s . %s\t", item.getProduction().getLeft().getValue(), left, right);
                for(Symbol symbol: item.getLookahead()) {
                    System.out.printf(" %c,", symbol.getValue());
                }
                System.out.println();
            }
        }
    }

    /**
     * 比较两个集合是否完全相同 注意我调用的是contain方法
     *
     * @param x item集合1
     * @param y item集合2
     * @return 相同则返回true 否则返回fasle
     */
    private boolean isEqual(Set<Item> x, Set<Item> y) {
        if (x.size() != y.size()) return false;

        for (Item item : x) {
            if (!y.contains(item))
                return false;
        }
        return true;
    }

    /**
     * 判断两条边是否完全相同 相同则返回true 否则返回false 分别比较了from to symbol
     *
     * @param x 边x
     * @param y 边y
     * @return 是否相同
     */
    private boolean isEqual(Edge x, Edge y) {
        return x.getFrom() == y.getFrom() && x.getTo() == y.getTo() && x.getSymbol() == y.getSymbol();
    }

    /**
     * 将从from到to 边上为symbol的边添加到了 全局的边列表和 局部的边列表中
     *
     * @param from        哪个状态
     * @param to          到哪个状态
     * @param symbol      边上值为
     * @param addEdgeList 局部边列表
     */
    private void addEdge(int from, int to, Symbol symbol, List<Edge> addEdgeList) {
        boolean isEdgeEq = false;
        Edge e = new Edge(from, to, symbol);
        for (Edge edge : edgeList) {
            if (isEqual(e, edge)) {
                isEdgeEq = true;
                break;
            }
        }
        if (!isEdgeEq) {
            edgeList.add(e);
            addEdgeList.add(e);
        }
    }

    /**
     * 构造LR0分析器的分析算法
     */
    public void construct() {
        boolean isChanged = true, isItemEq;
        int index = 1;//状态的编号

        Set<Item> tmp = Closure(states.get(beginState));//初始化T为最开始状态的闭包
        // E <= empty
        List<Set<Item>> addItemList = new ArrayList<>();
        List<Edge> addEdgeList = new ArrayList<>();

        states.put(index++, tmp);// T <= {closure(S' -> S)}
        //setItemList.add(tmp); 如果这里添加了就会出现重复添加的情况

        //计算DFA
        while (isChanged) {
            isChanged = false;//重置
            addItemList.clear();
            addEdgeList.clear();
            for (Map.Entry<Integer, Set<Item>> I : states.entrySet()) {// for T 中的每一个状态I
                for (Item item : I.getValue()) {
                    Set<Item> J = Goto(I.getValue(), item.getNextSymbol());
                    int idx = 0;

                    if (J.size() == 0) continue;
                    isItemEq = false;
                    for (Set<Item> items : setItemList) { //看一下状态是不是已经被包含了
                        if (isEqual(items, J)) {
                            if (isEqual(I.getValue(), J)) { //自己到自己
                                addEdge(I.getKey(), I.getKey(), item.getNextSymbol(), addEdgeList);
                            }
                            isItemEq = true;
                            idx = setItemList.indexOf(items) + 1;
                            break;
                        }
                    }
                    if (!isItemEq) {
                        setItemList.add(J);
                        addItemList.add(J);
                        idx = setItemList.size();
                    }

                    addEdge(I.getKey(), idx, item.getNextSymbol(), addEdgeList);
                }
            }

            //将这些新边和新状态添加到之前的内容重
            for (Set<Item> item : addItemList) {
                states.put(index++, item);
                isChanged = true;
            }
            isChanged |= edgeSet.addAll(addEdgeList);
        }

        printStates();
        printEdges();

        int dim;
        //目前我有 edge items
        //归约动作 REDUCE J reduce 这里和LR0是不一样的
        for (Map.Entry<Integer, Set<Item>> items : states.entrySet()) {
            for (Item item : items.getValue()) {
                System.out.println("当前进行处理的项目是" + item);
                if (item.getNextSymbol().equals(Symbol.RIGHTMOST) || item.getNextSymbol().equals(Terminal.End)) { //如果已经到最右边了 可以进行归约了
                    Production production = item.getProduction();

//                    System.out.println();

                    if (production.getNum() == 0) { // accept
                        dim = transDim(items.getKey(), Terminal.End);
                        //System.out.printf("accept %d production: %c->%s dim: %d\n", items.getKey(), production.getLeft().getValue(), production.getRight(), dim);
                        tables.put(dim, new Entry(production.getNum(), Entry.ACCEPT));
                    } else { // reduce j
//                        for(Terminal terminal: terminals) { // 这里就是LR0的缺陷 直接把所有的都置为了rj
//                            dim = transDim(items.getKey(), terminal);
//                            //System.out.printf("reduce %d production: %c->%s dim: %d\n", items.getKey(), production.getLeft().getValue(), production.getRight(), dim);
//                            tables.put(dim, new Entry(production.getNum(), Entry.REDUCE));
//                        }

                        for(Symbol symbol: item.getLookahead()) {
                            System.out.println("symbol:"+symbol.getValue());
                            System.out.printf("reduce %d production: %c->%s dim: \n", items.getKey(), production.getLeft().getValue(), production.getRight());
                            if(symbol.equals(Symbol.EMPTY) || symbol instanceof Terminal) {
                                symbol = symbol.equals(Symbol.EMPTY) ? Terminal.End : symbol;
                                dim = transDim(items.getKey(), symbol);
                                tables.put(dim, new Entry(production.getNum(), Entry.REDUCE));
                            }
                        }

                    }
                }
            }
        }

        //移入动作 SHIFT J
        //跳转动作 goto(I,X) = J
        for (Edge edge : edgeSet) {
            if (edge.getSymbol() instanceof NonTerminal) {
                dim = transDim(edge.getFrom(), edge.getSymbol());//    state, non-terminal
                //System.out.printf("goto %d -> %d using %c dim:%d\n", edge.getFrom(), edge.getTo(), edge.getSymbol().getValue(), dim);
                tables.put(dim, new Entry(edge.getTo(), Entry.GOTO));//Integer => Entry 遇到的
            } else {
                dim = transDim(edge.getFrom(), edge.getSymbol());// state, terminal
                //System.out.printf("shift %d -> %d using %c dim:%d\n", edge.getFrom(), edge.getTo(), edge.getSymbol().getValue(), dim);
                tables.put(dim, new Entry(edge.getTo(), Entry.SHIFT));// shift j
            }
        }

    }

    /**
     * 根据给定的action获取对应的字符 a g r s e
     * @param action 给定的动作action
     * @return 对应的字符
     */
    private char getCharAction(int action) {
        switch (action) {
            case Entry.ACCEPT:
                return 'a';
            case Entry.GOTO:
                return 'g';
            case Entry.REDUCE:
                return 'r';
            case Entry.SHIFT:
                return 's';
            case Entry.ERROR:
                return 'e';
        }
        return 'e';
    }

    /**
     * 打印first集合
     */
    void printFirst() {
        System.out.println("下面是first集合");
        for (First first : firsts) {
            System.out.print(first.getLeft() + "\t=>\t");
            for (Terminal t : first.getRight()) {
                System.out.print(t.toString() + " ");
            }
            System.out.println();
        }
    }

    /**
     * 打印所有的状态
     */
    void printStates() {
        //打印状态
        for (Map.Entry<Integer, Set<Item>> items : states.entrySet()) {
            System.out.printf("这是%d状态\n", items.getKey());
            print(items.getValue());
        }
    }

    /**
     * 打印所有的边
     */
    void printEdges() {
        //打印边
        //System.out.println("边数量" + edgeSet.size());
        for (Edge edge : edgeSet) {
            System.out.println();
            print(states.get(edge.getFrom()));
            System.out.println(edge);
            print(states.get(edge.getTo()));
            System.out.println();
        }
    }
    /**
     * 打印最后生成的表
     */
    void printTables() {
        System.out.printf("\t");
        for (Terminal terminal : terminals) {
            System.out.printf("%c\t", terminal.getValue());
        }
        for (NonTerminal nonTerminal : nonTerminals) {
            System.out.printf("%c\t", nonTerminal.getValue());
        }
        System.out.println();
        int sz = states.size(), dim;//获取状态总数
        for (int i = 1; i <= sz; ++i) {
            System.out.printf("%d\t", i);
            for (Terminal terminal : terminals) {
                dim = transDim(i, terminal);
                //System.out.printf("\n状态 %d terminal %c dim %d\n", i, terminal.getValue(), dim);
                Entry entry = tables.get(dim);
                if(entry == null)
                    System.out.print("  \t");
                else
                    System.out.printf("%c%d\t",  getCharAction(entry.getAction()), entry.getState());
            }
            for (NonTerminal nonTerminal : nonTerminals) {
                dim = transDim(i, nonTerminal);
                //System.out.printf("\n状态 %d nonterminal %c dim %d\n", i, nonTerminal.getValue(), dim);
                Entry entry = tables.get(dim);
                if(entry == null)
                    System.out.print("  \t");
               else {
                    //System.out.println("状态" + i + "遇到的符号" + nonTerminal.getValue());
                   // System.out.println(entry.toString());
                  //  System.out.println("动作" + entry.getAction() + nonTerminal.getValue());
                    System.out.printf("%c%d\t", getCharAction(entry.getAction()), entry.getState());
                }
            }
            System.out.println();
        }
    }

    //将state和符号转换成一个int 该int用于tables 去寻找对应的entry
    private int transDim(int state, Symbol symbol) {
        return (state << 8) + (int)symbol.getValue();
        //return Objects.hash(state, symbol.getValue());
    }

    /**
     * 根据分析表分析输入串是否符合要求
     *
     * @param input 输入串
     * @return 符合要求则返回true 否则返回false
     */
    boolean analysis(String input) {
        //1.初始化两个栈
        Stack<Integer> stateStack = new Stack<>();//状态栈
        Stack<Symbol> symbolStack = new Stack<>();//符号栈
        char c;
        boolean flag = false;
        int index = 0, state = 1, dim;
        Symbol x = Terminal.End, tmp;
        Production p;

        stateStack.push(beginState);//先将最开始的状态压栈
        symbolStack.push(Terminal.End);//将空压栈 尽可能保证两个栈的大小一样

        //2.串与当前分析的元素处理
        while (!flag) {
            c = index < input.length() ? input.charAt(index) : '#';
            tmp = getSymbols(c);
            dim = transDim(state, tmp);
            Entry entry = tables.get(dim);

            //出错
            if (entry == null) break;
            switch (entry.getAction()) {
                case Entry.SHIFT://shift
                    state = entry.getState();
                    System.out.printf("shift %d\n", state);
                    stateStack.push(state);
                    x = tmp;
                    symbolStack.push(x);
                    index++;//继续分析
                    break;
                case Entry.GOTO://其实不会出现这种情况的
                    break;
                case Entry.ACCEPT:
                    flag = true;//识别成功
                    break;
                case Entry.REDUCE://关键是这里
                    p = productions.get(entry.getState());//获得reduce j中的j
                    System.out.printf("reduce %d\t%c => %s", p.getNum(), p.getLeft().getValue(), p.getRight());
                    int length = p.getRight().length();
                    //两个栈弹出右部长度个元素
                    for (int i = 0; i < length; i++) {
                        stateStack.pop();
                        symbolStack.pop();
                    }
                    state = stateStack.pop();//获得当前状态栈的栈顶
                    stateStack.push(state);
                    x = p.getLeft();//获得归约得到的非终结符A

                    dim = transDim(state, x);
                    System.out.printf("\tgoto(%d,%c) = %d\n", state, x.getValue(), tables.get(dim).getState());
                    state = tables.get(dim).getState();
                    stateStack.push(state);//压入goto[k, A]
                    symbolStack.push(x);//压入A
                    break;
                case Entry.ERROR:
                    break;

            }
        }
        return flag;
    }

    /**
     * 运行最后的程序 给定input输入串 和grammar 输入的语法
     * @param input 要分析的串
     * @param grammar 要分析的语法
     * @return 是否匹配
     */
    public boolean program(String input, String grammar) {
        geneProduction(grammar);//生成产生式 终结符号 非终结符号
        geneFirst();//生成first集合
        geneItems();//先生成所有的项目 便于后续的set操作
        construct();//构造最后的表项
        printTables();//打印表
        return analysis(input);//返回分析结果
    }

    private void test() {
        //Set<Item> tmp = Closure(states.get(beginState));//初始化T为最开始状态的闭包
        //print(tmp);
        construct();
        printTables();
    }
    public static void main(String[] args) {
        /*
        给定的文法
        S V=E
        S E
        E V
        V x
        V *E


        S UTa
        S Tb
        T S
        T Sc
        T d
        U US
        U e


        P P(P)
        P Aa
        P $
        A $
        分析串
        *x=**x

        edbcbda
        * */
        LALR1 lr1 = new LALR1();
        //默认文法第一个符号为 S 即文法的开始符号
        String grammar = "S V=E\nS E\nE V\nV x\nV *E";
        String s = "***x=**x";

        grammar = "S UTa\n" +
                "S Tb\n" +
                "T S\n" +
                "T Sc\n" +
                "T d\n" +
                "U US\n" +
                "U e";
        s = "edbcbda";

        grammar = "P P(P)\n" +
                "P Aa\n" +
                "P $\n" +
                "A $";
        s = "a()";
        System.out.println(lr1.program(s, grammar));
//        lr1.geneProduction(grammar);
//        lr1.geneFirst();
//        lr1.printFirst();
//
//        lr1.geneItems();
//
//        lr1.test();
    }
}
