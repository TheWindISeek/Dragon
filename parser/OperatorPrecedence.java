package parser;

import java.util.*;

/**
 * @fileEncoding utf-8
 * @author JeffreySharp
 * @apiNote 算符优先分析程序
 *
 * 算符优先算法很明显会出现假真的情况
 * 因为他根本没有比较产生式是否相等
 * 而是在当前算符的优先级下是否是正确的
 *
 * 注意!
 *
 * 相当于我给定的产生式
 * 他根据这个产生式自动给我生成了算符的优先级表
 * 然后根据这个优先级表 对之后的输入串进行了分析
 *
 * 构建的过程分下几步
 * 首先我需要一个分析和识别的算符
 * analysis
 *      k <= 1
 *      S[k] <= #
 *      while(flag) {
 *          a = nextChar()
 *          while(eqFlag) {
 *              if(s[k] belong to Terminal) {
 *                  j = k
 *              } else {
 *                  j = k -1
 *              }
 *
 *              if(s[j] > a) {
 *                 do{
 *                     q = s[j]
 *                     if(s[j-1] belong to Terminal) {
 *                         j = j - 1
 *                     } else {
 *                         j = j - 2
 *                     }
 *                 } while(!(s[j] < Q));
 *
 *                 s[j+1]...s[k] 归约为N
 *                 k = j+1
 *                 s[k] = N
 *              } else {
 *                  if(s[j] < a) {
 *                      k = k + 1
 *                      s[k] = a
 *                      break;
 *                  } else {
 *                      if(s[i] = a) {
 *                          if(s[j] = a) {
 *                              if(s[j] = #) {
 *                                  if(index == string.length) {
 *                                      return true;
 *                                  } else {
 *                                      return false;
 *                                  }
 *                              } else {
 *                                  k = k + 1
 *                                  s[k] = a
 *                                  break;
 *                              }
 *                          }
 *                      } else {
 *                          return false
 *                      }
 *                  }
 *              }
 *          }
 *      }
 *
 * 然后是通过firstvt和lastvt 生成分析表的算法的算法
 * construct
 *      for A -> X1 X2 ... Xn
 *          for i = 1 to n-1
 *              if Xi belong to Terminal && Xi+1 belong to Terminal
 *                 set(Xi = Xi+1)
 *              if i <= n-2 && Xi belong to Terminal && Xi+2 belong to Terminal && Xi+1 belong to Non-Terminal
 *                  set(Xi = Xi+2)
 *              if Xi belong to Terminal && Xi+1 belong to Non-Terminal
 *                  for Terminal b : firstvt(Xi+1)
 *                      set(Xi < b)
 *               if Xi belong to Non-Terminal && Xi+1 belong to Terminal
 *                  for Terminal b : lastvt(Xi)
 *                      set(b > Xi+1)
 *
 * 生成firstvt的算法
 * insert(A, a)
 *      if !f[iA, ia]
 *          f[iA, ia] = true;
 *          stack.push({A, a})
 *
 * firstvtConstruct
 *      for i = 1 to m
 *          for j = 1 to n
 *              f[iA, ja] = false
 *
 *       for A -> a... || A -> Ba...
 *          insert(A, a)
 *
 *       while(!stack.empty)
 *          (B,a) = stack.top
 *          for A -> B...
 *              insert(A, a)
 *
 * lastvtConstruct
 *      for i = 1 to m
 *          for j = 1 to n
 *              f[iA, ja] = false
 *
 *       for A -> ...a || A -> ...aB
 *          insert(A, a)
 *
 *       while(!stack.empty)
 *          (B,a) = stack.top
 *          for A -> ...B
 *              insert(A, a)
 *
 * 需要用到的数据结果
 * firstVtMap:
 *      NonTerminal, Set<Terminal>
 *      存放对应non-terminal下的它所对应的terminal
 * lastVtMap
 *      NonTerminal, Set<Terminal>
 *      存放对应非终结符号的对应的terminal
 *
 *
 * tables
 *      Integer, Action
 *      Action = {
 *          LE
 *          EQ
 *          GE
 *      }
 *      Integer transDim(Terminal a, Terminal b)
 *      存放分析的结果表
 *
 * nonTerminals
 *      Integer, NonTerminal
 *      通过int访问非终结符号
 *
 * terminals
 *      Integer, Terminal
 *      通过int访问终结符号
 *
 * 当然也可以不这么做
 * 也就是我直接做一个ArrayList<NonTerminal> 和 ArrayList<terminal>
 *     然后用下标去生成
 *
 *
 * 既然都这样了 那我再写得花一点
 * 每次栈压入的不是(B,a)
 * 而是压入的 B * n + a
 *
 * 这样是有效的
 * 因为 0 <= B < m, 0 <= a < n
 *
 * 所以当我弹出之后
 * 可以直接
 *      B = nonTerminalArray.indexAt(top/n)
 *      a = terminalArray.indexAt(top%n)
 *      获取到
 */
public class OperatorPrecedence {
    //三种状态
    private static final int LE = 0, EQ = 1, GE = 2;

    //增广文法的开始符号
    NonTerminal beginSymbol = NonTerminal.Begin;

    //用于收集终结符和非终结符号
    private ArrayList<NonTerminal> nonTerminals = new ArrayList<>();
    private ArrayList<Terminal> terminals = new ArrayList<>();
    private int n, m;//终结符 和 非终结符号的个数
    //给定非终结符号和终结符号后能够映射到数字 以便于之后的访问
    private Map<NonTerminal, Integer> nonTerminalMap = new HashMap<>();
    private Map<Terminal, Integer> terminalMap = new HashMap<>();
    private Set<Production> productions = new HashSet<>();

    //非终结符对应终结符的集合 用于找到last和first
    private Map<NonTerminal, Set<Terminal> > firstNtMap = new HashMap<>();
    private Map<NonTerminal, Set<Terminal> > lastNtMap = new HashMap<>();

    //用于最后的分析过程
    private Map<Integer, Integer> tables = new HashMap<>();

    /**
     * 输入两个终结符 返回他们映射后得到的结果 用于tables项的获取
     * @param a 终结符a
     * @param b 终结符b
     * @return (a<<8) + b
     */
    Integer transDim(Terminal a, Terminal b) {
        return (a.getIntValue() << 8) + b.getIntValue();
    }

    /**
     * 查找字符c对应的符号
     * @param c 给定的字符c
     * @return
     */
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
     * 根据输入的文法得到产生式 终结符 和 非终结符
     * @param grammar 输入的文法形式为 "A b\nA d" 首位不能有多余的空格
     */
    public void geneProduction(String grammar) {
        String[] splits = grammar.split("\n");

        StringBuffer symbols = new StringBuffer();
        int index = 1;
        for (String s_production:splits) {
            String[] strings = s_production.split(" ");
            String left = strings[0];
            String right = strings[1];

            Symbol symbol = getSymbols(left.charAt(0));

            //非终结符号未被收录
            if(!(symbol instanceof NonTerminal)) {
                symbol = new NonTerminal(left.charAt(0));

                //进行文法的增广
                if(nonTerminals.size() == 0) {
                    Production production = new Production(beginSymbol, "#"+left+"#", index++);
                    productions.add(production);
                }

                nonTerminals.add((NonTerminal) symbol);
            }

            //添加产生式和右侧字符串
            productions.add(new Production((NonTerminal) symbol, right, index));
            symbols.append(right);
        }

        //add terminal
        for (int i = 0; i < symbols.length(); i++) {
            char c = symbols.charAt(i);
            Symbol symbol = getSymbols(c);

            if(symbol instanceof NonTerminal) continue;
            if(c == '$') continue; // 跳过空的处理

            //if not found
            if(symbol.equals(Terminal.End)) {
                terminals.add(new Terminal(c));
            }
        }

        //手动添加 # 终结符号
        terminals.add(Terminal.End);
        nonTerminals.add(beginSymbol);
    }

    private void geneMap() {
        n = terminals.size();m = nonTerminals.size();
        for(int i = 0; i < n; ++i) {
            terminalMap.put(terminals.get(i), i);
        }

        for(int i = 0; i < m; ++i) {
            nonTerminalMap.put(nonTerminals.get(i), i);
        }
    }

    private void  insert(int iA, int ia, boolean[][] f, Stack<Integer> stack) {
        if(!f[iA][ia]) {
            f[iA][ia] = true;
            stack.push(iA * n + ia);
        }
    }
    private void geneFirstVt() {
        boolean[][] f = new boolean[m][n];//m * n 的bool矩阵
        Stack<Integer> stack = new Stack<Integer>();

        for(Production production: productions) {
            Symbol first = getSymbols(production.getRight().charAt(0));

            if(first instanceof Terminal) {
                insert(nonTerminalMap.get(production.getLeft()) , terminalMap.get((Terminal) first),f,stack);
            } else {
                //还可能存在 Ba的情况
                if(production.getRight().length() >= 2) {
                    first = getSymbols(production.getRight().charAt(1));
                    if(first instanceof Terminal) {
                        insert(nonTerminalMap.get(production.getLeft()) , terminalMap.get((Terminal) first),f,stack);
                    }
                }
            }
        }

        //栈中已有数据 继续操作
        while (!stack.empty()) {
            Integer pop = stack.pop();//获取 (B, a)
            NonTerminal B = nonTerminals.get(pop/n);
            int a = pop%n;

            for(Production production: productions) {
                Symbol first = getSymbols(production.getRight().charAt(0));
                if(B.equals(first)) {//insert(A, a)
                    insert(nonTerminalMap.get(production.getLeft()), a, f, stack);
                }
            }
        }

        for(int i = 0; i < m; ++i) {
            Set<Terminal> set = new HashSet<>();
            for(int j = 0; j < n; ++j) {
                if(f[i][j])//将为true的添加到对应first集合中去
                    set.add(terminals.get(j));
            }
            firstNtMap.put(nonTerminals.get(i), set);
        }
    }

    private void geneLastVt() {
        boolean[][] f = new boolean[m][n];//m * n 的bool矩阵
        Stack<Integer> stack = new Stack<Integer>();

        for(Production production: productions) {
            Symbol last = getSymbols(production.getRight().charAt(production.getRight().length()-1));

            if(last instanceof Terminal) {
                insert(nonTerminalMap.get(production.getLeft()) , terminalMap.get((Terminal) last),f,stack);
            } else {
                //还可能存在 Ba的情况
                if(production.getRight().length() >= 2) {
                    last = getSymbols(production.getRight().charAt(production.getRight().length()-2));
                    if(last instanceof Terminal) {
                        insert(nonTerminalMap.get(production.getLeft()) , terminalMap.get((Terminal) last),f,stack);
                    }
                }
            }
        }

        //栈中已有数据 继续操作
        while (!stack.empty()) {
            Integer pop = stack.pop();//获取 (B, a)
            NonTerminal B = nonTerminals.get(pop/n);
            int a = pop%n;

            for(Production production: productions) {
                Symbol first = getSymbols(production.getRight().charAt(production.getRight().length()-1));
                if(B.equals(first)) {//insert(A, a)
                    insert(nonTerminalMap.get(production.getLeft()), a, f, stack);
                }
            }
        }

        for(int i = 0; i < m; ++i) {
            Set<Terminal> set = new HashSet<>();
            for(int j = 0; j < n; ++j) {
                if(f[i][j])//将为true的添加到对应first集合中去
                    set.add(terminals.get(j));
            }
            lastNtMap.put(nonTerminals.get(i), set);
        }
    }

    private void geneTables() {
        for(Production production: productions) {
            int length = production.getRight().length();
            for(int i = 0; i < length-1; ++i) {
                Symbol xi = getSymbols(production.getRight().charAt(i));
                Symbol xip1 = getSymbols(production.getRight().charAt(i+1));

                if(xi instanceof Terminal && xip1 instanceof Terminal) {
                    tables.put(transDim((Terminal) xi, (Terminal) xip1), EQ);
                }

                if(i < length-2) {
                    Symbol xip2 = getSymbols(production.getRight().charAt(i+2));
                    if(xi instanceof Terminal && xip2 instanceof Terminal && xip1 instanceof NonTerminal) {
                        tables.put(transDim((Terminal) xi, (Terminal) xip2), EQ);
                    }
                }

                if(xi instanceof Terminal && xip1 instanceof NonTerminal) {
                    for(Terminal b: firstNtMap.get(xip1)) {
                        tables.put(transDim((Terminal) xi, b), LE);
                    }
                }

                if(xi instanceof NonTerminal && xip1 instanceof Terminal) {
                    for(Terminal b : lastNtMap.get(xi)) {
                        tables.put(transDim(b, (Terminal) xip1), GE);
                    }
                }
            }
        }
    }

    /**
     * 判断给定终结符号t 和 c 他们的关系 action(t,c) 是否成立
     * @param t 左边的终结符号
     * @param terminal 右边的字符
     * @param action 给定的关系
     * @return 成立则返回true
     */
    private boolean isAction(Terminal t, Terminal terminal, int action) {
        int dim = transDim(t, terminal);
        Integer integer = tables.get(dim);
        if(integer == null) return false;//希望不会出现这种情况
        return integer == action;
    }
    /**
     * 分析函数 根据之前生成的符号优先表对输入串input进行分析
     * @param input 输入串
     * @return 是否符号之前的规则
     */
    private boolean analysis(String input) {
        char c;
        int k = 0, index = 0, j;
        Symbol temp;
        Terminal a;
        ArrayList<Symbol> symbols = new ArrayList<>();
        symbols.add(Terminal.End);

        System.out.println("开始进行分析");

        while (true) {
            c = index < input.length() ? input.charAt(index++) : '#';
            a = (Terminal) getSymbols(c);
            while (true) {
                if (symbols.get(k) instanceof Terminal) {
                    j = k;
                } else {
                    j = k - 1;
                }

                if (isAction((Terminal) symbols.get(j), a, GE)) {
                    System.out.printf("%c > %c ", symbols.get(j).getValue(), a.getValue());

                    do {
                        temp = symbols.get(j);

                        if (symbols.get(j - 1) instanceof Terminal) {
                            j = j - 1;
                        } else {
                            j = j - 2;
                        }
                    } while (!(isAction((Terminal) symbols.get(j), (Terminal) temp, LE)));
                    System.out.printf(" j+1:%d  k:%d\n", j+1, k);
                    //这里算是最可能出错的地方
                    for (int i = 0; i < k - j; ++i) {
                        symbols.remove(j + 1);
                    }
                    k = j + 1;
                    symbols.add(k, NonTerminal.NON_TERMINAL);
                } else {
                    if (isAction((Terminal) symbols.get(j), a, LE)) {
                        System.out.printf("%c < %c\n", symbols.get(j).getValue(), a.getValue());

                        k = k + 1;
                        symbols.add(k, a);
                        break;
                    } else {
                        if (isAction((Terminal) symbols.get(j), a, EQ)) {
                            System.out.printf("%c = %c\n", symbols.get(j).getValue(), a.getValue());

                            if (symbols.get(j).equals(Terminal.End)) {
                                if (index == input.length()) {
                                    return true;
                                } else {
                                    return false;
                                }
                            } else {
                                k = k + 1;
                                symbols.add(k, a);
                                break;
                            }
                        } else {
                            return false;
                        }
                    }
                }

            }
        }
    }

    //以下为打印系列函数
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

    void showFirstVt() {
        System.out.println("there are firstvt.");
        show(firstNtMap);
    }

    void showLastVt() {
        System.out.println("there are lastvt.");
        show(lastNtMap);
    }

    private void show(Map<NonTerminal, Set<Terminal>> lastNtMap) {
        for(Map.Entry<NonTerminal, Set<Terminal>> entry : lastNtMap.entrySet()) {
            System.out.printf("%c = ", entry.getKey().getValue());
            for(Terminal terminal: entry.getValue()) {
                System.out.printf("%c ", terminal.getValue());
            }
            System.out.println();
        }
    }

    /**
     * 获取action对应的char字符
     * @param action
     * @return
     */
    private char getAction(int action) {
        switch (action) {
            case EQ:
                return '=';
            case LE:
                return '<';
            case GE:
                return '>';
        }
        return ' ';
    }

    private void showTable() {
        System.out.println("this is the table.");

        //表头
        System.out.printf(" \t");
        for (int i = 0; i < n; ++i) {
            System.out.printf("%c\t", terminals.get(i).getValue());
        }
        System.out.printf("\n");

        //内容
        for(int i = 0; i < n; ++i) {
            System.out.printf("%c\t", terminals.get(i).getValue());
            for(int j = 0; j < n; ++j) {
                int dim = transDim(terminals.get(i), terminals.get(j));
                if(tables.containsKey(dim)) {
                    System.out.printf("%c\t", getAction(tables.get(dim)));
                } else {
                    System.out.printf(" \t");
                }
            }
            System.out.printf("\n");
        }
    }

    //测试和工作函数
    private void test(String input, String grammar) {
        geneProduction(grammar);
        showTerminal();
        showNonTerminal();
        showProduction();

        geneMap();
        geneFirstVt();
        showFirstVt();
        geneLastVt();
        showLastVt();

        geneTables();
        showTable();

        System.out.println(analysis(input));
    }


    public boolean program(String input, String grammar) {
        geneProduction(grammar);
        showTerminal();
        showNonTerminal();
        showProduction();

        geneMap();
        geneFirstVt();
        showFirstVt();
        geneLastVt();
        showLastVt();

        geneTables();
        showTable();

        return analysis(input);
    }

    public static void main(String[] args) {
        OperatorPrecedence op = new OperatorPrecedence();

        /**
         *  E E+T
         *  E T
         *  T T*F
         *  T F
         *  F P!F
         *  F P
         *  P (E)
         *  P i
         */
        String grammar = "E E+T\n" +
                        "E T\n" +
                        "T T*F\n" +
                        "T F\n" +
                        "F P!F\n" +
                        "F P\n" +
                        "P (E)\n" +
                        "P i";
        String input = "i+i";
        input = "i+i*(i)!i*i";

        op.test(input, grammar);
    }
}
