package parser;

import java.util.*;
import java.util.stream.Stream;

/**
 * @FileEncoding UTF-8
 * @author JeffreySharp
 * @Description
 * we assume an example's grammer of LL(1) as followed.
 *
 *      Non-Terminal:
 *          S
 *          A
 *          B
 *      Terminal:
 *          a
 *          b
 *          c
 *          d
 *          sigma (we use $ to replace it)
 *
 *      Set<Non-Terminal> non_terminals;
 *      Set<Terminal>   terminals;
 *      Grammer:
 *          S -> AaS
 *              | BbS
 *              | d
 *          A -> a
 *          B -> sigma
 *              | c
 *
 *       Input grammer:
 *          Non-Terminal{1}\s[Terminal Non-Terminal]{1,}
 *
 *          S AaS (1)
 *          S BbS (2)
 *          A a (3)
 *          B $ (4)
 *          B c (5)
 *          S d (6)
 *
 *          Set<Production> productions;
 *              left: NonTerminal
 *              right: String
 *              num: int
 *
 *       Input String:
 *          aabd
 *
 *          String input;
 *      Output:
 *          First:
 *              First(AaS) = {a}
 *              First(BbS) = {c,b}
 *              First(d) = {d}
 *              First(a) = {a}
 *              First(sigma) = {sigma}
 *              First(c) = {c}
 *
 *              List<First> firsts;
 *                  left:String
 *                  right: Set<Terminal>
 *        Follow:
 *              Follow(S) = {#}
 *              Follow(A) = {a}
 *              Follow(B) = {b}
 *
 *              List<Follow> follows;
 *                  Left: Non-Terminal
 *                  Right: Terminal
 *         Select:
 *              Select(S->AaS) = {a}
 *              Select(S->BbS) = {c,b}
 *              Select(S->d) = {d}
 *              Select(A->a) = {a}
 *              Select(B->sigma) = {b}
 *              Select(B->c) = {c}
 *
 *              List<Select> selects;
 *                  Left: Production
 *                  Right: Set<Terminal>
 *         Analysis Table:
 *                  a   b   c   d   #
 *              S   1   2   2   6
 *              A   3
 *              B       4   5
 *
 *              Map<<Non-Terminal, Terminal>, Number> should rewrite equals method.
 *
 *              we use mapping <Non-Terminal,Terminal> -> Non-Terminal * 256 + Terminal;
 *
 *
 *
 *        Temp Values:
 *              //store all non-terimal terminal production, for search
 *              Set<Non-Terminal> nonterminals;
 *              Set<Terminal> terminals;
 *              Set<Production> productions;
 *
 *              List<First> firsts;
 *              List<Follow> follows;
 *              List<Select> selects;
 *
 *              transDim(Non-Terminal, Terminal);
 *                  Non-Terminal.value << 8 + Terminal.value;
 *              Map<int, Production> analysis;
 */
public class LL {
    //input string
    String input = "";
    //all terminals, non-terminals, productions.
    Set<NonTerminal> nonTerminals = new HashSet<>();
    Set<Terminal> terminals = new HashSet<>();
    Set<Production> productions = new HashSet<>();
    //first, follow, select
    List<First> firsts = new ArrayList<>();
    List<Follow> follows = new ArrayList<>();
    List<Select> selects = new ArrayList<>();
    //analysis table
    Map<Integer, Production> analysisTable = new HashMap<>();
    //begin analysis symbol (non-terminal)
    NonTerminal beginSymbol = NonTerminal.Begin;
    //symbol -> first; Given Terminal or Nonterminal
    Map<Symbol, First> firstMap = new HashMap<>();
    //Nonterminal -> Follow; whileas only Nonterminal have Follow set...?
    Map<NonTerminal, Follow> followMap = new HashMap<>();
    //the right part of production -> First. Given right part of prodution
    Map<String, First> stringFirstMap = new HashMap<>();


    // transform two indexes to one index
    Integer transDim(NonTerminal nonTerminal, Terminal terminal) {
        //show index
       // System.out.println((nonTerminal.getIntValue() << 8 + terminal.getIntValue()));
        return nonTerminal.getIntValue() << 8 + terminal.getIntValue();
    }

    Integer transDim(NonTerminal nonTerminal, char c) {
       // System.out.println(nonTerminal.getIntValue() << 8 + (int) c);
        return nonTerminal.getIntValue() << 8 + (int)c;
    }

    //when i don't have followMap and firstMap, this function will be used for mapping c to symbol
    Symbol getSymbols(char c) {
        for (Terminal terminal: terminals) {
            if(terminal.getValue() == c) {
                return terminal;
            }
        }

        for(NonTerminal nonTerminal: nonTerminals) {
            if(nonTerminal.getValue() == c) {
                return nonTerminal;
            }
        }
        return Terminal.End;
    }

    //cpoy set from "from" to "to"
    private <T>void copySet(Set<T> to, Set<T> from) {
        for (T item :from) {
            to.add(item);
        }
    }

    /**
     * generate terminals, nontermianls, productions using grammar
     * which is
     * String grammar = "S AaS\nS BbS\nS d\nA a\nB $\nB c";
     * @param grammar
     */
    public void geneProduction(String grammar) {
        String[] splits = grammar.split("\n");

        StringBuffer symbols = new StringBuffer();
        int index = 1;
        for (String s_production:splits) {
            String[] strings = s_production.split(" ");
            //0->NonTermianl 1->right
            String left = strings[0];
            String right = strings[1];
            //add nonterminal and production to set
            Symbol symbol = getSymbols(left.charAt(0));
            //already have
            if(!(symbol instanceof NonTerminal)) {
                symbol = new NonTerminal(left.charAt(0));
                if(nonTerminals.size() == 0) {
                    beginSymbol = ((NonTerminal) symbol);
                }
                nonTerminals.add((NonTerminal) symbol);
            }
            //add production and nonterminal
            Production production = new Production((NonTerminal) symbol, right, index);
            productions.add(production);
            symbols.append(right);
        }
        //add terminal
        for (int i = 0; i < symbols.length(); i++) {
            char c = symbols.charAt(i);
            Symbol symbol = getSymbols(c);
            //we finally process sigma
            if(symbol instanceof NonTerminal) continue;
            if(c == '$') continue;
            //if not found
            if(symbol.equals(Terminal.End)) {
                terminals.add(new Terminal(c));
            }
        }
        //add end and empty, while as empty maybe have been added.
        terminals.add(Terminal.Empty);
        terminals.add(Terminal.End);
    }

    /**
     * gene first set from productions
     */
    public void geneFirst() {
        //all terminals's first = {terminal}
        for(Terminal t: terminals) {
            First first = new First(t.toString());
            first.add(t);

            firsts.add(first);
            firstMap.put(t, first);
        }

        //get all first(X)
       // Map<NonTerminal, First> map = new HashMap<>();
        for(NonTerminal nt: nonTerminals) {
            firstMap.put(nt, new First(nt.toString()));
        }

        for(Production production: productions) {
            //if X -> a...
            Symbol symbol = getSymbols(production.getRight().charAt(0));
            if(symbol instanceof Terminal) {
                //skip X -> sigma
//                if(((Terminal)symbol).equals(Terminal.Empty)) continue;
                First first = firstMap.get(production.getLeft());
                first.add((Terminal) symbol);

                //if right part only have one symbols which is terminal, then we should skip this case.
                if(production.getRight().length() == 1) continue;
                //right part also should be added to first list
                First rightFirst = new First(production.getRight());
//                copySet(rightFirst.getRight(),first.getRight());
                rightFirst.add((Terminal) symbol);
                stringFirstMap.put(production.getRight(),rightFirst);
            }
        }

        for(int i = 0; i < 2; ++i) {
            for(Production production: productions) {
                //if X -> Y1 Y2 Y3 ....
                int index = 0;
                Symbol symbol = getSymbols(production.getRight().charAt(index));
                if(symbol instanceof NonTerminal) {
                    First X = new First(production.getRight());
                    First Y = firstMap.get((NonTerminal) symbol);
                    boolean end = false;
                    for(Terminal t:Y.getRight()) {
                        if(t.equals(Terminal.Empty)) {
                            end = true;
                            continue;
                        }
                        X.add(t);
                    }

                    while (end) {
                        //find all non-terminal
                        if(index + 1 == production.getRight().length()) {
                            break;
                        }
                        symbol = getSymbols(production.getRight().charAt(++index));
                        //the terminal should be included in the first
                        if(symbol instanceof Terminal) {
                            X.add((Terminal) symbol);
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
                            X.add(t);
                        }
                    }

                    //all Y have sigma
                    if(end) {
                        X.add(Terminal.Empty);
                    }
                    //right part which is Y1Y2Y3...
//                    System.out.println(production);
//                    System.out.println("production.getright\t" + production.getRight());
                    stringFirstMap.put(production.getRight(),X);
                    //left part which is X
                    First first = firstMap.get(production.getLeft());
//                    System.out.println("production.getleft\t" + first.getLeft());
                    copySet(first.getRight(),X.getRight());
                }
            }
        }

        //add all first to firsts
        for(Map.Entry<Symbol, First> entry: firstMap.entrySet()) {
            if(entry.getKey() instanceof NonTerminal)
                firsts.add(entry.getValue());
        }
        for(Map.Entry<String, First> entry: stringFirstMap.entrySet()) {
//            System.out.println("key:\t" + entry.getKey() + "\tvalue:\t" + entry.getValue().getLeft());
            firsts.add(entry.getValue());
        }
    }

    /**
     * abused. for the begin symbols.
     * */
    public NonTerminal getBeginSymbol() {
        //根据现有的产生式 找到文法的开始符号
        //入度为0的点 但是左边是它自己的话 不应该算入度
        Set<NonTerminal> indegeree = new HashSet<>();
        for(Production production: productions) {
            String right = production.getRight();
            NonTerminal left = production.getLeft();
            int index = 0;
            Symbol symbol;

            while (index < right.length()) {
                symbol = getSymbols(right.charAt(index++));
                //非终结符号 且左边不是他自己
                if(symbol instanceof NonTerminal
                    && !((NonTerminal)symbol).equals(left)) {
                    indegeree.add((NonTerminal) symbol);
                }
            }
        }

        for(NonTerminal nt: nonTerminals) {
            if(!indegeree.contains(nt)) {
                return nt;
            }
        }
        //此时就无法确定开始符号了 随机返回一个作为开始符号
        return nonTerminals.iterator().next();
    }

    /**
     * recursive slove follow
     * @param left left part of Non-terminal
     * @param index from index to length
     * @param right production body
     */
    private void followFromIndex(NonTerminal left, int index, String right) {
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
                follow.add(t);
            }

            //if sigma belong to beta
            if(empty) {
                //follow(B) += follow(A)
                for(Terminal t: followMap.get(left).getRight()) {
                    follow.add(t);
                }
            }
        }
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

        //the first production's left is begin!
        for(int i = 0; i < 2; ++i) {
            for(Production production:productions) {
                NonTerminal left = production.getLeft();
                String right = production.getRight();
                int index = 0;
                while (index < right.length()) {
                    followFromIndex(left,index,right);
                    index++;
                }
            }
        }

        //to follow list
        for(Map.Entry<NonTerminal,Follow> followEntry:followMap.entrySet()) {
            follows.add(followEntry.getValue());
        }
    }
    /**
     * generate selects set from first set and follow set
     */
    public void geneSelects() {
        for (Production production: productions) {
            Select select = new Select(production);
            for (First first :firsts) {
                //adjust first's content and production's right part
                if(first.getLeft().equals(production.getRight().toString())) {
                    //find the production's first

                    //if contain sigma then select(production) = first(production)-sigma + follow(production)
                    if(first.contain(Terminal.Empty)) {
                        for (Terminal t: first.getRight()) {
                            if(t.equals(Terminal.Empty)) continue;
                            select.add(t);
                        }
                        //add rest's follow
                        for (Follow follow : follows) {
                            if (production.getLeft().equals(follow.getLeft())) {
                                copySet(select.getRight(),follow.getRight());
                            }
                        }
                    } else {
                        //select(production) = first(production)
                        copySet(select.getRight(), first.getRight());
                    }
                    break;
                }
            }
            //add select to selects
            selects.add(select);
        }
    }

    /**
     * from select set
     * generate analysis table
     */
    public void geneAnalysisTable() {
        for (Select s: selects) {
            //production used for select
            Production left = s.getLeft();
            //current production's select set
            Set<Terminal> right = s.getRight();
            //left part of production
            NonTerminal nonTerminal = left.getLeft();

            for (Terminal t :right) {
                this.analysisTable.put(transDim(nonTerminal, t), left);
            }
        }
    }

    /**
     * LL(1) analysis
     * using analysis table
     * @param input the format input string
     * @return is this input string legal
     */
    boolean isLegal(String input) {
        //is correct
        boolean flag = true;
        //index to indicate the index of input
        int index = 0;
        char a = input.charAt(index);
        //symbols stack; first we push #
        Stack<Symbol> symbolStack = new Stack<>();
        symbolStack.push(Terminal.End);
        symbolStack.push(beginSymbol);
        //the top of Stack
        Symbol X;

        while (flag) {
            X = symbolStack.pop();
            //x is termianl
            if(X instanceof Terminal) {
                //x == #
                if(((Terminal)X).equals(Terminal.End)) {
                    if(a == '#') {
                        flag = false;
                    } else {
                        //input string have rest charster
                        return false;
                    }
                } else if (((Terminal)X).equals(Terminal.Empty)) {
                    continue;
                } else {
                 //x != #
                    if(((Terminal) X).getValue() == a) {
                        if(index + 1 < input.length())
                            a = input.charAt(++index);
                        else
                            a = '#';
                    } else {
                        return false;
                    }
                }
            } else if (analysisTable.containsKey(transDim((NonTerminal) X, a))) {
                //if have X -> X1...
                Production production = analysisTable.get(transDim((NonTerminal) X, a));
                //add production to stack
                for (int i = production.getRight().length()-1; i >= 0; i--) {
                    //n-1 -> 0
                    symbolStack.push(getSymbols(production.getRight().charAt(i)));
                }
            } else {
                return false;
            }
        }
        return true;
    }

    //below function are used for show intermediate results

    /**
     * show the analysis table use the format as follow.
     *         a        b    c    d    #
     *  S   S->AaS S->BbS S->BbS S->d
     *  A    A->a
     *  B           B->$    B->c
     * @Param width: the max length of production
     */
    void showAnalysisTable(int width) {
        System.out.println("the result table are as followed.");
        //table head
        String format = "%-" + width + "s";
        System.out.printf(format, "");
        for (Terminal t : terminals) {
            System.out.printf(format, t.getValue());
        }
        System.out.println();
        //table items
        for (NonTerminal nt: nonTerminals) {
            System.out.printf(format,nt.getValue());
            for (Terminal t: terminals) {
                String output = "";
                // end and empty should not have production
                if(!t.equals(Terminal.Empty)) {
                    //if exists production
                    int key = transDim(nt, t);
                    if(analysisTable.containsKey(key)) {
                        output = analysisTable.get(key).toString();
                    }
                }
                System.out.printf(format, output);
            }
            System.out.println();
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

    void showBeginSymbol() {
        System.out.println("begin symbol:" + beginSymbol.toString());
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
    void showSelect() {
        System.out.println("select are as followed.");
        for(Select select:selects) {
            System.out.print(select.getLeft() + "\t=\t");
            for (Terminal t :
                    select.getRight()) {
                System.out.print(t.toString() + " ");
            }
            System.out.println();
        }
    }

    void showTerminal() {
        System.out.println("there are terminals:");
        for(Terminal t:terminals) {
            System.out.println(t.toString());
        }
    }

    void showNonTerminal() {
        System.out.println("there are non-termianls:");
        for(NonTerminal nt:nonTerminals) {
            System.out.println(nt.toString());
        }
    }

    void showProduction() {
        System.out.println("there are productions.");
        for(Production production:productions) {
            System.out.println(production);
        }
    }

    public void program(String input, String grammar) {
        //1.generate production from grammar
        this.geneProduction(grammar);
        this.showTerminal();
        this.showNonTerminal();
        this.showProduction();
        //2.generate First set from production
        this.geneFirst();
        this.showFirst();
        //3.generate Follow set from First and production
        this.geneFollow();
        this.showFollow();
        //4.generate Select set from First, Follow and production
        this.geneSelects();
        this.showSelect();
        //5.generate analysis table from select set
        this.geneAnalysisTable();
        this.showAnalysisTable(10);
        this.showBeginSymbol();
        //6.adjust the input string's legal
        System.out.println(this.isLegal(input));
    }
    public static void main(String[] args) {
        LL ll = new LL();
        String input = "a*a+a";
//        String grammar = "S AaS\nS BbS\nS d\nA a\nB $\nB c";
        String grammar = "E TQ\nQ +TQ\nQ $\nT FW\nW *FW\nW $\nF (E)\nF a";
        ll.geneProduction(grammar);
        ll.showTerminal();
        ll.showNonTerminal();
        ll.showProduction();
        ll.geneFirst();
        ll.showFirst();
        ll.geneFollow();
        ll.showFollow();
        ll.geneSelects();
        ll.showSelect();
        ll.geneAnalysisTable();
        ll.showAnalysisTable(10);

        ll.showBeginSymbol();
        System.out.println(ll.isLegal(input));
    }
}
