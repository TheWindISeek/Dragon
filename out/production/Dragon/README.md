# 任务

[toc]

## 编译原理实验报告

### 实验目的
设计、编制、调试一个典型的语法分析程序，实现对词法分析程序所提供的单词序列进行语法检查和结构分析，进一步掌握常用的语法分析方法。
### 实验要求 
(1）根据 LL（1）分析法总控制流程图，编写一个语法分析程序。
可根据自己的能力选择以下三项之一作为分析算法中的输入：
	(a）直接输入根据已知文法人工构造的分析表 M。
	(b）输入己知文法的集合 FIRST(x）和 FOLLOW(U），由程序自动生成该文法的分析表 M。
	(c）输入已知文法，由程序自动生成该文法分析表 M。
(2）程序具有通用性，即所编制的 LL(1）语法分析程序能够适用于不同文法以及各种输入单词串，并能判断该文法是否为算符文法和算符优先文法。
(3）有运行实例。对于输入的一个文法和一个单词串，所编制的语法分析程序应能正确地判断，此单词串是否为该文法的句子，并要求输出分析过程。



### 实验过程与结果
本实验所有代码可在 https://www.github.com/TheWindISeek/Dragon.git 查看
其中Main包中main函数为主函数，其余代码中的main函数被用于测试。
本次实验完成的是实验要求中的C，即用户提供两个字符串，Input和grammar。一个作为用于识别的串，一个作为文法串。输出的结果将包含构造过程中的产生的first集合、follow集合、select集合、分析表等信息，以及input是否符合文法grammar。
目前代码中有很多的地方可以继续优化。如数据结构的使用上，函数的调用上，变量的申请，输入文法的限制等等。但本实验的目的是为了练习LL(1)文法，这些先暂且不做。

#### 算法描述

核心算法有4个。分别为，构造first集合，构造follow集合，构造select集合，表驱动LL(1)预测分析算法。
以下给出简要算法描述。
```c
First

将所有属于终结符的符号所归属的first集合置为其本身

对于所有形如 X -> a.. 的产生式
	first(X) += {a}
	first(a..) = {a}

do
	对于所有形如 X -> Y..的产生式
		first(Y..) += first(Y)
		Yi = Y
		while first(Yi) 存在 空(sigma) && X -> Y Y1 Y2 ...Yi...Yk
			first(Y..) += first(Yi)
			Yi = Yi+1
		if Yk 存在 空(sigma)
			first(Y..) += {空}
		first(X) += first(Y..)
 until 所有的first集合不再发生变化
```

```c
Follow

为所有非终结符生成对应的Follow集合
为文法的开始符号S在对应的Follow集合中添加 # (输入串结束符号)


do
	对所有产生式X -> ...Y...
		index = 0
		while index < 产生式右部长度
			找到产生式右部中从 index 开始的第一个非终结符号 Y
				if 该非终结符号右部存在符号Y1
					Follow(Y) += First(Y1)
				else
					Follow(Y) += Follow(X)
			index++;
until 所有的Follow集合不再发生变化
```

```c
Select

对于所有的产生式 X->Y...
	if first(Y...) 不包含 空(sigma)
		select(X->Y...) = first(Y...)
	else
		select(X->Y...) += first(Y...) - {sigma}
		select(X->Y...) += follow(X)
```


```cpp
LL(1) based on analysis table

a(始终)为将要读取的下一个字符
X(始终)为符号栈顶元素
S为符号栈

//压入终结符号和文法开始符号
S.push('#')
S.push(S)

while true
	X = S.pop()
	if X 属于 终结符号
		if X 匹配 a 成功
			continue;
		else
			return false;
	else if X 为 sigma
		continue;
	else if X 为 # 
		if a == #
			return true;
		else 
			return false;
	else 
		production = analysisTable[X][a]
		//将production的右部right 逆向压栈
		for(i = production.right.length-1; i >= 0; --i)
			S.push(production.right[i])
```



#### 程序结构

![[../Resources/Pasted image 20230502193816.png]]
这是使用IDEA工具自动生成的LL包中类的UML图。
这里只画了继承关系，没有画引用。引用看起啦会十分混乱。
Symbol类用于表示文法中所有符号。两个继承自它的类分别为Terminal和NonTerminal，分别代表着终结符号和非终结符号。他们的value指的就是他们所代表的字符。在现在的代码中，他们为char类型。
接下来的Production为产生式，left为产生式左部，right为产生式右部，num是原先计划用于产生式的编号，但弃用了的属性。产生式右部可以为list<Symbol>，但我在还没有写到自动生成产生式的时候，用的是String，后续懒得修改了。
然后是First,代表一个具体的First集合。left指的是谁的first集合，right放的是first集合的内容。Follow集合和Select集合同理。
接下来是LL类，该类作为分析驱动引擎。terminals为所有终结符的集合。nonterminals为所有非终结符的集合，productions为所有产生式的集合。follows,firsts,selects同理。
analysis table为最后得到的分析表，不过这里玩了一点小技巧，分析表应该是输入[X,a]得到一个值，为了简单，这里将[X,a]映射为了一个int。beginSymbol为文法的开始符号，用于最后驱动的算法，我们这里默认第一个产生式左部符合为文法的开始符号。input为用于最终匹配的符号串。
firstMap, stringFirstMap, followMap这三个被用于求解First和Follow集合，firstMap是存储Symbol对应的First, stringFirstMap是存储产生式右部对应的First, followMap是存储非终结符号对应的Follow。
除此之外，在LL中还有一些工具函数。tranDim正是前文中分析表中将[X,a]映射为int的函数。copySet有两个版本，简单的版本就是直接将一个集合中所有的元素添加到另外一个集合中，另外一个版本则考虑了是否全部添加失败。（这个时候就说明没有变化，可以退出了）所有以show开头的，都是为了在控制台中输出相关信息。

#### 程序清单
parser/

First.java
Follow.java
LL.java
NonTerminal.java
Production.java
Select.java
Symbol.java
Terminal.java


#### 主要函数说明

求Production Terminal NonTerminal beginSymbol

```java
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
```
我们首先根据输入的语法grammar使用\n划分得到了splits，这里面放的就是是每一条具体的产生式。
接着我们先将产生式左部的符号映射为非终结符。当然这里我们必须考虑左部非终结符重复的问题。即我们应该先判断这个符号出现过没有，再去决定是新建一个符号还是不进行任何操作。因为我们新建使用的是new，借用一下浅拷贝和深拷贝的概念，如果我们这里新建一个对象，就算他的value和之前的相同，那这两个也不会被set识别为同一个对象。然后将这个非终结符和右边的字符串作为产生式构造函数的参数传进去。
最后将所有在产生式右部中出现过但不是非终结符的符号识别为终结符号
这里需要注意的细节是如果产生式右部出现了空(sigma)，我们不要去新建一个对象，因为后续我们使用的是Terminal类中的静态变量EMPTY。之后的比较都是基于这个静态变量的。这里直接跳过，在处理完普通终结符后，我们再将结束符号和空符号添加到集合中去。

```java
public void geneFirst() {  
        //all terminals's first = {terminal}  
        for(Terminal t: terminals) {  
            First first = new First(t.toString());  
            first.add(t);  
  
            firsts.add(first);  
            firstMap.put(t, first);  
        }  
  
        //get all first(X)  
       // Map<NonTerminal, First> map = new HashMap<>();        for(NonTerminal nt: nonTerminals) {  
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
  
        //adjust whether set is changed or not.  
        boolean isChanged = true;  
        while (isChanged){  
            isChanged = false;  
            for(Production production: productions) {  
                //if X -> Y1 Y2 Y3 ....  
                int index = 0;  
                Symbol symbol = getSymbols(production.getRight().charAt(index));  
                if(symbol instanceof NonTerminal) {  
                    First X = stringFirstMap.get(production.getRight());  
                    if(X == null)  
                        X = new First(production.getRight());  
                    First Y = firstMap.get((NonTerminal) symbol);  
                    boolean end = false;  
                    for(Terminal t:Y.getRight()) {  
                        if(t.equals(Terminal.Empty)) {  
                            end = true;  
                            continue;                        }  
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
                            break;                        }  
                        Y = firstMap.get((NonTerminal) symbol);  
                        end = false;  
                        for (Terminal t: Y.getRight()) {  
                            if(t.equals(Terminal.Empty)) {  
                                end = true;  
                                continue;                            }  
                            isChanged |= X.add(t);  
                        }  
                    }  
  
                    //all Y have sigma  
                    if(end) {  
                       isChanged |= X.add(Terminal.Empty);  
                    }  
                    //right part which is Y1Y2Y3...  
//                    System.out.println(production);  
//                    System.out.println("production.getright\t" + production.getRight());  
                    stringFirstMap.put(production.getRight(),X);  
                    //left part which is X  
                    First first = firstMap.get(production.getLeft());  
//                    System.out.println("production.getleft\t" + first.getLeft());  
                    copySet(first.getRight(),X.getRight(), isChanged);  
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
```

在生成first函数时，需要注意以下几点。

1.预处理。不要直接按照算法描述的就去生成first集合。应该先将终结符他们的first集合，以终结符开头的产生式的first集合先行求出。

2.求的first集合要考虑全面。有很长一段时间我求的first集合只考虑了符号的first集合。没有考虑产生式的集合。导致我在后续处理select集合的时候总是出现求多的情况。实际上，虽然在算法中没有明说，虽然在求解follow集合中用不到了产生式右部的first集合。我们还是应该将其求解出来，当然求解的时候也不能按照算法描述中给的一个右部一个一个求，我们直接把右部当成一个整体求就行。

3.在求解非终结符X对应的产生式右部情况为 aY...时，对于X，其first集合应当加上{a}。但是对于右部，则需要判断长度，如果长度为1，说明只有一个a，这个时候如果我们还新建一个first对象，并添加，会和第一步中求解终结符号的first集合相重复。

4.在求解产生式X -> Y1 Y2...时，我们需要先使用stringFirstMap去获取当前产生式右部的First,没有的时候我们再新建一个对象。这里这么操作的主要原因是我们在求解这种情况的时候，会循环判断，直到集合不再增加，如果这里我们每次都为右部新建一个对象，则会出现之前所说的深拷贝的类似问题。

5.最后产生式的First是直接放的，但是对于非终结符X，则是添加，这里不能错乱。

6.isChanged是用于判断每次添加时候是否成功的，set如果添加失败则说明之前存在这个元素，如果在一次操作中，所有的添加都失败，即所有的集合都不再变化，此时就可以退出了。表示所有集合都添加失败，也就是没有一个true，也就是原变量为false,然后对所有添加的结果都执行或操作。

```java
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
            for(Production production:productions) {
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
```

follow集合本质上的注意事项和first集合差不多，但奈何我先写的follow。

1.follow初始化。初始化的时候注意要给文法的开始符号加上#。原先我是打算使用拓扑排序找文法的开始符号的。但是后来发现不现实，LL文法的开始符号就得人为指定。方便期间，我就把输入的第一个产生式的左部符号作为了文法的开始符号。

2.Follow集合在分析的时候有一个很坑的地方。在算法的伪代码描述中没有明说，但是其实是指出来了的。即我们分析产生式右部时，要分析产生式右部所有的非终结符号，而不仅仅只是一个符号。所以这里我还额外使用了index搭配followFromIndex函数实现了这一个功能。

```java
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
```

比较简单的函数，唯一值得说的恐怕就只有根据产生式找first集合的时候用的是产生式右部的first，而不是产生式左部非终结符的first。

```java
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
```

根据select生成分析表，没什么好说的，跳。

```java
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
```

LL1表驱动算法。得益于这里的算法描述要精确地多。（当然也可能是这里确实好描述）一遍完工，修改的地方不多。仅仅指出几个和前文算法中不一样的地方。

由于我这里将空和结束都使用的是Terminal类型，所以对于它们的处理都放到了对于终结符号处理的地方。而为了防止可能的越界，以及a读完字符串后应该为#的情况，我们需要在index要超过length之后把a置为#。

#### 实验结果

两个测试样例

```
input:
	a*a+a
grammar:
	"E TQ\nQ +TQ\nQ $\nT FW\nW *FW\nW $\nF (E)\nF a"
```







```
input:
	aabd
grammar:
	"S AaS\nS BbS\nA a\nB $\nB c\nS d"
```

### 心得体会

这份代码是分了两次写的。

第一次是在大概半个月前的周四下午（因为上午满课，周五就要检查了），当时设计了这些类，并手动构造了前文提到的productions, terminals, non-termianls,firsts, follows, selects, analysis table等。就写了一个最简单的表驱动算法。这些并不算是什么难事。总共也就花了2小时多。现在LL类的大部分注释都是那个时候加的。

第二次则是昨天加今天一丢丢时间。昨天以为就按照算法写一下对应的代码，应该和之前的难度差不多，只不过是算法多了一点点嘛。但是没想到啊。书上算法在描述FIrst,Follow,还有select的时候，含糊其辞。例子不够说明问题。而且我还出现了好几次，因为想简化的求解First和Follow，鬼迷心窍的把原先的文法改成了它的增广文法，然后跑测试样例的时候总是不对，结果debug半天后，突然发现这个增广文法不是LL1文法。（悲）原先我的打算本来是逆推，从表驱动算法开始，一步步把手动构造的过程变成函数自动构成的过程。但在求这三个集合这里错乱了。因为follow集合依赖着first，但我之前根本不知道咋写求first的算法，我可不想写好了求follow的算法，结果发现first的结果不是我想要的。于是就先去求的first，然后就遇到了所有集合都不再发生变化怎么用代码简单的解决，求产生式右部还是产生式左部，终结符和非终结符等等问题。这些其实都不是最影响心态的。最影响心态的是在之后测试的时候，由于我找了一个+ * 的文法。别人样例给的是i*i+i。但是我终结符是a，我傻乎乎给写成了i。但是呢？在输出分析表的时候，我输出的时候没有给输出#那一行的内容，结果我一直以为是我在求三个集合的时候出错了，找了半天问题，突然发现输入串写错了。

编译原理啊！

千言万语，汇成一句话。Talk is cheap, show me the code. 当然还可以再加一句，Done is better than perfect.


# Bottom