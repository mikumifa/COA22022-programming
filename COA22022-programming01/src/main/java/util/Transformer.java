package util;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Transformer {
    public static String valueOf(String str, int redis) {
        int ans = 0;
        for (int i = 0; i < str.length(); i++) {
            int now = str.charAt(i) - '0';
            ans = ans * redis + now;
        }
        return String.valueOf(ans);
    }

    /**
     * 将整数真值（十进制表示）转化成补码表示的二进制，默认长度32位
     *
     * @param numStr
     * @return
     */
    public static String intToBinary(String numStr) {
        StringBuilder ansBuilder = new StringBuilder();
        int num = Integer.parseInt(numStr);
        for (int i = 0; i < 32; i++) {
            ansBuilder.append(num % 2);
            num /= 2;
        }
        return ansBuilder.reverse().toString();
    }

    /**
     * @param binStr
     * @return
     */
    public static String binaryToInt(String binStr) {
        String num = valueOf(binStr.substring(1), 2);
        int ans = Integer.parseInt(num) - (binStr.charAt(0) - '0') * (Integer.MAX_VALUE) - 1;
        return String.valueOf(ans);
    }

    public static String decimalToNBCD(String decimal) {
        StringBuilder ansBuilder = new StringBuilder();
        int inp = Integer.parseInt(decimal);
        if (inp < 0) {
            ansBuilder.append("1101");
        } else ansBuilder.append("1100");
        for (int i = 0; i < 7; i++) {
            int now = inp % 10;
            String nowStr = intToBinary(now + "").substring(28);
            ansBuilder.insert(4, nowStr);
            inp /= 10;
        }
        return ansBuilder.toString();
    }

    public static String NBCDToDecimal(String NBCDStr) {
        String ans = "";
        if (NBCDStr.startsWith("1101")) {
            ans += "-";
        }
        NBCDStr = NBCDStr.substring(4);
        for (int i = 0; i < 7; i++) {
            String now = NBCDStr.substring(i * 4, i * 4 + 4);
            ans += valueOf(now, 2);
        }
        return String.valueOf(Integer.parseInt(ans));
    }

    /**
     * calculate the Exponent
     *
     * @param x
     * @return exponent
     */
    public static int getExponent(float x) {
        assert (x != 0);
        int exponent = 0;
        while (x >= 2) {
            x /= 2;
            exponent++;
        }
        while (x < 1) {
            x *= 2;
            exponent--;
        }
        return exponent;
    }

    public static String gitFix(float x) {
        int length = 23;
        StringBuilder ansBuilder = new StringBuilder();
        float bit = 0.5f;
        for (int i = 0; i < length; i++) {
            if (x >= bit) {
                ansBuilder.append("1");
                x-=bit;
            } else {
                ansBuilder.append("0");
            }
            bit /= 2;
        }
        return ansBuilder.toString();
    }

    public static String floatToBinary(String floatStr) {
        float inp = Float.parseFloat(floatStr);
        if (Float.isNaN(inp))
            return "Nan";
        if (Float.isInfinite(inp) || inp > Float.MAX_VALUE || inp < -Float.MAX_VALUE)
            return inp > 0 ? "+Inf" : "-Inf";
        if (inp == 0.0)
            return "00000000000000000000000000000000";
        StringBuilder ansBuilder = new StringBuilder();
        if (inp < 0) ansBuilder.append("1");
        else ansBuilder.append("0");
        int bias = 127;
        boolean isNormal = Math.abs(inp) >= Math.pow(2, -126);
        if (isNormal) {
            int exp = getExponent(inp);
            inp = inp / (float) Math.pow(2, exp);
            ansBuilder.append(intToBinary(String.valueOf(exp + bias)).substring(32 - 8));
            ansBuilder.append(gitFix(inp-1));
        }else {
            ansBuilder.append("00000000");
            inp=inp*(float) Math.pow(2,126);//一直使用Float
            ansBuilder.append(gitFix(inp));
        }
        return ansBuilder.toString();
        //各部位合并
    }

    public static String binaryToFloat(String binStr) {
        boolean postive= binStr.charAt(0) != '1';
        String exp = binStr.substring(1, 9);
        String part = binStr.substring(9);
        if(exp.equals("11111111")){
            if(part.equals("00000000000000000000000"))
            {
                return postive?"+Inf":"-Inf";
            }else return "Nan";
        }else if (exp.equals("00000000"))
        {
            if(part.equals("00000000000000000000000"))
                return postive?"0.0":"-0.0";
            float ans=Float.parseFloat(valueOf(part,2))*(float)(Math.pow(2,-23)/(float)(Math.pow(2,126)));//善用valueOf，和tranformer.binaryToInt
            if (!postive) {
                ans=-ans;
            }
            return String.valueOf(ans);
        }else {
            float ans=Integer.parseInt(valueOf(part,2))*(float)(Math.pow(2,-23))+1;
            ans*=Math.pow(2,Integer.parseInt(valueOf(exp,2))-127);
            if (!postive) {
                ans=-ans;
            }
            return String.valueOf(ans);
        }
    }

}
