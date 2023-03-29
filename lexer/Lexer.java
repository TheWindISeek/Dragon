package lexer;

import java.io.*;
import java.util.*;

import symbols.*;

/**
 * file encoding: utf-8
 * @author JeffreySharp
 *
 * 词法分析
 * */
public class Lexer {
    // enf of file
    public static IOException eof = new IOException("The file have already been read, whereas you still want to read.");
    // record line
    public static int line = 1;
    char peek = ' ';

    InputStream inputStream;
    Hashtable words = new Hashtable();

    boolean isMac;//选择换行符

    public Hashtable getWords() {
        return words;
    }

    void reserve(Word w) {
        words.put(w.lexeme, w);
    }

    public Lexer() {
        // 保留字
        reserve(new Word("if", Tag.IF));
        reserve(new Word("else", Tag.ELSE));
        reserve(new Word("while", Tag.WHILE));
        reserve(new Word("do", Tag.DO));
        reserve(new Word("break", Tag.BREAK));

        // 类型
        reserve(Word.True);
        reserve(Word.False);
        reserve(Type.Int);
        reserve(Type.Char);
        reserve(Type.Bool);
        reserve(Type.Float);

        //选择输入流
        inputStream = System.in;

        // 操作系统类型
        char osName = System.getProperty("os.name").toString().charAt(0);
        //W window L linux M mac U unix
        switch (osName) {
            case 'W':
            case 'w':
                isMac = false;
                break;
            case 'M':
            case 'm':
                isMac = true;
                break;
            case 'U':
            case 'L':
            case 'l':
            case 'u':
                isMac = false;
                break;
        }
    }

    /**
     * file path 输入文件字符流的路径
     */
    public Lexer(String filePath) throws FileNotFoundException {
        //先初始化类型
        this();
        inputStream = new FileInputStream(filePath);
    }

    void readch() throws IOException {
        if (inputStream.available() != 0)
            peek = (char) inputStream.read();
        else
            throw eof;
    }


    boolean readch(char c) throws IOException {
        readch();
        if (peek != c) return false;
        peek = ' ';
        return true;
    }

    /**
     * unix: \n 0x0A
     * mac: \r 0x0D
     * win: \r\n 0x0D 0x0A
     */
    public Token scan() throws IOException {
        for (; ; readch()) {
            if (peek == ' ' || peek == '\t') {
                continue;
            } else if (peek == '\r') {
                if (isMac)
                    line = line + 1;
                else if (readch('\n'))
                    line = line + 1;
            } else if (peek == '\n') {
                line = line + 1;
            } else {
                break;
            }
        }

        switch (peek) {
            case '&':
                if (readch('&'))
                    return Word.and;
                else
                    return new Token('&');
            case '|':
                if (readch('|'))
                    return Word.or;
                else
                    return new Token('|');
            case '=':
                if (readch('='))
                    return Word.eq;
                else
                    return new Token('=');
            case '!':
                if (readch('='))
                    return Word.ne;
                else
                    return new Token('!');
            case '<':
                if (readch('='))
                    return Word.le;
                else
                    return new Token('<');
            case '>':
                if (readch('='))
                    return Word.ge;
                else
                    return new Token('>');
        }

        if (Character.isDigit(peek)) {
            int v = 0;
            do {
                v = 10 * v + Character.digit(peek, 10);
                readch();
            } while (Character.isDigit(peek));

            if (peek != '.') return new Num(v);

            float x = v;
            float d = 10;
            for (; ; ) {
                readch();
                if (!Character.isDigit(peek))
                    break;
                x = x + Character.digit(peek, 10) / d;
                d = d * 10;
            }
            return new Real(x);
        }

        if (Character.isLetter(peek)) {
            StringBuffer b = new StringBuffer();
            do {
                b.append(peek);
                readch();
            } while (Character.isLetterOrDigit(peek));

            String s = b.toString();
            Word w = (Word) words.get(s);
            if (w != null) return w;

            w = new Word(s, Tag.ID);
            words.put(s, w);
            return w;
        }

        Token tok = new Token(peek);
        peek = ' ';
        return tok;
    }
}
