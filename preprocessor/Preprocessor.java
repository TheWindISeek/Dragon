package preprocessor;

import java.io.*;

/**
 * file encoding: utf-8
 *
 * @author JeffreySharp
 * <p>
 * 预处理器
 * 处理宏替换 #define false
 * 单行注释 // false
 * 多行注释 / * * / false
 * 处理导入库 #include ".h" false
 *
 *
 */
public class Preprocessor {
    InputStream inputStream;

    OutputStream outputStream;

    File file;

    String outFilePath = "Preprocess.i";

    char peek;//向前看一个字符
    char cur;//当前字符

    public Preprocessor() throws IOException {
        inputStream = System.in;

        //选择输出流对象 以写入的方式解决
        file = new File(outFilePath);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        outputStream = new FileOutputStream(file);
    }

    public Preprocessor(String srcFilePath) throws IOException {
        this();
        inputStream = new FileInputStream(srcFilePath);
    }

    public Preprocessor(InputStream inputStream) throws IOException {
        this();
        this.inputStream = inputStream;
    }

    void readch() throws IOException {
        peek = (char) inputStream.read();
    }

    boolean readch(char c) throws IOException {
        readch();
        if (peek != c) return false;
        peek = ' ';
        return true;
    }

    // 可能的测试样例 // /***/
    // /*/ //* /**//*/
    public void commentAndMacro() {
        boolean flag = false;
        boolean multiline = false;
        boolean singleline = false;

        try {
            readch();//读取数据到peek
            while (inputStream.available() != 0) {
                cur = peek;
                readch();//此时有两份数据 cur 指向第一个字符 peek 指向下一个字符
                switch (cur) {
                    case '/':
                        if (!singleline && peek == '*') { //多行注释开
                            multiline = true;
                        } else if (!multiline && peek == '/') { // 单行注释开
                            singleline = true;
                        } else {
                            outputStream.write(cur);//将当前的写入到输出流中去
                        }
                        break;
                    case '*':
                        if (peek == '/') { //关闭多行注释
                            multiline = false;
                        } else {
                            outputStream.write(cur);
                        }
                        break;
                    case '\r'://遇到换行的处理
                        if (!multiline && peek == '\n') {
                            singleline = false;
                            outputStream.write('\n');//保留换行符
                        }
                    default://默认情况下 写入cur
                        if (!multiline && singleline)
                            outputStream.write(cur);
                }
            }
            flag = true;
        } catch (IOException exception) {

        }
        //如果正常读取完毕
        if (flag) {

        }
    }

    /**
     * 同步
     * 将输出流输出到指定的文件路径路径中去
     */
    public boolean syncAndClose() throws Exception {
        outputStream.flush();
        outputStream.close();
        return true;
    }

}
