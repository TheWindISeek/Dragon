package main;

import java.io.*;
import java.util.Iterator;
import java.util.Map;

import inter.Set;
import lexer.*;
import parser.*;

import javax.rmi.CORBA.Util;

/**
 * file encoding: utf-8
 * @author JeffreySharp
 *
 * 是否可以处理
 * 1.不同作用域下同名的变量
 * 2.同一作用域下的重复声明
 * 3.同一作用域下的重复定义
 * 4.预处理后重名的定义
 * 5.导入的包重名的代码
 * 6.导入的包代码错误
 * */
public class Main {
    public static void lab() throws IOException {
//        String filePath = "F:\\CompilerProject\\Dragon\\main\\1.txt";
        String filePath = "test\\1.txt";//相对路径相对的是项目的根目录
        // first we build a lexer for scanning token.
        Lexer lex = new Lexer(filePath);
        //then we initialize parser with lexer.
        Parser parse = new Parser(lex);
        //we run the parse program.
        parse.program();
        //just for format.
        System.out.write('\n');
    }
    public static void lab1() throws IOException{
//        String filePath = "F:\\CompilerProject\\Dragon\\main\\1.txt";
        String filePath = "test\\1.txt";//相对路径相对的是项目的根目录
        // first we build a lexer for scanning token.
        Lexer lex = new Lexer(filePath);
        try {
            while (true) {
                lex.scan();
            }
        }catch (IOException ex) {
            if(ex == Lexer.eof) { // 不得不说 这代码写得我害怕 强制goto了
                System.out.println("file have already been read.");
            }
        } finally {
            java.util.Set entry = lex.getWords().entrySet();
            Iterator<Map.Entry> it = entry.iterator();
            while (it.hasNext()) {
                Map.Entry entry1 = it.next();//单词 + 类别 token + token.id
                System.out.printf("name: %10s", entry1.getKey());
                System.out.printf("\ttype: %-10s\n", Tag.getTag(((Word)entry1.getValue()).tag));
            }
        }
    }
    public static void main(String[] args) throws IOException {
        lab1();
    }
}
