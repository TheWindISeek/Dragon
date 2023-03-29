package main;

import java.io.*;

import lexer.*;
import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
        String filePath = "F:\\CompilerProject\\Dragon\\main\\testCode.txt";
        // first we build a lexer for scanning token.
        Lexer lex = new Lexer(filePath);
        //then we initialize parser with lexer.
        Parser parse = new Parser(lex);
        //we run the parse program.
        parse.program();
        //just for format.
        System.out.write('\n');
    }
}
