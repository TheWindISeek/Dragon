package parser;

import java.util.*;

/**
 * @author JeffreySharp
 * @apiNote SLR分析程序的构建
 * 因为SLR和LR从构建上来说可以说几乎一样
 * 只是在生成归约项的时候会出现不一样的地方
 * SLR只在下一个符号 在当前的左部的文法符号 的follow集合的时候才会去归约
 * 也就是说构造的方法如下所示
 *
 * 其实我这么写就是默认 shift的优先级比reduce高了 也不是不可以 这样其实也是可以解决冲突的
 *
 * Reduce
 *      R <= {}
 *      for T 中的每一个状态 I
 *          for I 中的每一个项 A-> a.
 *              for Follow(A) 中的每一个单词 X
 *                  R <= R + {I, X, A->a}
 *
 * 对于文法
 * 0 S => E
 * 1 E => T + E
 * 2 E => T
 * 3 T => x
 */
public class SLR {
    List<First> firsts = new ArrayList<>();//所有的 firsts 集合
    Map<Symbol, First> firstMap = new HashMap<>();//终结符和所有非终结符号对应的 first
    Map<String, First> stringFirstMap = new HashMap<>();//产生式的一部分对应first string => first
    //Nonterminal -> Follow; whileas only Nonterminal have Follow set...?
    Map<NonTerminal, Follow> followMap = new HashMap<>();
    List<Follow> follows = new ArrayList<>();


    Map<Integer, Set<Item>> states = new HashMap<>();//状态的编号 -> 状态编号里面的项目
    List<Set<Item>> setItemList = new ArrayList<>();//这里存放所有的set<Item>
    List<Edge> edgeList = new ArrayList<>();//这里存放所有的edge
    Map<Integer, Item> itemMap = new HashMap<>();//用于存放所有的item 便于之后的管理
    NonTerminal beginSymbol = NonTerminal.Begin;
    Map<Integer, Production> productions = new HashMap<>();
    //所有的终结符
    Set<Terminal> terminals = new HashSet<>();
    //所有的非终结符号
    Set<NonTerminal> nonTerminals = new HashSet<>();

    //最开始的状态
    int beginState = 1; //状态默认从1开始 由于起始状态必为1 故可这么操作
    //分析表
    Map<Integer, Entry> tables = new HashMap<>();


    //get symbol from char
    Symbol getSymbols(char c) {
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
                    Production production = new Production(beginSymbol, left, index++);
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

        //add all first to firsts
        for(Map.Entry<Symbol, First> entry: firstMap.entrySet()) {
            if(entry.getKey() instanceof NonTerminal)
                firsts.add(entry.getValue());
        }
        for(Map.Entry<String, First> entry: stringFirstMap.entrySet()) {
            firsts.add(entry.getValue());
        }
    }


    /**
     * recursive slove follow
     * @param left left part of Non-terminal
     * @param index from index to length
     * @param right production body
     */
    private boolean followFromIndex(NonTerminal left, int index, String right, boolean isChanged) {
        Symbol symbol = getSymbols(right.charAt(index++));
        //find the first non-terminal of production
        while (symbol instanceof Terminal && index < right.length()) {
            symbol = getSymbols(right.charAt(index++));
        }

        //really found
        if(symbol instanceof NonTerminal) {
            boolean empty = false;
            NonTerminal B = (NonTerminal) symbol;
            symbol = index < right.length() ? getSymbols(right.charAt(index)) : Terminal.Empty;
            //get next char's first set
            First first = firstMap.get(symbol);
            Follow follow = followMap.get(B);

            for(Terminal t:first.getRight()) {
                if(t.equals(Terminal.Empty)) {
                    empty = true;
                    continue;
                }
                isChanged |= follow.add(t);
            }

            //if sigma belong to beta
            if(empty) {
                //follow(B) += follow(A)
                for(Terminal t: followMap.get(left).getRight()) {
                    isChanged |= follow.add(t);
                }
            }
        }

        return isChanged;
    }

    /**
     * generate follow set from first set
     */
    public void geneFollow() {
//        beginSymbol = getBeginSymbol();

        //all non-terminal to follow map
        Follow follow_beginSymbol = new Follow(beginSymbol, Terminal.End);
        followMap.put(beginSymbol,follow_beginSymbol);
        for(NonTerminal nonTerminal:nonTerminals) {
            if(nonTerminal.equals(beginSymbol)) continue;
            followMap.put(nonTerminal, new Follow(nonTerminal));
        }

        boolean isChanged = true;
        //the first production's left is begin!
        while (isChanged){
            isChanged = false;
            for(Production production:productions.values()) {
                NonTerminal left = production.getLeft();
                String right = production.getRight();
                int index = 0;
                while (index < right.length()) {
                    isChanged |= followFromIndex(left,index,right, isChanged);
                    index++;
                }
            }
        }

        //to follow list
        for(Map.Entry<NonTerminal,Follow> followEntry:followMap.entrySet()) {
            follows.add(followEntry.getValue());
        }
    }


    void showFirst() {
        System.out.println("first are as followed.");
        for (First first : firsts) {
            System.out.print(first.getLeft() + "\t=\t");
            for (Terminal t :
                    first.getRight()) {
                System.out.print(t.toString() + " ");
            }
            System.out.println();
        }
    }

    void showFollow() {
        System.out.println("follow are as followed.");
        for(Follow follow:follows) {
            System.out.print(follow.getLeft() + "\t=\t");
            for(Terminal t:follow.getRight()) {
                System.out.print(t.toString() + " ");
            }
            System.out.println();
        }
    }

    /**
     * 根据产生式生成所有的items 方便后续set的判断
     */
    private void geneItems() {
        int index, sz, hashCode;
        Item item;
        for (Production production : productions.values()) {
            index = 0;
            sz = production.getRight().length();
            while (index < sz) { //其实在这里我生成所有的式子的时候 直接把所有的都生成了
                hashCode = Objects.hash(production, getSymbols(production.getRight().charAt(index)));
                item = new Item(production, getSymbols(production.getRight().charAt(index)), index);
                itemMap.put(hashCode, item);
                index++;
                System.out.printf("%-10d\t %s \n", hashCode, item.toString());
            }
            hashCode = Objects.hash(production, Symbol.RIGHTMOST);
            item = new Item(production, Symbol.RIGHTMOST);
            itemMap.put(hashCode, item);
            System.out.printf("%-10d\t %s \n", hashCode, item.toString());
        }
    }

    /**
     * 求状态I的闭包
     *
     * @param I 状态I 其中包含项目
     * @return closure(I)
     */
    public Set<Item> Closure(Set<Item> I) {
        boolean isChanged = true;
        List<Item> list = new ArrayList<>();
        while (isChanged) {
            isChanged = false;
            list.clear();
            for (Item item : I) {
                //跳过 下一个符号是终结符号的 因为这样肯定不需要求解
                if (item.getNextSymbol().equals(Symbol.RIGHTMOST)) continue;
                if (item.getNextSymbol() instanceof Terminal) continue;
                NonTerminal nextSymbol = (NonTerminal) item.getNextSymbol();
                for (Production production : productions.values()) {
                    if (nextSymbol.equals(production.getLeft())) {//仔细思考一下这里 其实也没有问题 是吧 因为只要识别到了是这个 我把他可能到的状态都给加进去就是了
                        //新建一个项目 加入到set中; x->y .y
                        int hashCode = Objects.hash(production, getSymbols(production.getRight().charAt(0)));
                        //Item i = new Item(production, getSymbols(production.getRight().charAt(0)));
                        list.add(itemMap.get(hashCode));
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
        Set<Item> J = new HashSet<>();
        if (X.equals(Symbol.RIGHTMOST)) return J;//如果都扫描到最右边了 那么很显然走不下去了
        for (Item item : I) {
            //如果可以跳到下一个状态
            if (item.getNextSymbol().equals(X)) {
                String s = item.getProduction().getRight();
                int index = item.getIndex()+1;
                Symbol c = index == s.length() ? Symbol.RIGHTMOST : getSymbols(s.charAt(index));
                int hash = Objects.hash(item.getProduction(), c);
                J.add(itemMap.get(hash));
            }
        }
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
                System.out.printf("%c -> %5s .\n", item.getProduction().getLeft().getValue(), s);
            } else {
                String left = "", right = "";
                if (find != 0) {
                    left = s.substring(0, find);
                }
                right = s.substring(find);
                System.out.printf("%c -> %s . %s\n", item.getProduction().getLeft().getValue(), left, right);
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
        geneItems();//先生成所有的项目 便于后续的set操作

        boolean isChanged = true, isItemEq;
        int index = 1;//状态的编号

        Set<Item> tmp = Closure(states.get(beginState));//初始化T为最开始状态的闭包
        Set<Edge> edgeSet = new HashSet<>();// E <= empty
        List<Set<Item>> addItemList = new ArrayList<>();
        List<Edge> addEdgeList = new ArrayList<>();

        states.put(index++, tmp);// T <= {closure(S' -> S)}
        //setItemList.add(tmp);

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

        //打印状态
        for (Map.Entry<Integer, Set<Item>> items : states.entrySet()) {
            System.out.printf("这是%d状态\n", items.getKey());
            print(items.getValue());
        }
        //打印边
        //System.out.println("边数量" + edgeSet.size());
        for (Edge edge : edgeSet) {
            System.out.println();
            print(states.get(edge.getFrom()));
            System.out.println(edge);
            print(states.get(edge.getTo()));
            System.out.println();
        }


        int dim;
        //目前我有 edge items
        //归约动作 REDUCE J
        for (Map.Entry<Integer, Set<Item>> items : states.entrySet()) {
            for (Item item : items.getValue()) {
                if (item.getNextSymbol().equals(Symbol.RIGHTMOST)) { //如果已经到最右边了 可以进行归约了
                    Production production = item.getProduction();

                    if (production.getNum() == 0) { // accept
                        dim = transDim(items.getKey(), Terminal.End);
                        //System.out.printf("accept %d production: %c->%s dim: %d\n", items.getKey(), production.getLeft().getValue(), production.getRight(), dim);
                        tables.put(dim, new Entry(production.getNum(), Entry.ACCEPT));
                    } else { // reduce j

                        //只把follow后面的置为可跳转的
                        for(Terminal terminal: followMap.get(production.getLeft()).getRight()) {
                            dim = transDim(items.getKey(), terminal);
                            //System.out.printf("reduce %d production: %c->%s dim: %d\n", items.getKey(), production.getLeft().getValue(), production.getRight(), dim);
                            tables.put(dim, new Entry(production.getNum(), Entry.REDUCE));
                        }
//                        for(Terminal terminal: terminals) { // 这里就是LR0的缺陷 直接把所有的都置为了rj
//                            dim = transDim(items.getKey(), terminal);
//                            //System.out.printf("reduce %d production: %c->%s dim: %d\n", items.getKey(), production.getLeft().getValue(), production.getRight(), dim);
//                            tables.put(dim, new Entry(production.getNum(), Entry.REDUCE));
//                        }
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


                Entry entry = tables.get(dim);
                if(entry != null) {//遇到了移进归约冲突
                    System.out.printf("shift-reduce %d-> %d %c\n\n", edge.getFrom(), edge.getTo(), edge.getSymbol().getValue());
                }

                //System.out.printf("shift %d -> %d using %c dim:%d\n", edge.getFrom(), edge.getTo(), edge.getSymbol().getValue(), dim);
                tables.put(dim, new Entry(edge.getTo(), Entry.SHIFT));// shift j
            }
        }

        printTables();
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

    public boolean program(String input, String grammar) {
        geneProduction(grammar);
        geneFirst();
        geneFollow();
        showFirst();
        showFollow();
        construct();
        return analysis(input);
    }

    public static void main(String[] args) {
        /*
        给定的文法
        E T+E
        E T
        T x
        * */
        SLR lr0 = new SLR();
        //默认文法第一个符号为 S 即文法的开始符号

        String grammar = "S (L)\nS x\nL S\nL L,S";
        String s = "((x,(x)))";
        grammar = "E T+E\nE T\nT x";
        s = "x+x+x";
        System.out.println(lr0.program(s, grammar));
    }
}
