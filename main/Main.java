package main;

import java.io.*;

import lexer.*;
import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
        // first we build a lexer for scanning token.
        Lexer lex = new Lexer();
        //then we initialize parser with lexer.
        Parser parse = new Parser(lex);
        //we run the parse program.
        parse.program();
        //just for format.
        System.out.write('\n');
    }
}
