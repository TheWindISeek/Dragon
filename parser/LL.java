package parser;

import java.util.*;

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

    // transform two indexes to one index
    Integer transDim(NonTerminal nonTerminal, Terminal terminal) {
        //show index
        System.out.println((nonTerminal.getIntValue() << 8 + terminal.getIntValue()));
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
        symbolStack.push(getSymbols('S'));
        //the top of Stack
        Symbol X;

        while (flag) {
            X = symbolStack.pop();

            System.out.println("X‘s value:" + X.getValue() + " a'value:" + a);

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
        String input = "bdddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd";


        //1. init nonTerminals, terminals, productions
        //All nonTerminals
        NonTerminal S = new NonTerminal('S');
        NonTerminal A = new NonTerminal('A');
        NonTerminal B = new NonTerminal('B');
        //add to non-terminals
        ll.nonTerminals.add(S);
        ll.nonTerminals.add(A);
        ll.nonTerminals.add(B);

        //All Terminal
        Terminal a = new Terminal('a');
        Terminal b = new Terminal('b');
        Terminal c = new Terminal('c');
        Terminal d = new Terminal('d');
        //add to terminals
        ll.terminals.add(a);
        ll.terminals.add(b);
        ll.terminals.add(c);
        ll.terminals.add(d);
        ll.terminals.add(Terminal.End);
        ll.terminals.add(Terminal.Empty);

        //All Productions
        Production SAaS = new Production(S,"AaS", 1);
        Production SBbS = new Production(S, "BbS", 2);
        Production Aa = new Production(B, "a", 3);
        Production B_ = new Production(B, "$", 4);
        Production Bc = new Production(B, "c", 5);
        Production Sd = new Production(S, "d", 6);
        //add to productions
        ll.productions.add(SAaS);
        ll.productions.add(SBbS);
        ll.productions.add(Aa);
        ll.productions.add(B_);
        ll.productions.add(Bc);
        ll.productions.add(Sd);


        //2. we have firsts, follows
        First first_AaSa = new First("AaS");
        first_AaSa.Add(a);
        First first_BbS = new First("BbS");
        first_BbS.Add(c);
        first_BbS.Add(b);
        First first_d = new First("d");
        first_d.Add(d);
        First first_a = new First("a");
        first_a.Add(a);
        First first_sigma = new First("$");
        first_sigma.Add(Terminal.Empty);
        First first_c = new First("c");
        first_c.Add(c);
        //add to firsts
        ll.firsts.add(first_AaSa);
        ll.firsts.add(first_BbS);
        ll.firsts.add(first_d);
        ll.firsts.add(first_a);
        ll.firsts.add(first_c);

        Follow follow_S = new Follow(S, Terminal.End);
        Follow follow_A = new Follow(A, a);
        Follow follow_B = new Follow(B, b);
        //add to follows
        ll.follows.add(follow_S);
        ll.follows.add(follow_A);
        ll.follows.add(follow_B);



        //3.we even have Select
        Select select_SAaS = new Select(SAaS);
        select_SAaS.Add(a);
        Select select_SBbS = new Select(SBbS);
        select_SBbS.Add(c);
        select_SBbS.Add(b);
        Select select_Sd = new Select(Sd);
        select_Sd.Add(d);
        Select select_Aa = new Select(Aa);
        select_Aa.Add(a);
        Select select_B_ = new Select(B_);
        select_B_.Add(b);
        Select select_Bc = new Select(Bc);
        select_Bc.Add(c);
        //add to selects
        ll.selects.add(select_SAaS);
        ll.selects.add(select_SBbS);
        ll.selects.add(select_Sd);
        ll.selects.add(select_Aa);
        ll.selects.add(select_B_);
        ll.selects.add(select_Bc);

        //4.analysis table!!!
        Integer integer_Sa = ll.transDim(S, a);
        Integer integer_Sb = ll.transDim(S, b);
        Integer integer_Sc = ll.transDim(S, c);
        Integer integer_Sd = ll.transDim(S, d);
        Integer integer_Aa = ll.transDim(A, a);
        Integer integer_Bb = ll.transDim(B, b);
        Integer integer_Bc = ll.transDim(B, c);

        ll.analysisTable.put(integer_Sa, SAaS);
        ll.analysisTable.put(integer_Sb, SBbS);
        ll.analysisTable.put(integer_Sc, SBbS);
        ll.analysisTable.put(integer_Sd, Sd);
        ll.analysisTable.put(integer_Aa, Aa);
        ll.analysisTable.put(integer_Bb, B_);
        ll.analysisTable.put(integer_Bc, Bc);



        //5.adjust the result
        System.out.println(ll.isLegal(input));
    }
}
