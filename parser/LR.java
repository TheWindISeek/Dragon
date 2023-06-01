package parser;

import java.util.*;

/**
 * @author JeffreySharp
 * @apiNote LR分析程序的构建
 * 和构建LL分析程序一样 分为两步
 * 首先是构建分析引擎 接着是构建分析产生器 先构建LR0 再深入构建LR1
 * 分析引擎在这里比较麻烦的就是多了一堆东西
 * 因此我打算使用和之前差不多的思路 即将这个表进行压缩
 * 状态+遇到的符号 进行转移 得到具体的表项编号
 * 因此我需要一种哈希技术 能够将状态和符号映射到一个数字上
 * 状态是 1 2 3 4
 * 符号是 可以预先假定小于256 然后用byte[] 啥的替换
 * 接着再让这个作为key 去得到最后的项目 这个项目应该有的内容是
 * 项目种类 accept shift reduce goto
 * 状态  int
 * <p>
 * <p>
 * 因此假如给定的文法是 (很明显 这是一个增广文法 现在我先不管是否为增广文法 开始处理)
 * 0 S' -> S #
 * 1 S  -> (L)
 * 2 S -> x
 * 3 L -> S
 * 4 L -> L, S
 * <p>
 * 执行的核心动作是
 * Closure(I) 项集合
 * Closure(I) =
 * repeat
 * for I 中 任意 item A-> alpha . X beta
 * for 任意产生式 X -> γ
 * I <- I + { X -> . γ}
 * until I 没有改变
 * return I
 * <p>
 * Goto(I, X) 项集合 文法符号
 * Goto(I, X) =
 * J <- empty
 * for I 中 任意 item A -> alpha . X beta
 * J = J + {A -> alpha X . beta}
 * return Closure(J)
 * <p>
 * 构造算法
 * T <- {Closure(S' -> . S # }
 * E <- empty
 * repeat
 * for T 中每一个状态 I
 * for I 中 每一个项 A -> alpha . X beta
 * J <- Goto(I, X)
 * T <- T + J
 * E <- E + I->X J
 * until E, T没有发生改变
 * <p>
 * R <- empty
 * for T 中 每一个状态 I
 * //如果是最后的结束符号
 * if I contain S' -> S . #
 * (I, #) = a
 * //归约动作
 * for I 中每一项 A -> a.
 * R <- R + {I, A->a}
 * (I,Y) = rn (n代表规则几 或者说这是第几个产生式)
 * <p>
 * 对于E 中的每一条边 I ->X J
 * //终结符说明是移进
 * X 为 终结符号
 * (I,X) = sJ
 * //非终结符说明已归约 可goto
 * X 为 非终结符号
 * (I,X) = gJ
 * <p>
 * 会得到下列表
 * (   )   x   ,   #   S   L
 * 1   s3      s2          g4
 * 2   r2  r2  r2  r2  r2
 * 3   s3      s2      g7  g5
 * 4                   a
 * 5       s6      s8
 * 6   r1  r1  r1  r1  r1
 * 7   r3  r3  r3  r3  r3
 * 8   s3      s2          g9
 * 9   r4  r4  r4  r4  r4
 * <p>
 * <p>
 * 表分析算法
 * 如果动作是
 * shift n
 * 前进到下一个单词
 * 将n压入栈中
 * reduce k
 * 从栈顶依次弹出单词 个数就是产生式右部单词的数量
 * 令 X 是规则k的左部符号
 * 在栈顶当前所处的状态I下 Goto(I, X)
 * 接收 停止分析 报告成功
 * 错误 停止分析 报告失败
 * <p>
 * DFA下标是状态编号
 * 因此需要一个符号栈和一个状态栈
 * <p>
 * <p>
 * <p>
 * 总结一下
 * <p>
 * 我需要一个类
 * 项目类用于每一个的推导与产生式
 * item
 * Production production; //哪个产生式对应的项目
 * Symbol nextSymbol; // 下一个将要被分析的符号是谁 如果是最右边了 该归约了 那么这个就可以等于一个特殊的symbol
 * Symbol lookahead; // 为后续的LR1准备
 * <p>
 * 分析表应该采取何种方法构建呢
 * <p>
 * 首先我有一个状态Int 接着我有一个文法符号Symbol
 * 我需要将这两个组合起来
 * int transDim(int state, Symbol symbol) {
 * return state << 8 + (int)symbol.getValue();
 * }
 * 接着我需要一个Map<int, Entry> tables;
 * Entry
 * int action;//什么语义动作 accept reduce shift (error)
 * int state;//紧跟着的状态 对于reduce来说其实是哪个产生式 但是没必要特地为其存储一个
 * <p>
 * 然后就是驱动算法的计算了
 * 首先需要做两个栈
 * 符号栈最开始为空
 * 但是状态栈不为空 他需要压入存在增广文法S' -> .S中的那个状态 可以验证 这样的状态只有一个
 * <p>
 * 所以这个状态是开始的状态 int beginState;//最开始的状态
 * <p>
 * <p>
 * List<set<Item> > states;//
 */
public class LR {
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
    public void geneProduction(String grammar) {
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
                    items.add(new Item(production, symbol));
                    states.put(beginState, items);
                }
                nonTerminals.add((NonTerminal) symbol);
            }

            //add production and nonterminal
            Production production = new Production((NonTerminal) symbol, right, index++);
//            productions.add(production);
            productions.put(production.getNum(), production);
            symbols.append(right);
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
     */
    private void geneItems() {
        int index, sz, hashCode;
        Item item;
        for (Production production : productions.values()) {
            index = 0;
            sz = production.getRight().length();
            while (index < sz) {
                hashCode = Objects.hash(production, getSymbols(production.getRight().charAt(index)));
                item = new Item(production, getSymbols(production.getRight().charAt(index)));
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
                    if (nextSymbol.equals(production.getLeft())) {
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
                int index = 1 + s.indexOf(X.getValue());
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
        setItemList.add(tmp);

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
                        for(Terminal terminal: terminals) { // 这里就是LR0的缺陷 直接把所有的都置为了rj
                            dim = transDim(items.getKey(), terminal);
                            //System.out.printf("reduce %d production: %c->%s dim: %d\n", items.getKey(), production.getLeft().getValue(), production.getRight(), dim);
                            tables.put(dim, new Entry(production.getNum(), Entry.REDUCE));
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
        int sz = 9, dim;//获取状态总数
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
        return Objects.hash(state, symbol.getValue());
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

    public static void main(String[] args) {
        /*
        给定的文法
        S (L)
        S x
        L S
        L L,S
        * */
        LR lr = new LR();
        //默认文法第一个符号为 S 即文法的开始符号
        String grammar = "S (L)\nS x\nL S\nL L,S";
        String s = "((x,(x)))";
        //1.读入产生式
        //2.构建增广文法
        lr.geneProduction(grammar);
        lr.construct();
        /*
        Terminal leftParenthesis = new Terminal('(');
        Terminal rightParenthesis = new Terminal(')');
        Terminal x = new Terminal('x');
        Terminal comma = new Terminal(',');

        NonTerminal S = new NonTerminal('S');
        NonTerminal L = new NonTerminal('L');

        Production Shat = new Production(lr.beginSymbol, "S", 0);
        Production SL = new Production(S, "(L)", 1);
        Production Sx = new Production(S, "x", 2);
        Production LS = new Production(L, "S", 3);
        Production LLS = new Production(L, "L,S", 4);

        lr.terminals.add(leftParenthesis);
        lr.terminals.add(rightParenthesis);
        lr.terminals.add(x);
        lr.terminals.add(Terminal.End);
        lr.terminals.add(comma);
        lr.nonTerminals.add(S);
        lr.nonTerminals.add(L);
        lr.productions.put(Shat.getNum(), Shat);
        lr.productions.put(SL.getNum(), SL);
        lr.productions.put(Sx.getNum(), Sx);
        lr.productions.put(LS.getNum(), LS);
        lr.productions.put(LLS.getNum(), LLS);


        //3.构造项目集合

        //4.根据项目集合产生分析表

        int dim = lr.transDim(1, leftParenthesis);
        lr.tables.put(dim, new Entry(3, Entry.SHIFT));
        dim = lr.transDim(1, x);
        lr.tables.put(dim, new Entry(2, Entry.SHIFT));
        dim = lr.transDim(1, S);
        lr.tables.put(dim, new Entry(4, Entry.GOTO));

        dim = lr.transDim(2, leftParenthesis);
        lr.tables.put(dim, new Entry(2, Entry.REDUCE));
        dim = lr.transDim(2, rightParenthesis);
        lr.tables.put(dim, new Entry(2, Entry.REDUCE));
        dim = lr.transDim(2, x);
        lr.tables.put(dim, new Entry(2, Entry.REDUCE));
        dim = lr.transDim(2, comma);
        lr.tables.put(dim, new Entry(2, Entry.REDUCE));
        dim = lr.transDim(2, Terminal.End);
        lr.tables.put(dim, new Entry(2, Entry.REDUCE));

        dim = lr.transDim(3, leftParenthesis);
        lr.tables.put(dim, new Entry(3, Entry.SHIFT));
        dim = lr.transDim(3, x);
        lr.tables.put(dim, new Entry(2, Entry.SHIFT));
        dim = lr.transDim(3, S);
        lr.tables.put(dim, new Entry(7, Entry.GOTO));
        dim = lr.transDim(3, L);
        lr.tables.put(dim, new Entry(5, Entry.GOTO));

        dim = lr.transDim(4, Terminal.End);
        lr.tables.put(dim, new Entry(0, Entry.ACCEPT));

        dim = lr.transDim(5, rightParenthesis);
        lr.tables.put(dim, new Entry(6, Entry.SHIFT));
        dim = lr.transDim(5, comma);
        lr.tables.put(dim, new Entry(8, Entry.SHIFT));

        dim = lr.transDim(6, leftParenthesis);
        lr.tables.put(dim, new Entry(1, Entry.REDUCE));
        dim = lr.transDim(6, rightParenthesis);
        lr.tables.put(dim, new Entry(1, Entry.REDUCE));
        dim = lr.transDim(6, x);
        lr.tables.put(dim, new Entry(1, Entry.REDUCE));
        dim = lr.transDim(6, comma);
        lr.tables.put(dim, new Entry(1, Entry.REDUCE));
        dim = lr.transDim(6, Terminal.End);
        lr.tables.put(dim, new Entry(1, Entry.REDUCE));

        dim = lr.transDim(7, leftParenthesis);
        lr.tables.put(dim, new Entry(3, Entry.REDUCE));
        dim = lr.transDim(7, rightParenthesis);
        lr.tables.put(dim, new Entry(3, Entry.REDUCE));
        dim = lr.transDim(7, x);
        lr.tables.put(dim, new Entry(3, Entry.REDUCE));
        dim = lr.transDim(7, comma);
        lr.tables.put(dim, new Entry(3, Entry.REDUCE));
        dim = lr.transDim(7, Terminal.End);
        lr.tables.put(dim, new Entry(3, Entry.REDUCE));

        dim = lr.transDim(8, leftParenthesis);
        lr.tables.put(dim, new Entry(3, Entry.SHIFT));
        dim = lr.transDim(8, x);
        lr.tables.put(dim, new Entry(2, Entry.SHIFT));
        dim = lr.transDim(8, S);
        lr.tables.put(dim, new Entry(9, Entry.GOTO));

        dim = lr.transDim(9, leftParenthesis);
        lr.tables.put(dim, new Entry(4, Entry.REDUCE));
        dim = lr.transDim(9, rightParenthesis);
        lr.tables.put(dim, new Entry(4, Entry.REDUCE));
        dim = lr.transDim(9, x);
        lr.tables.put(dim, new Entry(4, Entry.REDUCE));
        dim = lr.transDim(9, comma);
        lr.tables.put(dim, new Entry(4, Entry.REDUCE));
        dim = lr.transDim(9, Terminal.End);
        lr.tables.put(dim, new Entry(4, Entry.REDUCE));

        lr.printTables();*/
        //5.根据分析表进行分析
        System.out.println(lr.analysis(s));
    }
}
