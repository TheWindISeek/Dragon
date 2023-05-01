package parser;

import java.util.*;
import java.util.stream.Stream;

/**
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

    Map<Symbol, First> firstMap = new HashMap<>();
    Map<NonTerminal, Follow> followMap = new HashMap<>();
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
                copySet(rightFirst.getRight(),first.getRight());
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
     * generate follow set from first set
     *      *        First first_AaSa = new First("AaS");
     *      *         first_AaSa.Add(a);
     *      *         First first_BbS = new First("BbS");
     *      *         first_BbS.Add(c);
     *      *         first_BbS.Add(b);
     *      *         First first_d = new First("d");
     *      *         first_d.Add(d);
     *      *         First first_a = new First("a");
     *      *         first_a.Add(a);
     *      *         First first_sigma = new First("$");
     *      *         first_sigma.Add(Terminal.Empty);
     *      *         First first_c = new First("c");
     *      *         first_c.Add(c);
     *      *         //add to firsts
     *      *         ll.firsts.add(first_AaSa);
     *      *         ll.firsts.add(first_BbS);
     *      *         ll.firsts.add(first_d);
     *      *         ll.firsts.add(first_a);
     *      *         ll.firsts.add(first_c);
     *      *         ll.first.add(first_sigma);
     *      *
     *      *         Follow follow_S = new Follow(S, Terminal.End);
     *      *         Follow follow_A = new Follow(A, a);
     *      *         Follow follow_B = new Follow(B, b);
     *      *         //add to follows
     *      *         ll.follows.add(follow_S);
     *      *         ll.follows.add(follow_A);
     *      *         ll.follows.add(follow_B);
     */
    public void geneFollow() {
        beginSymbol = getBeginSymbol();

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
        }

        //to follow list
        for(Map.Entry<NonTerminal,Follow> followEntry:followMap.entrySet()) {
            follows.add(followEntry.getValue());
        }
    }
    /**
     * generate selects set from first set and follow set
     *        First first_AaSa = new First("AaS");
     *         first_AaSa.Add(a);
     *         First first_BbS = new First("BbS");
     *         first_BbS.Add(c);
     *         first_BbS.Add(b);
     *         First first_d = new First("d");
     *         first_d.Add(d);
     *         First first_a = new First("a");
     *         first_a.Add(a);
     *         First first_sigma = new First("$");
     *         first_sigma.Add(Terminal.Empty);
     *         First first_c = new First("c");
     *         first_c.Add(c);
     *         //add to firsts
     *         ll.firsts.add(first_AaSa);
     *         ll.firsts.add(first_BbS);
     *         ll.firsts.add(first_d);
     *         ll.firsts.add(first_a);
     *         ll.firsts.add(first_c);
     *
     *         Follow follow_S = new Follow(S, Terminal.End);
     *         Follow follow_A = new Follow(A, a);
     *         Follow follow_B = new Follow(B, b);
     *         //add to follows
     *         ll.follows.add(follow_S);
     *         ll.follows.add(follow_A);
     *         ll.follows.add(follow_B);
     *
     *         Select select_SAaS = new Select(SAaS);
     *         select_SAaS.Add(a);
     *         Select select_SBbS = new Select(SBbS);
     *         select_SBbS.Add(c);
     *         select_SBbS.Add(b);
     *         Select select_Sd = new Select(Sd);
     *         select_Sd.Add(d);
     *         Select select_Aa = new Select(Aa);
     *         select_Aa.Add(a);
     *         Select select_B_ = new Select(B_);
     *         select_B_.Add(b);
     *         Select select_Bc = new Select(Bc);
     *         select_Bc.Add(c);
     *         //add to selects
     *         ll.selects.add(select_SAaS);
     *         ll.selects.add(select_SBbS);
     *         ll.selects.add(select_Sd);
     *         ll.selects.add(select_Aa);
     *         ll.selects.add(select_B_);
     *         ll.selects.add(select_Bc);
     */
    public void geneSelects() {
        for (Production production: productions) {
            Select select = new Select(production);
            for (First first :firsts) {
                if(first.getLeft().equals(production.getRight().toString())) {
                    //find the production's first
                    if(first.contain(Terminal.Empty)) {
                        //select(production) = first(production)-sigma + follow(production)
                        for (Terminal t: first.getRight()) {
                            if(t.equals(Terminal.Empty)) continue;
                            select.add(t);
                        }

                        for (Follow follow : follows) {
                            if (production.getLeft().equals(follow.getLeft())) {
                                for(Terminal t: follow.getRight()) {
                                    select.add(t);
                                }
                            }
                        }
                    } else {
                        //select(production) = first(production)
                        for(Terminal t:first.getRight()) {
                            select.add(t);
                        }
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
     *
     *         Integer integer_Sa = ll.transDim(S, a);
     *         Integer integer_Sb = ll.transDim(S, b);
     *         Integer integer_Sc = ll.transDim(S, c);
     *         Integer integer_Sd = ll.transDim(S, d);
     *         Integer integer_Aa = ll.transDim(A, a);
     *         Integer integer_Bb = ll.transDim(B, b);
     *         Integer integer_Bc = ll.transDim(B, c);
     *
     *         ll.analysisTable.put(integer_Sa, SAaS);
     *         ll.analysisTable.put(integer_Sb, SBbS);
     *         ll.analysisTable.put(integer_Sc, SBbS);
     *         ll.analysisTable.put(integer_Sd, Sd);
     *         ll.analysisTable.put(integer_Aa, Aa);
     *         ll.analysisTable.put(integer_Bb, B_);
     *         ll.analysisTable.put(integer_Bc, Bc);
     *
     *         Select select_SAaS = new Select(SAaS);
     *         select_SAaS.Add(a);
     *         Select select_SBbS = new Select(SBbS);
     *         select_SBbS.Add(c);
     *         select_SBbS.Add(b);
     *         Select select_Sd = new Select(Sd);
     *         select_Sd.Add(d);
     *         Select select_Aa = new Select(Aa);
     *         select_Aa.Add(a);
     *         Select select_B_ = new Select(B_);
     *         select_B_.Add(b);
     *         Select select_Bc = new Select(Bc);
     *         select_Bc.Add(c);
     *         //add to selects
     *         ll.selects.add(select_SAaS);
     *         ll.selects.add(select_SBbS);
     *         ll.selects.add(select_Sd);
     *         ll.selects.add(select_Aa);
     *         ll.selects.add(select_B_);
     *         ll.selects.add(select_Bc);
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
                if(!t.equals(Terminal.End) && !t.equals(Terminal.Empty)) {
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
    public static void main(String[] args) {
        /*
        Terminal terminal = new Terminal('a');
        Terminal terminal1 = new Terminal('a');
        System.out.println(terminal1 == terminal);
        System.out.println(terminal1.equals(terminal));
        Set<Terminal> set = new HashSet<>();
        Map<Character, Terminal> map = new HashMap<Character,Terminal>();
        map.put('a', terminal);
        System.out.println(map.get('a').equals(terminal1));

        set.add(terminal);
        System.out.println(set.contains(terminal1));

        NonTerminal nonTerminal = new NonTerminal('S');
        Production production = new Production(nonTerminal,"as",1);
        System.out.println(production.getLeft() == nonTerminal);
        System.out.println(production.getLeft().equals(nonTerminal));

        First first = new First("S");
        first.Add(terminal);
        System.out.println(first.Contain(terminal));
        System.out.println(first.Contain(terminal1));

        Select select = new Select(production);
        Set<Select> selects = new HashSet<>();
        selects.add(select);
        */
        LL ll = new LL();
        String input = "aabd";
        String grammar = "S AaS\nS BbS\nS d\nA a\nB $\nB c";
//
//        //1. init nonTerminals, terminals, productions
//        //All nonTerminals
//        NonTerminal S = new NonTerminal('S');
//        NonTerminal A = new NonTerminal('A');
//        NonTerminal B = new NonTerminal('B');
//        //add to non-terminals
//        ll.nonTerminals.add(S);
//        ll.nonTerminals.add(A);
//        ll.nonTerminals.add(B);
//
////        ll.nonTerminals.add(NonTerminal.Begin);
//
//        //All Terminal
//        Terminal a = new Terminal('a');
//        Terminal b = new Terminal('b');
//        Terminal c = new Terminal('c');
//        Terminal d = new Terminal('d');
//        //add to terminals
//        ll.terminals.add(a);
//        ll.terminals.add(b);
//        ll.terminals.add(c);
//        ll.terminals.add(d);
//        ll.terminals.add(Terminal.End);
//        ll.terminals.add(Terminal.Empty);
//
//        //All Productions
//        Production SAaS = new Production(S,"AaS", 1);
//        Production SBbS = new Production(S, "BbS", 2);
//        Production Aa = new Production(A, "a", 3);
//        Production B_ = new Production(B, "$", 4);
//        Production Bc = new Production(B, "c", 5);
//        Production Sd = new Production(S, "d", 6);
//
////        Production S_S = new Production(NonTerminal.Begin,S.toString(),7);
////        Production S_A = new Production(NonTerminal.Begin,A.toString(),8);
////        Production S_B = new Production(NonTerminal.Begin,B.toString(),9);
//
//
//        //add to productions
//        ll.productions.add(SAaS);
//        ll.productions.add(SBbS);
//        ll.productions.add(Aa);
//        ll.productions.add(B_);
//        ll.productions.add(Bc);
//        ll.productions.add(Sd);

//        ll.productions.add(S_A);
//        ll.productions.add(S_B);
//        ll.productions.add(S_S);
        ll.geneProduction(grammar);
        ll.showTerminal();
        ll.showNonTerminal();
        ll.showProduction();
        //2. we have firsts, follows
//        First first_AaSa = new First("AaS");
//        first_AaSa.add(a);
//        First first_BbS = new First("BbS");
//        first_BbS.add(c);
//        first_BbS.add(b);
//        First first_d = new First("d");
//        first_d.add(d);
//        First first_a = new First("a");
//        first_a.add(a);
//        First first_sigma = new First("$");
//        first_sigma.add(Terminal.Empty);
//        First first_c = new First("c");
//        first_c.add(c);
//        //add to firsts
//        ll.firsts.add(first_AaSa);
//        ll.firsts.add(first_BbS);
//        ll.firsts.add(first_d);
//        ll.firsts.add(first_a);
//        ll.firsts.add(first_c);
//        ll.firsts.add(first_sigma);
        ll.geneFirst();
        ll.showFirst();
        ll.geneFollow();
        ll.showFollow();
//        Follow follow_S = new Follow(S, Terminal.End);
//        Follow follow_A = new Follow(A, a);
//        Follow follow_B = new Follow(B, b);
//        //add to follows
//        ll.follows.add(follow_S);
//        ll.follows.add(follow_A);
//        ll.follows.add(follow_B);

        //3.we even have Select
//        Select select_SAaS = new Select(SAaS);
//        select_SAaS.add(a);
//        Select select_SBbS = new Select(SBbS);
//        select_SBbS.add(c);
//        select_SBbS.add(b);
//        Select select_Sd = new Select(Sd);
//        select_Sd.add(d);
//        Select select_Aa = new Select(Aa);
//        select_Aa.add(a);
//        Select select_B_ = new Select(B_);
//        select_B_.add(b);
//        Select select_Bc = new Select(Bc);
//        select_Bc.add(c);
//        //add to selects
//        ll.selects.add(select_SAaS);
//        ll.selects.add(select_SBbS);
//        ll.selects.add(select_Sd);
//        ll.selects.add(select_Aa);
//        ll.selects.add(select_B_);
//        ll.selects.add(select_Bc);
        ll.geneSelects();
        ll.showSelect();

        //4.analysis table!!!
//        Integer integer_Sa = ll.transDim(S, a);
//        Integer integer_Sb = ll.transDim(S, b);
//        Integer integer_Sc = ll.transDim(S, c);
//        Integer integer_Sd = ll.transDim(S, d);
//        Integer integer_Aa = ll.transDim(A, a);
//        Integer integer_Bb = ll.transDim(B, b);
//        Integer integer_Bc = ll.transDim(B, c);
//
//        ll.analysisTable.put(integer_Sa, SAaS);
//        ll.analysisTable.put(integer_Sb, SBbS);
//        ll.analysisTable.put(integer_Sc, SBbS);
//        ll.analysisTable.put(integer_Sd, Sd);
//        ll.analysisTable.put(integer_Aa, Aa);
//        ll.analysisTable.put(integer_Bb, B_);
//        ll.analysisTable.put(integer_Bc, Bc);
        ll.geneAnalysisTable();
        ll.showAnalysisTable(10);

        ll.showBeginSymbol();
        //5.adjust the result
        System.out.println(ll.isLegal(input));
    }
}
