package test;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.zip.CRC32;

public class CRCMD5 {
    private byte[] ans;
    private CRC32 crc = new CRC32();

    private MessageDigest md = MessageDigest.getInstance("MD5");
    private int index;

    private CRCMD5(byte[] ans) throws NoSuchAlgorithmException {
        this.ans = ans;
        index = 0;
    }
    private CRCMD5(byte[] ans, int index) throws NoSuchAlgorithmException {
        this.ans = ans;
        crc.update(ans);
        md.update(ans);// 计算md5函数
        this.index = index;
    }

    public void update(byte[] b) {
        ans = b;
        crc.update(ans);
        md.update(ans);
    }

    public String getValue() {
        String hashedPwd = new BigInteger(1, md.digest()).toString(16);// 16是表示转换为16进制数
        String crcPwd = String.format("%08x", crc.getValue());

//        System.out.println(crcPwd);
//        System.out.println(hashedPwd);

        StringBuffer stringBuffer = new StringBuffer();

        String left = hashedPwd.substring(0, index);
        String right = hashedPwd.substring(index, 32);
//        System.out.println(left);
//        System.out.println(right);
        stringBuffer.append(left);
        stringBuffer.append(crcPwd);
        stringBuffer.append(right);
        return stringBuffer.toString();
    }


    private String getMD5() {
        return new BigInteger(1, md.digest()).toString(16);
    }

    private String getCRC() {
        return String.format("%08x", crc.getValue());
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        String s = "河北大学-基于SSM的实验室信息管理系统";
        Scanner scanner = new Scanner(System.in);
        s = scanner.nextLine();
        CRCMD5 crcmd5 = new CRCMD5(s.getBytes(), 6);
        System.out.println("转换前字符串为:" + s);
        System.out.println("字符串MD5加密后结果为:" + crcmd5.getMD5());
        System.out.println("字符串CRC加密后结果为:" + crcmd5.getCRC());
        System.out.println("转换后的结果为:" + crcmd5.getValue());
    }
}
